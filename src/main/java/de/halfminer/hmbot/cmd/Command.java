package de.halfminer.hmbot.cmd;

import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import de.halfminer.hmbot.HalfminerBotClass;
import de.halfminer.hmbot.storage.Storage;
import de.halfminer.hmbot.util.StringArgumentSeparator;

abstract class Command extends HalfminerBotClass {

    final Storage storage = bot.getStorage();

    final int clientId;
    final ClientInfo invoker;

    private final String commandFull;
    final StringArgumentSeparator command;

    @SuppressWarnings("WeakerAccess")
    public Command(int clientId, StringArgumentSeparator command) {

        this.clientId = clientId;
        this.invoker = api.getClientInfo(clientId);

        this.commandFull = command.getConcatenatedString();
        this.command = command.removeFirstElement();
    }

    /**
     * Run instantianted Action.
     *
     * @throws CommandNotCompletedException when the action wasn't completed properly
     */
    abstract void run() throws CommandNotCompletedException;

    void sendMessage(String message) {
        api.sendPrivateMessage(clientId, message);
    }

    String getCommand() {
        return commandFull;
    }

    ClientInfo getClientInfo() {
        return this.invoker;
    }
}
