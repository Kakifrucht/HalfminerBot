package de.halfminer.hmbot.storage;

import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import de.halfminer.hmbot.HalfminerBotClass;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Storage extends HalfminerBotClass {

    private final Map<Integer, HalfClient> clientsOnline = new ConcurrentHashMap<>();
    private final List<Map.Entry<String, HalfGroup>> groups = new ArrayList<>();

    public Storage() {

        configWasReloaded();

        // add already online clients
        for (Client client : api.getClients()) {
            if (client.isRegularClient()) {
                clientJoinedOrReloaded(client);
            }
        }

        // check for dead client objects every 10 minutes
        scheduler.scheduleRunnable(new Runnable() {
            @Override
            public void run() {
                Iterator<HalfClient> it = clientsOnline.values().iterator();
                while (it.hasNext()) {
                    if (it.next().canBeEvicted()) {
                        it.remove();
                    }
                }
            }
        }, 0, 10, TimeUnit.MINUTES);
    }

    public void configWasReloaded() {
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
                        permissions.add(String.valueOf(o));
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

        //TODO inheritance
        if (groups.isEmpty()) {
            groups.add(new AbstractMap.SimpleEntry<>("default",
                    new HalfGroup(0, Collections.<String>emptySet())));
            logger.warn("No groups or permissions were loaded, please check your config file");
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
