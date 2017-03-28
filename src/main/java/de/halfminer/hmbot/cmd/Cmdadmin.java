package de.halfminer.hmbot.cmd;

import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import de.halfminer.hmbot.util.StringArgumentSeparator;

import java.util.List;
import java.util.Map;

/**
 * - Lookup player information via username or client id
 * - Reload the config file
 *   - Won't reload if file was not modified or if it is in invalid format
 * - Restart (full reconnect) or stop the bot entirely
 */
@SuppressWarnings("unused")
public class Cmdadmin extends Command {

    public Cmdadmin(int clientId, StringArgumentSeparator command) throws InvalidCommandLineException {
        super(clientId, command);

        if (!command.meetsLength(1)) {
            throw new InvalidCommandLineException("!admin <lookup|reload|restart|stop> [username/id]");
        }
    }

    @Override
    void run() throws CommandNotCompletedException {
        String argument = command.getArgument(0);
        switch (argument.toLowerCase()) {
            case "stop":
            case "restart":
                boolean restart = argument.startsWith("r");
                sendMessage("Bot is " + (restart ? "restarting." : "stopping."));
                bot.stop("Bot was " + (restart ? "restarted" : "stopped")
                        + " via command by client " + api.getClientInfo(clientId).getNickname(), restart);
                break;
            case "reload":
                if (bot.reloadConfig()) {
                    sendMessage("Configuration was reloaded.");
                } else {
                    sendMessage("Configuration was not reloaded, either because it wasn't modified or because it is not in valid format (see console for details)");
                }
                break;
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

                                StringBuilder send = new StringBuilder("Mehrere Clients mit diesem Namen wurden gefunden:\n");
                                for (Client client : clients) {
                                    send.append(" ID: ")
                                            .append(client.getId())
                                            .append(" - Nickname: ")
                                            .append(client.getNickname())
                                            .append("\n");
                                }
                                sendMessage(send.toString());
                                return;
                            }
                        }
                    }

                    if (toLookup == null) {
                        sendMessage("Kein Client gefunden mit diesem Namen oder ID.");
                        return;
                    }

                    StringBuilder send = new StringBuilder(toLookup.getNickname() + ":\n");
                    for (Map.Entry<String, String> entry : toLookup.getMap().entrySet()) {
                        if (entry.getValue().length() == 0) continue;
                        send.append(" ").append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
                    }

                    // send in 1024 character chunks
                    String toSend = send.toString();
                    for (int i = 0; i < toSend.length(); i += 1023) {
                        sendMessage(toSend.substring(i, Math.min(i + 1023, toSend.length())));
                    }
                    return;
                } else sendUsage();
            default:
                sendUsage();
        }
    }

    private void sendUsage() {
        sendMessage("Verwendung: !admin <lookup|reload|restart|stop>");
    }
}
