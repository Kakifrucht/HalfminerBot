package de.halfminer.hmbot.cmd;

import com.github.theholywaffle.teamspeak3.api.wrapper.DatabaseClientInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerGroup;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import de.halfminer.hmbot.storage.HalfClient;
import de.halfminer.hmbot.util.MessageBuilder;
import de.halfminer.hmbot.util.StringArgumentSeparator;
import okhttp3.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * - Automatic retrieval of server donation group after querying Halfminer REST API for privileges
 * - Removes old server group, even if a different identity was used
 */
class CmdRank extends Command {

    private final static String url = "https://api.halfminer.de/storage/";

    private final OkHttpClient httpClient = new OkHttpClient();

    public CmdRank(HalfClient client, StringArgumentSeparator command) {
        super(client, command);
    }

    @Override
    void run() throws InvalidCommandException {

        if (command.meetsLength(1)) {
            String pin = command.getArgument(0);
            Request request = new Request.Builder()
                    .url(url + "pins/" + pin)
                    .get()
                    .build();

            Response response = null;
            Response putResponse = null;
            try {
                response = httpClient.newCall(request).execute();
                if (response.isSuccessful()) {

                    Gson gson = new Gson();

                    Type type = new TypeToken<Map<String, String>>(){}.getType();
                    Map<String, String> GETMap = new Gson().fromJson(response.body().string(), type);
                    String rank = GETMap.get("pins." + pin + ".rank");
                    String uuid = GETMap.get("pins." + pin + ".uuid");
                    String ip = GETMap.get("pins." + pin + ".ip");
                    boolean isUpgraded = GETMap.get("pins." + pin + ".isUpgraded").equalsIgnoreCase("true");

                    if (!isUpgraded || !ip.equals(clientInfo.getIp())) {
                        MessageBuilder.create("cmdRankInvalidPin").sendMessage(clientInfo);
                        return;
                    }

                    String putString = "expiry=0&identity=" + clientInfo.getUniqueIdentifier() + "&rank=" + rank;
                    Request putRequest = new Request.Builder()
                            .url(url + "ranks/teamspeak/" + uuid)
                            .put(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), putString))
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
                        Map<String, String> jsonPut = gson.fromJson(putResponse.body().string(), type);
                        String oldIdentity = jsonPut.get("ranks.teamspeak." + uuid + ".identity") + '=';
                        String oldGroupName = jsonPut.get("ranks.teamspeak." + uuid + ".rank");
                        if (rank.equals(oldGroupName) && oldIdentity.equals(clientInfo.getUniqueIdentifier())) {
                            MessageBuilder.create("cmdRankAlreadyGiven").sendMessage(clientInfo);
                            return;
                        }

                        ServerGroup oldGroup = getMatchingGroup(groupList, oldGroupName);
                        if (oldGroup != null) {
                            DatabaseClientInfo oldClient = api.getDatabaseClientByUId(oldIdentity);
                            api.removeClientFromServerGroup(oldGroup.getId(), oldClient.getDatabaseId());
                        } else {
                            logger.warn("Couldn't remove client from old group {}, as it doesn't exist anymore", oldGroupName);
                        }
                    }

                    ServerGroup newGroup = getMatchingGroup(groupList, rank);
                    if (newGroup != null) {
                        api.addClientToServerGroup(newGroup.getId(), clientInfo.getDatabaseId());
                        MessageBuilder.create("cmdRankSet")
                                .addPlaceholderReplace("GROUPNAME", newGroup.getName())
                                .sendMessage(clientInfo);
                    } else {
                        logger.error("No group found with name {}", rank);
                        MessageBuilder.create("cmdDispatcherUnknownError").sendMessage(clientInfo);
                    }

                } else {
                    MessageBuilder.create("cmdRankInvalidPin").sendMessage(clientInfo);
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

        } else {
            MessageBuilder.create("cmdRankInfo").sendMessage(clientInfo);
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
}