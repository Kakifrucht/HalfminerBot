package de.halfminer.hmbot.storage;

import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import de.halfminer.hmbot.BotClass;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Default {@link Storage} implementation, uses flatfile for offline storage.
 */
public class DefaultStorage extends BotClass implements Storage {

    private final ClientMap clients = new ClientMap();
    private final List<HalfGroup> groups = Collections.synchronizedList(new ArrayList<>());


    public DefaultStorage() {
        doFullReload();
    }

    @Override
    public void doFullReload() {

        configWasReloaded();

        // check for dead client objects every hour, save on disk every 15 minutes
        scheduler.scheduleRunnable(clients::doCleanup, 1, 1, TimeUnit.HOURS);
        scheduler.scheduleRunnable(clients::saveData, 15, 15, TimeUnit.MINUTES);
    }

    @Override
    public void configWasReloaded() {

        // load groups and their permissions from config
        groups.clear();
        Map<?, ?> groupYamlMap = (Map<?, ?>) config.get("groups", Map.class);
        for (Map.Entry<?, ?> entry : groupYamlMap.entrySet()) {

            if (entry.getKey() instanceof String
                    && entry.getValue() instanceof Integer) {

                String groupName = (String) entry.getKey();
                int talkPower = (Integer) entry.getValue();
                Set<String> permissions = new HashSet<>();

                List<?> permissionsObj = (List<?>) config.get("permissions." + groupName, List.class);
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

            // add default group if not supplied
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

            // inherit permissions of lower groups
            for (int i = groups.size() - 1; i > 0; i--) {
                HalfGroup group = groups.get(i);
                groups.get(i - 1).addPermissions(group);
            }

            // log groups
            logger.info("Loaded groups ({}), talk power and permissions: ", groups.size());
            for (HalfGroup group : groups) {
                logger.info("{}: {}, Permissions: {}",
                        group.getName(), group.getTalkPower(), group.getPermissions());
            }
        }

        // reload online clients
        clients.reload();
        getTS3Api().getClients().forEach(this::clientJoinedOrReloaded);
        clients.doDebugLog();
    }

    @Override
    public void clientJoinedOrReloaded(Client client) {

        if (!client.isRegularClient()) return;

        HalfGroup clientGroup = null;
        for (HalfGroup group : groups) {
            if (group.getTalkPower() <= client.getTalkPower()) {
                clientGroup = group;
                break;
            }
        }

        clients.clientJoined(client, clientGroup);
    }

    @Override
    public void clientLeft(int clientId) {
        clients.clientLeft(clientId);
    }

    @Override
    public HalfClient getClient(Client client) {
        return clients.getClient(client.getDatabaseId());
    }

    @Override
    public Map<Client, HalfClient> getOnlineClients() {
        return clients.getOnlineClients(getTS3Api().getClients());
    }

    @Override
    public void saveData() {
        clients.saveData();
    }

    private HalfGroup getDefaultGroup() {
        return new HalfGroup("default", 0, Collections.emptySet());
    }
}
