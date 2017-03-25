package de.halfminer.hmbot.cmd;

import de.halfminer.hmbot.util.StringArgumentSeparator;

/**
 * Command to stop the bot.
 */
@SuppressWarnings("unused")
public class Cmdstop extends Command {

    public Cmdstop(int clientId, StringArgumentSeparator command) {
        super(clientId, command);
    }

    @Override
    void run() {
        bot.stop("Bot was stopped via command by client " + api.getClientInfo(clientId).getNickname());
    }
}
