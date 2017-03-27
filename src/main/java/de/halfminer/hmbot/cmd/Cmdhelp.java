package de.halfminer.hmbot.cmd;

import de.halfminer.hmbot.HalfminerBot;
import de.halfminer.hmbot.util.StringArgumentSeparator;

@SuppressWarnings("unused")
public class Cmdhelp extends Command {

    public Cmdhelp(int clientId, StringArgumentSeparator command) {
        super(clientId, command);
    }

    @Override
    void run() {
        sendMessage("HalfminerBot v" + HalfminerBot.getVersion() + " von Kakifrucht - © halfminer.de\n \n" +
                "Verfügbare Kommandos: \n!channel <create|update> <passwort> -> erstelle einen eigenen Channel");
    }
}
