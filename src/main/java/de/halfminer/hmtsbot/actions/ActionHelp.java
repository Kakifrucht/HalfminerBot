package de.halfminer.hmtsbot.actions;

import de.halfminer.hmtsbot.exception.InvalidCommandLineException;

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
