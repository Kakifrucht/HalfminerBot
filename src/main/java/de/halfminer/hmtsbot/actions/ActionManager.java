package de.halfminer.hmtsbot.actions;

import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import de.halfminer.hmtsbot.HalfminerBot;
import de.halfminer.hmtsbot.actions.Action;
import de.halfminer.hmtsbot.actions.ActionChannelCreate;
import de.halfminer.hmtsbot.actions.ActionHelp;
import de.halfminer.hmtsbot.actions.CommandLine;
import de.halfminer.hmtsbot.exception.ActionNotCompletedException;
import de.halfminer.hmtsbot.exception.InvalidCommandLineException;

public class ActionManager {

    private final HalfminerBot bot;

    public ActionManager() {
        this.bot = HalfminerBot.getInstance();
    }

    public void parseAction(TextMessageEvent event) {

        CommandLine command = new CommandLine(event);
        HalfminerBot.getLogger().info("User " + event.getInvokerName() + " issued server command: " + command.getLine());

        Action action = null;

        try {
            switch (command.getCommand().toLowerCase()) {
                case "help":
                    action = new ActionHelp(command);
                    break;
                case "channelcreate":
                    action = new ActionChannelCreate(command);
                    break;
            }
        } catch (InvalidCommandLineException e) {
            bot.getApi().sendPrivateMessage(event.getInvokerId(), e.getError() + " | Verwendung: " + e.getCorrectUsage());
            return;
        }

        if (action == null) {
            bot.getApi().sendPrivateMessage(event.getInvokerId(),
                    "Unbekanntes Kommando. Verwende !help für eine Befehlsübersicht.");
            return;
        }

        try {
            runAction(action);
        } catch (ActionNotCompletedException e) {
            HalfminerBot.getLogger().info(e.getError());
        }
    }

    private void runAction(Action action) throws ActionNotCompletedException {
        action.run();
    }
}
