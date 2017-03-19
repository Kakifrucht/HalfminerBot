package de.halfminer.hmbot.cmd.abs;

import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import de.halfminer.hmbot.HalfminerBot;
import de.halfminer.hmbot.HalfminerBotClass;
import de.halfminer.hmbot.exception.CommandNotCompletedException;
import de.halfminer.hmbot.storage.BotConfig;
import de.halfminer.hmbot.util.StringArgumentSeparator;

public abstract class Command extends HalfminerBotClass {

    protected final static BotConfig botConfig = HalfminerBot.getInstance().getBotConfig();

    //Variable information about the action
    protected final int clientId;
    protected final ClientInfo invoker;

    private final String commandFull;
    protected final StringArgumentSeparator commandLine;

    public Command(int clientId, StringArgumentSeparator command) {

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
    public abstract void run() throws CommandNotCompletedException;

    public String getCommand() {
        return commandFull;
    }

    public ClientInfo getClientInfo() {
        return this.invoker;
    }
}
