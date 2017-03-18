package de.halfminer.hmbot.actions;

import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import de.halfminer.hmbot.HalfminerBotClass;
import de.halfminer.hmbot.storage.BotConfig;
import de.halfminer.hmbot.exception.ActionNotCompletedException;
import de.halfminer.hmbot.util.CommandLine;

public abstract class Action extends HalfminerBotClass {

    final BotConfig botConfig;

    //Variable information about the action
    final CommandLine command;
    final ClientInfo invoker;

    Action(CommandLine command) {
        this.botConfig = bot.getBotConfig();

        this.command = command;
        this.invoker = api.getClientInfo(command.getClientId());
    }

    /**
     * Run instantianted Action
     *
     * @throws ActionNotCompletedException - When the action wasn't completed properly
     */
    public abstract void run() throws ActionNotCompletedException;

    public CommandLine getCommand() {
        return this.command;
    }

    public ClientInfo getClientInfo() {
        return this.invoker;
    }
}
