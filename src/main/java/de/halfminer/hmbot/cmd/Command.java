package de.halfminer.hmbot.cmd;

import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import de.halfminer.hmbot.HalfminerBot;
import de.halfminer.hmbot.HalfminerBotClass;
import de.halfminer.hmbot.storage.YamlConfig;
import de.halfminer.hmbot.util.StringArgumentSeparator;

abstract class Command extends HalfminerBotClass {

    final static YamlConfig botConfig = HalfminerBot.getInstance().getBotConfig();

    //Variable information about the action
    final int clientId;
    final ClientInfo invoker;

    private final String commandFull;
    final StringArgumentSeparator commandLine;

    Command(int clientId, StringArgumentSeparator command) {

        this.clientId = clientId;
        this.invoker = api.getClientInfo(clientId);

        this.commandFull = command.getConcatenatedString();
        this.commandLine = command.removeFirstElement();
    }

    /**
     * Run instantianted Action
     *
     * @throws CommandNotCompletedException when the action wasn't completed properly
     */
    abstract void run() throws CommandNotCompletedException;

    String getCommand() {
        return commandFull;
    }

    ClientInfo getClientInfo() {
        return this.invoker;
    }
}
