package de.halfminer.hmbot.storage;

import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holding all currently connected and/or loaded clients. Also contains mapping from clientID to databaseID and
 * handles flatfile storage saving and loading.
 */
class ClientMap {

    private static final Logger logger = LoggerFactory.getLogger(ClientMap.class);

    private final Map<Integer, HalfClient> clientsByDbId = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> clientIdToDatabaseId = new ConcurrentHashMap<>();

    private final File storageFile = new File("hmbot/", "storage.yml");

    ClientMap() {
        reload();
    }

    void reload() {
        saveData();
        clientsByDbId.clear();
        clientIdToDatabaseId.clear();

        if (storageFile.exists()) {
            try (FileReader reader = new FileReader(storageFile)) {

                Object loaded = new Yaml().load(reader);
                if (loaded instanceof Map) {
                    for (Map.Entry o : ((Map<?, ?>) loaded).entrySet()) {
                        if (o.getValue() instanceof Map) {
                            Map<?, ?> currentMap = (Map) o.getValue();
                            int databaseId = (int) o.getKey();
                            int channelID = (Integer) currentMap.get("channelID");

                            HalfClient currentClient = new DefaultHalfClient(databaseId, null);
                            currentClient.setChannelId(channelID);
                            clientsByDbId.put(databaseId, currentClient);
                        }
                    }
                } else if (loaded != null) {
                    logger.warn("Storage file is in invalid format and was ignored");
                }

            } catch (Exception e) {
                logger.warn("Couldn't read storage file", e);
            }
        }
    }

    void doDebugLog() {

        if (clientsByDbId.size() > 0) {

            StringBuilder sb = new StringBuilder("Clients currently loaded (")
                    .append(clientsByDbId.size())
                    .append("): ");

            for (HalfClient client : clientsByDbId.values()) {
                sb.append(client.toString()).append(", ");
            }

            sb.setLength(sb.length() - 2);
            logger.debug(sb.toString());
        }
    }

    void clientJoined(Client client, HalfGroup group) {

        int databaseId = client.getDatabaseId();
        clientIdToDatabaseId.put(client.getId(), databaseId);

        if (clientsByDbId.containsKey(databaseId)) {
            clientsByDbId.get(databaseId).clientJoined(group);
        } else {
            clientsByDbId.put(databaseId, new DefaultHalfClient(client.getDatabaseId(), group));
        }
    }

    void clientLeft(int clientId) {
        int databaseId = clientIdToDatabaseId.get(clientId);
        HalfClient hClient = clientsByDbId.get(databaseId);
        hClient.clientLeft();
        if (hClient.doDispose()) {
            clientsByDbId.remove(databaseId);
        }

        clientIdToDatabaseId.remove(clientId);
    }

    HalfClient getClient(int databaseId) {
        return clientsByDbId.get(databaseId);
    }

    Map<Client, HalfClient> getOnlineClients(List<Client> clientList) {

        Map<Client, HalfClient> toReturn = new HashMap<>();
        for (Client client : clientList) {
            if (client.isRegularClient()) {
                HalfClient toPut = clientsByDbId.get(client.getDatabaseId());
                if (toPut != null) {
                    toReturn.put(client, toPut);
                } else {
                    logger.info("Online client {} (ID: {}) was not found in storage, just logged in or out?",
                            client.getNickname(), client.getDatabaseId());
                }
            }
        }
        return toReturn;
    }

    void doCleanup() {
        clientsByDbId
                .values()
                .removeIf(HalfClient::doDispose);
    }

    void saveData() {
        if (!clientsByDbId.isEmpty()) {
            Map<Integer, Map<String, Object>> toStore = new HashMap<>();
            for (Map.Entry<Integer, HalfClient> entry : clientsByDbId.entrySet()) {
                HalfClient current = entry.getValue();
                if (current.doSaveToDisk()) {
                    Map<String, Object> clientData = new HashMap<>();
                    clientData.put("channelID", current.getChannelId());
                    toStore.put(entry.getKey(), clientData);
                }
            }

            try {
                new Yaml().dump(toStore, new FileWriter(storageFile));
                logger.debug("Storage was saved to disk");
            } catch (IOException e) {
                logger.error("Couldn't write storage save file to disk", e);
            }
        }
    }
}
