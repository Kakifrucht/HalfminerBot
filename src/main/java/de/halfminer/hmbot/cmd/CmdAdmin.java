package de.halfminer.hmbot.cmd;

import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import de.halfminer.hmbot.storage.HalfClient;
import de.halfminer.hmbot.util.MessageBuilder;
import de.halfminer.hmbot.util.StringArgumentSeparator;

import java.util.List;
import java.util.Map;

/**
 * - Lookup player information via username or client id
 * - Reload the config file
 *   - Won't reload if file was not modified or if it is in invalid format
 * - Restart (full reconnect) or stop the bot entirely
 */
class CmdAdmin extends Command {

    public CmdAdmin(HalfClient client, StringArgumentSeparator command) throws InvalidCommandException {
        super(client, command);

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

                    Client toLookup = null;
                    int id = command.getArgumentInt(1);
                    if (id > Integer.MIN_VALUE) {
                        toLookup = api.getClientInfo(id);
                    }

                    if (toLookup == null) {
                        List<Client> clients = api.getClientsByName(command.getArgument(1));
                        if (clients != null) {
                            if (clients.size() == 1) {
                                toLookup = clients.get(0);
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

                    if (toLookup == null) {
                        sendMessage("cmdAdminLookupNotFound");
                        return;
                    }

                    StringBuilder send = new StringBuilder(toLookup.getNickname() + ":\n");
                    for (Map.Entry<String, String> entry : toLookup.getMap().entrySet()) {
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
}
