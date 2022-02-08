package de.halfminer.hmbot.cmd;

import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.DatabaseClient;
import de.halfminer.hmbot.storage.HalfClient;
import de.halfminer.hmbot.util.MessageBuilder;
import de.halfminer.hmbot.util.StringArgumentSeparator;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
                sendMessage(restart ? "cmdAdminRestarted" : "cmdAdminStopped");
                componentHolder.getStateHolder().stop("Bot was " + (restart ? "restarted" : "stopped")
                        + " via command by client " + clientInfo.getNickname(), restart);
                return;
            case "reload":
                boolean success = componentHolder.getStateHolder().reloadConfig();
                sendMessage("cmdAdminConfigReloaded" + (success ? "" : "Error"));
                return;
            case "lookup":
            case "info":
            case "whois":
                if (command.meetsLength(2)) {
                    doLookup();
                    return;
                }
            default:
                throw new InvalidCommandException(CommandEnum.ADMIN);
        }
    }

    private void doLookup() {

        String lookupArg = command.getArgument(1);

        // determine what input we have
        LookupType type;
        if (lookupArg.length() == 28 && lookupArg.endsWith("=")) {
            type = LookupType.STRING_UID;
        } else if (command.getArgumentInt(1) > Integer.MIN_VALUE) {
            type = LookupType.NUMERIC_ID;
        } else {
            type = LookupType.NICKNAME;
        }

        Map<String, String> mapToSend = null;
        boolean isOnline = false;

        switch (type) {
            case NICKNAME:

                try {
                    List<Client> clients = api.getClientsByName(lookupArg);
                    if (clients.size() == 1) {
                        mapToSend = getClientInfoMap(clients.get(0));
                        isOnline = true;
                    } else if (!clients.isEmpty()) {
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

                } catch (TS3CommandFailedException ignored) {}
                break;
            case NUMERIC_ID:

                int id = command.getArgumentInt(1);
                for (Client client : api.getClients()) {
                    if (client.getId() == id || client.getDatabaseId() == id) {
                        mapToSend = getClientInfoMap(client);
                        isOnline = true;
                        break;
                    }
                }

                // offline database id lookup
                if (mapToSend == null) {
                    try {
                        DatabaseClient dbClient = api.getDatabaseClientInfo(id);
                        mapToSend = dbClient.getMap();
                    } catch (TS3CommandFailedException ignored) {}
                }

                break;
            case STRING_UID:

                try {
                    Client client = api.getClientByUId(lookupArg);
                    mapToSend = getClientInfoMap(client);
                    isOnline = true;
                } catch (TS3CommandFailedException e) {

                    try {
                        DatabaseClient dbClient = api.getDatabaseClientByUId(lookupArg);
                        mapToSend = dbClient.getMap();
                    } catch (TS3CommandFailedException ignored) {}
                }
                break;
        }

        if (mapToSend == null) {
            sendMessage("cmdAdminLookupNotFound");
            return;
        }

        mapToSend.put("client_created", stringToDate(mapToSend.get("client_created")));
        mapToSend.put("client_lastconnected", stringToDate(mapToSend.get("client_lastconnected")));

        StringBuilder toSend = new StringBuilder("======= ")
                .append(mapToSend.get("client_nickname"))
                .append(" (")
                .append(isOnline ? "Online" : "Offline")
                .append(", DBID: ")
                .append(mapToSend.get("client_database_id"))
                .append(") =======\n");

        for (Map.Entry<String, String> entry : mapToSend.entrySet()) {
            if (entry.getValue().length() == 0) continue;
            toSend.append(" ")
                    .append(entry.getKey())
                    .append("=")
                    .append(entry.getValue())
                    .append("\n");
        }

        MessageBuilder.create(toSend.toString())
                .setDirectString()
                .sendMessage(clientId);
    }

    private Map<String, String> getClientInfoMap(Client client) {
        if (client instanceof ClientInfo) {
            return client.getMap();
        } else {
            try {
                return api.getClientInfo(client.getId()).getMap();
            } catch (TS3CommandFailedException e) {
                return null;
            }
        }
    }

    private String stringToDate(String timestampStr) {
        try {
            long timestamp = Long.parseLong(timestampStr);
            Instant instant = Instant.ofEpochSecond(timestamp);

            return DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(instant);
        } catch (NumberFormatException e) {
            logger.warn("Couldn't parse timestamp {}", timestampStr);
            return timestampStr;
        }
    }

    private enum LookupType {
        NUMERIC_ID,
        STRING_UID,
        NICKNAME
    }
}
