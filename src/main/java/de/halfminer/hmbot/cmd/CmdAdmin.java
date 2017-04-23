package de.halfminer.hmbot.cmd;

import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.DatabaseClientInfo;
import de.halfminer.hmbot.storage.HalfClient;
import de.halfminer.hmbot.util.MessageBuilder;
import de.halfminer.hmbot.util.StringArgumentSeparator;

import java.util.List;
import java.util.Map;

/**
 * - Lookup player information via username or client id
 *   - Supports client, database and unique id as parameters
 *   - Checks database for offline client with given database or unique id, if no player was found
 * - Reload the config file
 *   - Won't reload if file was not modified or if it is in invalid format
 * - Restart (full reconnect) or stop the bot entirely
 */
class CmdAdmin extends Command {

    private String lookupArg;

    public CmdAdmin(HalfClient client, ClientInfo clientInfo, StringArgumentSeparator command) throws InvalidCommandException {
        super(client, clientInfo, command);

        if (!command.meetsLength(1)) {
            throw new InvalidCommandException(CommandEnum.ADMIN);
        }
    }

    @Override
    void run() throws InvalidCommandException {
        String argument = command.getArgument(0);
        switch (argument.toLowerCase()) {
            case "stop":
            case "restart":
                boolean restart = argument.startsWith("r");
                if (restart) {
                    sendMessage("cmdAdminRestarted");
                } else {
                    sendMessage("cmdAdminStopped");
                }
                bot.stop("Bot was " + (restart ? "restarted" : "stopped")
                        + " via command by client " + clientInfo.getNickname(), restart);
                return;
            case "reload":
                if (bot.reloadConfig()) {
                    sendMessage("cmdAdminConfigReloaded");
                } else {
                    sendMessage("cmdAdminConfigReloadedError");
                }
                return;
            case "lookup":
                if (command.meetsLength(2)) {

                    lookupArg = command.getArgument(1);

                    Client toLookup = null;
                    Map<String, String> mapToSend;
                    String nickName;

                    int id = command.getArgumentInt(1);
                    if (id > Integer.MIN_VALUE) {
                        // lookup by id
                        toLookup = api.getClientInfo(id);
                    }

                    if (toLookup == null) {

                        if (isUniqueID()) {
                            toLookup = api.getClientByUId(lookupArg);
                        } else {
                            List<Client> clients = api.getClientsByName(lookupArg);
                            if (clients != null) {
                                if (clients.size() == 1) {
                                    toLookup = api.getClientInfo(clients.get(0).getId());
                                } else {

                                    StringBuilder send = new StringBuilder();
                                    for (Client client : clients) {
                                        send.append(" ID: ")
                                                .append(client.getId())
                                                .append(" - Nickname: ")
                                                .append(client.getNickname())
                                                .append("\n");
                                    }
                                    sendMessage("cmdAdminLookupList", "LIST", send.toString());
                                    return;
                                }
                            }
                        }
                    }

                    if (toLookup != null) {
                        mapToSend = toLookup.getMap();
                        nickName = toLookup.getNickname()
                                + " (ID: "
                                + toLookup.getId()
                                + ", DBID: "
                                + toLookup.getDatabaseId()
                                + ")";
                    } else {

                        // no online user was found, check database
                        DatabaseClientInfo toLookupOffline;
                        if (isUniqueID()) {
                            toLookupOffline = api.getDatabaseClientByUId(lookupArg);
                        } else {
                            toLookupOffline = api.getDatabaseClientInfo(command.getArgumentInt(1));
                        }

                        if (toLookupOffline != null) {
                            mapToSend = toLookupOffline.getMap();
                            nickName = toLookupOffline.getNickname() + " (Offline)";
                        } else {
                            sendMessage("cmdAdminLookupNotFound");
                            return;
                        }
                    }

                    StringBuilder send = new StringBuilder("======= ")
                            .append(nickName)
                            .append(" =======\n");

                    for (Map.Entry<String, String> entry : mapToSend.entrySet()) {
                        if (entry.getValue().length() == 0) continue;
                        send.append(" ").append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
                    }

                    MessageBuilder.create(send.toString())
                            .setDirectString()
                            .sendMessage(clientId);
                    return;
                }
            default:
                throw new InvalidCommandException(CommandEnum.ADMIN);
        }
    }

    private boolean isUniqueID() {
        return lookupArg.length() == 28 && lookupArg.endsWith("=");
    }
}
