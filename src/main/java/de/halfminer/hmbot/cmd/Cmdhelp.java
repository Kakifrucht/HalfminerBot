package de.halfminer.hmbot.cmd;

import de.halfminer.hmbot.util.StringArgumentSeparator;

@SuppressWarnings("unused")
public class Cmdhelp extends Command {

    public Cmdhelp(int clientId, StringArgumentSeparator command) {
        super(clientId, command);
    }

    @Override
    void run() {
        sendMessage("HalfminerBot von Kakifrucht - © halfminer.de\n \n" +
                "Verfügbare Kommandos: \n!channelcreate <passwort> -> erstelle einen eigenen Channel");
    }
}
