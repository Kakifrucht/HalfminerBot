package de.halfminer.hmbot.cmd;

import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.DatabaseClientInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerGroup;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import de.halfminer.hmbot.storage.HalfClient;
import de.halfminer.hmbot.util.MessageBuilder;
import de.halfminer.hmbot.util.RESTHelper;
import de.halfminer.hmbot.util.StringArgumentSeparator;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * - Automatic retrieval of server donation group after querying Halfminer REST API for privileges
 * - Removes old server group, even if a different identity was used
 */
class CmdRank extends Command {

    private final OkHttpClient httpClient = new OkHttpClient();

    public CmdRank(HalfClient client, ClientInfo clientInfo, StringArgumentSeparator command) {
        super(client, clientInfo, command);
    }

    @Override
    void run() {

        if (!command.meetsLength(1)) {
            MessageBuilder.create("cmdRankInfo").sendMessage(clientInfo);
            return;
        }

        String pin = command.getArgument(0);
        if (pin.length() != 4) {
            sendInvalidPinMessage();
            return;
        }

        for (char c : pin.toCharArray()) {
            if (!Character.isDigit(c)) {
                sendInvalidPinMessage();
                return;
            }
        }

        Response response = null;
        Response putResponse = null;
        try {
            Request request = new Request.Builder()
                    .url(RESTHelper.getBaseUrl("storage/pins/" + pin))
                    .get()
                    .build();

            response = httpClient.newCall(request).execute();
            addCooldown(10);
            if (!response.isSuccessful()) {
                sendInvalidPinMessage();
                return;
            }

            Gson gson = new Gson();

            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> GETMap = gson.fromJson(response.body().string(), type);
            String rank = GETMap.get("rank");
            String uuid = GETMap.get("uuid");
            String ip = GETMap.get("ip");
            boolean isUpgraded = GETMap.get("isUpgraded").equalsIgnoreCase("true");

            if (!isUpgraded || !ip.equals(clientInfo.getIp())) {
                sendInvalidPinMessage();
                return;
            }

            String putString = "expiry=0&identity=" + clientInfo.getUniqueIdentifier() + "&rank=" + rank;
            Request putRequest = new Request.Builder()
                    .url(RESTHelper.getBaseUrl("storage/ranks/teamspeak/" + uuid))
                    .put(RESTHelper.getRequestBody(putString))
                    .build();

            putResponse = httpClient.newCall(putRequest).execute();
            if (!putResponse.isSuccessful()) {
                logger.error("Couldn't PUT rank data for {}", clientInfo.getNickname());
                MessageBuilder.create("cmdDispatcherUnknownError").sendMessage(clientInfo);
                return;
            }

            List<ServerGroup> groupList = api.getServerGroups();
            // remove old group if client already has rank on server
            if (putResponse.code() != 201) {
                removeOldGroupFromClient(gson, putResponse, type, groupList, rank);
            }

            ServerGroup newGroup = getMatchingGroup(groupList, rank);
            if (newGroup == null) {
                logger.error("No group found with name {}", rank);
                MessageBuilder.create("cmdDispatcherUnknownError").sendMessage(clientInfo);
                return;
            }

            addCooldown(900);
            try {
                api.addClientToServerGroup(newGroup.getId(), clientInfo.getDatabaseId());
                MessageBuilder.create("cmdRankSet")
                        .addPlaceholderReplace("GROUPNAME", newGroup.getName())
                        .sendMessage(clientInfo);
                logger.info("Set group for client {} to '{}'", clientInfo.getNickname(), newGroup.getName());
            } catch (TS3CommandFailedException e) {
                logger.error("Couldn't add client {} to group '{}'", clientInfo.getNickname(), newGroup.getName());
                MessageBuilder.create("cmdDispatcherUnknownError").sendMessage(clientInfo);
            }

        } catch (IOException e) {
            logger.error("Could not connect to API", e);
        } finally {
            if (response != null) {
                response.close();
            }
            if (putResponse != null) {
                putResponse.close();
            }
        }
    }

    private ServerGroup getMatchingGroup(List<ServerGroup> list, String groupName) {
        for (ServerGroup serverGroup : list) {
            if (serverGroup.getName().equalsIgnoreCase(groupName)) {
                return serverGroup;
            }
        }
        return null;
    }

    private void removeOldGroupFromClient(Gson gson,
                                          Response putResponse, Type typeToken,
                                          List<ServerGroup> groupList, String rank) throws IOException {

        Map<String, String> jsonPut = gson.fromJson(putResponse.body().string(), typeToken);
        String oldIdentity = jsonPut.get("identity") + '=';
        String oldGroupName = jsonPut.get("rank");

        if (rank.equals(oldGroupName) && oldIdentity.equals(clientInfo.getUniqueIdentifier())) {
            MessageBuilder.create("cmdRankAlreadyGiven").sendMessage(clientInfo);
            addCooldown(300);
            return;
        }

        ServerGroup oldGroup = getMatchingGroup(groupList, oldGroupName);
        if (oldGroup == null) {
            logger.warn("Couldn't remove client from old group {}, as the group couldn't be found", oldGroupName);
            return;
        }

        DatabaseClientInfo oldClient = api.getDatabaseClientByUId(oldIdentity);
        if (oldClient == null) {
            logger.warn("Old client with identity '{}' could not be found, group is not being removed", oldIdentity);
            return;
        }

        try {
            api.removeClientFromServerGroup(oldGroup.getId(), oldClient.getDatabaseId());
            logger.info("Removed client {} (dbid: {}) from group '{}'", oldClient.getNickname(), oldClient.getDatabaseId(), oldGroup.getName());
        } catch (TS3CommandFailedException ignored) {}
    }

    private void sendInvalidPinMessage() {
        MessageBuilder.create("cmdRankInvalidPin").sendMessage(clientInfo);
    }
}
