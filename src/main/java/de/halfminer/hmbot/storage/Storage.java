package de.halfminer.hmbot.storage;

import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import de.halfminer.hmbot.HalfminerBotClass;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Storage extends HalfminerBotClass {

    private final Map<Integer, HalfClient> clientsOnline = new ConcurrentHashMap<>();
    private final List<Map.Entry<String, HalfGroup>> groups =
            Collections.synchronizedList(new ArrayList<Map.Entry<String, HalfGroup>>());

    public Storage() {

        configWasReloaded();

        // add already online clients
        List<Client> clients = api.getClients();
        for (Client client : clients) {
            if (client.isRegularClient()) {
                clientJoinedOrReloaded(client);
            }
        }

        // check for dead client objects every 10 minutes
        scheduler.scheduleRunnable(() -> clientsOnline.values().removeIf(c -> c.canBeEvicted(clients)),
                10, 10, TimeUnit.MINUTES);
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

                HalfGroup group = new HalfGroup(talkPower, permissions);

                boolean hasInserted = false;
                Map.Entry<String, HalfGroup> newEntry = new AbstractMap.SimpleEntry<>(groupName, group);
                for (int i = 0; i < groups.size(); i++) {

                    Map.Entry<String, HalfGroup> currentEntry = groups.get(i);
                    HalfGroup groupToCheck = currentEntry.getValue();
                    if (groupToCheck.getTalkPower() == talkPower) {
                        logger.info("Merged group {} with {}, as they have the same talk power requirement",
                                groupName, currentEntry.getKey());
                        groupToCheck.addPermissions(group);
                        hasInserted = true;
                        continue;
                    }

                    if (groupToCheck.getTalkPower() < talkPower) {
                        groups.add(newEntry);
                        hasInserted = true;
                        break;
                    }
                }

                if (!hasInserted) {
                    groups.add(newEntry);
                }

            } else {
                logger.warn("Error during read of groups in config.yml, line not in correct format");
            }
        }

        if (groups.isEmpty()) {
            groups.add(new AbstractMap.SimpleEntry<>("default",
                    new HalfGroup(0, Collections.emptySet())));
            logger.warn("No groups or permissions were loaded, please check your config file");
        } else {

            for (int i = groups.size() - 1; i > 0; i--) {
                HalfGroup group = groups.get(i).getValue();
                groups.get(i - 1).getValue().addPermissions(group);
            }

            logger.info("Loaded groups ({}), talk power and permissions: ", groups.size());
            for (Map.Entry<String, HalfGroup> group : groups) {
                logger.info("{}: {}, Permissions: {}",
                        group.getKey(), group.getValue().getTalkPower(), group.getValue().getPermissions());
            }
        }

        for (Client client : api.getClients()) {
            clientJoinedOrReloaded(client);
        }
    }

    public void clientJoinedOrReloaded(Client client) {

        HalfGroup clientGroup = null;
        for (Map.Entry<String, HalfGroup> group : groups) {
            if (group.getValue().getTalkPower() <= client.getTalkPower()) {
                clientGroup = group.getValue();
                break;
            }
        }

        if (clientsOnline.containsKey(client.getDatabaseId())) {
            clientsOnline.get(client.getDatabaseId()).updateClient(client.getId(), clientGroup);
        } else {
            clientsOnline.put(client.getDatabaseId(), new HalfClient(client.getId(), clientGroup));
        }
    }

    public HalfClient getClient(int clientId) {
        return clientsOnline.get(api.getClientInfo(clientId).getDatabaseId());
    }
}
