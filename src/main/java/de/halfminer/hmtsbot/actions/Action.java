package de.halfminer.hmtsbot.actions;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import de.halfminer.hmtsbot.HalfminerConfig;
import de.halfminer.hmtsbot.HalfminerBot;
import de.halfminer.hmtsbot.exception.ActionNotCompletedException;
import de.halfminer.hmtsbot.exception.InvalidCommandLineException;

public abstract class Action {

    final HalfminerBot bot;
    final TS3Api api;
    final HalfminerConfig config;

    //Variable information about the action
    final CommandLine command;
    final ClientInfo invoker;

    Action(CommandLine command) throws InvalidCommandLineException {
        this.bot = HalfminerBot.getInstance();
        this.api = bot.getApi();
        this.config = bot.getConfig();

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
