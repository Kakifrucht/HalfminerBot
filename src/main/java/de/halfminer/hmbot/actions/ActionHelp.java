package de.halfminer.hmbot.actions;

import de.halfminer.hmbot.exception.InvalidCommandLineException;
import de.halfminer.hmbot.util.CommandLine;

public class ActionHelp extends Action {

    public ActionHelp(CommandLine command) throws InvalidCommandLineException {
        super(command);
    }

    @Override
    public void run() {
        api.sendPrivateMessage(command.getClientId(),
                "HalfminerBot - halfminer.de | Verfügbare Kommandos: !channelcreate <passwort> -> erstelle einen eigenen Channel");
    }
}
