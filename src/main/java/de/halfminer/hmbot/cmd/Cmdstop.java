package de.halfminer.hmbot.cmd;

import de.halfminer.hmbot.util.StringArgumentSeparator;

/**
 * Created by fabpw on 24.03.2017.
 */
@SuppressWarnings("unused")
public class Cmdstop extends Command {

    public Cmdstop(int clientId, StringArgumentSeparator command) {
        super(clientId, command);
    }

    @Override
    void run() throws CommandNotCompletedException {
        //TODO implement after permission system in place
        //bot.stop("Bot was stopped via command by client " + api.getClientInfo(clientId).getLoginName());
    }
}
