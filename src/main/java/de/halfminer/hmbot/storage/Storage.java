package de.halfminer.hmbot.storage;

import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import de.halfminer.hmbot.BotClass;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Storage extends BotClass {

    private final Map<Integer, HalfClient> clients = new ConcurrentHashMap<>();
    private final List<HalfGroup> groups = Collections.synchronizedList(new ArrayList<>());
    private final File storageFile = new File("hmbot/", "storage.yml");

    public Storage() {

        if (storageFile.exists()) {
            try (FileReader reader = new FileReader(storageFile)){
                Object loaded = new Yaml().load(reader);
                if (loaded instanceof Map) {
                    for (Map.Entry o : ((Map<?, ?>) loaded).entrySet()) {
                        if (o.getValue() instanceof Map) {
                            Map<?, ?> currentMap = (Map) o.getValue();
                            int clientID = (Integer) currentMap.get("clientID");
                            int channelID = (Integer) currentMap.get("channelID");
                            HalfClient currentClient = new HalfClient(clientID, null);
                            currentClient.setChannelId(channelID);
                            clients.put((int) o.getKey(), currentClient);
                        }
                    }
                } else if (loaded != null) {
                    logger.warn("Storage file is in invalid format and was ignored");
                }
            } catch (Exception e) {
                logger.warn("Couldn't read storage file", e);
            }
        }

        configWasReloaded();

        // check for dead client objects every hour
        scheduler.scheduleRunnable(() ->
                clients.values().removeIf(c -> !c.doSaveToDisk() && !c.isOnline()),
                1, 1, TimeUnit.HOURS);

        scheduler.scheduleRunnable(this::doSaveOnDisk, 15, 15, TimeUnit.MINUTES);
    }

    public void configWasReloaded() {
        groups.clear();
        Map<?, ?> groupYamlMap = (Map) config.get("groups", Map.class);
        for (Map.Entry<?, ?> entry : groupYamlMap.entrySet()) {

            if (entry.getKey() instanceof String
                    && entry.getValue() instanceof Integer) {

                String groupName = (String) entry.getKey();
                int talkPower = (Integer) entry.getValue();
                Set<String> permissions = new HashSet<>();

                List<?> permissionsObj = (List) config.get("permissions." + groupName, List.class);
                if (permissionsObj != null) {
                    for (Object o : permissionsObj) {
                        permissions.add(String.valueOf(o).toLowerCase());
                    }
                }

                if (permissions.isEmpty()) {
                    logger.warn("No permissions set for group {}, skipping", groupName);
                    continue;
                }

                HalfGroup group = new HalfGroup(groupName, talkPower, permissions);

                boolean hasInserted = false;
                for (int i = 0; i < groups.size(); i++) {

                    HalfGroup groupToCheck = groups.get(i);
                    if (groupToCheck.getTalkPower() == talkPower) {
                        logger.info("Merged group {} with {}, as they have the same talk power requirement",
                                groupName, groupToCheck.getName());
                        groupToCheck.addPermissions(group);
                        hasInserted = true;
                        continue;
                    }

                    if (groupToCheck.getTalkPower() < talkPower) {
                        groups.add(group);
                        hasInserted = true;
                        break;
                    }
                }

                if (!hasInserted) {
                    groups.add(group);
                }

            } else {
                logger.warn("Error during read of groups in config.yml, line not in correct format");
            }
        }

        if (groups.isEmpty()) {
            groups.add(getDefaultGroup());
            logger.warn("No groups or permissions were loaded, please check your config file");

        } else {
            boolean hasDefaultGroup = false;
            for (HalfGroup group : groups) {
                if (group.getTalkPower() <= 0) {
                    hasDefaultGroup = true;
                    break;
                }
            }

            if (!hasDefaultGroup) {
                groups.add(getDefaultGroup());
            }

            for (int i = groups.size() - 1; i > 0; i--) {
                HalfGroup group = groups.get(i);
                groups.get(i - 1).addPermissions(group);
            }

            logger.info("Loaded groups ({}), talk power and permissions: ", groups.size());
            for (HalfGroup group : groups) {
                logger.info("{}: {}, Permissions: {}",
                        group.getName(), group.getTalkPower(), group.getPermissions());
            }
        }

        for (Client client : api.getClients()) {
            clientJoinedOrReloaded(client);
        }

        // log currently held clients
        if (clients.size() > 0) {

            StringBuilder sb = new StringBuilder("Clients currently loaded (")
                    .append(clients.size())
                    .append("): ");

            for (HalfClient client : clients.values()) {
                sb.append(client.toString()).append(", ");
            }

            sb.setLength(sb.length() - 2);
            logger.debug(sb.toString());
        }
    }

    public void clientJoinedOrReloaded(Client client) {

        if (!client.isRegularClient()) return;

        HalfGroup clientGroup = null;
        for (HalfGroup group : groups) {
            if (group.getTalkPower() <= client.getTalkPower()) {
                clientGroup = group;
                break;
            }
        }

        int databaseId = client.getDatabaseId();
        if (clients.containsKey(databaseId)) {
            clients.get(databaseId).updateClient(client.getId(), clientGroup);
        } else {
            clients.put(databaseId, new HalfClient(client.getId(), clientGroup));
        }
    }

    public void clientLeft(int clientId) {
        Iterator<HalfClient> it = clients.values().iterator();
        while (it.hasNext()) {
            HalfClient next = it.next();
            if (next.getClientId() == clientId) {
                next.setOffline();
                if (!next.doSaveToDisk()) {
                    it.remove();
                }
                return;
            }
        }
    }

    public HalfClient getClient(Client client) {
        return clients.get(client.getDatabaseId());
    }

    public HalfClient getClient(int clientId) {
        return clients.get(api.getClientInfo(clientId).getDatabaseId());
    }

    public Map<Client, HalfClient> getOnlineClients() {
        Map<Client, HalfClient> toReturn = new HashMap<>();
        for (Client client : api.getClients()) {
            if (client.isRegularClient()) {
                toReturn.put(client, clients.get(client.getDatabaseId()));
            }
        }
        return toReturn;
    }

    public void doSaveOnDisk() {

        Map<Integer, Map<String, Object>> toStore = new HashMap<>();
        for (Map.Entry<Integer, HalfClient> entry : clients.entrySet()) {
            HalfClient current = entry.getValue();
            if (current.doSaveToDisk()) {
                Map<String, Object> clientData = new HashMap<>();
                clientData.put("clientID", current.getClientId());
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

    private HalfGroup getDefaultGroup() {
        return new HalfGroup("default", 0, Collections.emptySet());
    }
}
