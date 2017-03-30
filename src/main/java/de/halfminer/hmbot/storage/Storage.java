package de.halfminer.hmbot.storage;

import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import de.halfminer.hmbot.HalfminerBotClass;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Storage extends HalfminerBotClass {

    private final Map<Integer, HalfClient> clientsOnline = new ConcurrentHashMap<>();
    private final List<HalfGroup> groups =
            Collections.synchronizedList(new ArrayList<HalfGroup>());

    public Storage() {

        configWasReloaded();

        // check for dead client objects every 10 minutes
        scheduler.scheduleRunnable(() -> clientsOnline.values().removeIf(c -> c.canBeEvicted(api.getClients())),
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
            groups.add(new HalfGroup("default", 0, Collections.emptySet()));
            logger.warn("No groups or permissions were loaded, please check your config file");
        } else {

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

        // debug currently held clients
        if (clientsOnline.size() > 0) {

            StringBuilder sb = new StringBuilder("Clients currently online (")
                    .append(clientsOnline.size())
                    .append("): ");

            for (HalfClient client : clientsOnline.values()) {
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

        if (clientsOnline.containsKey(client.getDatabaseId())) {
            clientsOnline.get(client.getDatabaseId()).updateClient(client.getId(), clientGroup);
        } else {
            clientsOnline.put(client.getDatabaseId(), new HalfClient(client.getId(), clientGroup));
        }
    }

    public HalfClient getClient(int clientId) {
        return clientsOnline.get(api.getClientInfo(clientId).getDatabaseId());
    }

    public HalfClient getClient(Client client) {
        return clientsOnline.get(client.getDatabaseId());
    }
}
