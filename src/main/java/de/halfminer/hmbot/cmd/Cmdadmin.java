package de.halfminer.hmbot.cmd;

import de.halfminer.hmbot.util.StringArgumentSeparator;

/**
 * - Reload the config file
 *   - Won't reload if file was not modified or if it is in invalid format
 * - Restart (full reconnect) or stop the bot entirely
 */
@SuppressWarnings("unused")
public class Cmdadmin extends Command {

    public Cmdadmin(int clientId, StringArgumentSeparator command) throws InvalidCommandLineException {
        super(clientId, command);

        if (!command.meetsLength(1)) {
            throw new InvalidCommandLineException("!admin <reload|restart|stop>");
        }
    }

    @Override
    void run() throws CommandNotCompletedException {
        String argument = commandLine.getArgument(0);
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
            default:
                sendMessage("Verwendung: !admin <reload|restart|stop>");
        }
    }
}
