package de.halfminer.hmbot.cmd;

import de.halfminer.hmbot.util.StringArgumentSeparator;

@SuppressWarnings("unused")
public class Cmdhelp extends Command {

    Cmdhelp(int clientId, StringArgumentSeparator command) throws InvalidCommandLineException {
        super(clientId, command);
    }

    @Override
    void run() {
        api.sendPrivateMessage(clientId,
                "HalfminerBot - halfminer.de | Verfügbare Kommandos: !channelcreate <passwort> -> erstelle einen eigenen Channel");
    }
}
