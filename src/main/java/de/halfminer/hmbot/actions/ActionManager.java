package de.halfminer.hmbot.actions;

import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import de.halfminer.hmbot.HalfminerBotClass;
import de.halfminer.hmbot.exception.ActionNotCompletedException;
import de.halfminer.hmbot.exception.InvalidCommandLineException;
import de.halfminer.hmbot.util.CommandLine;

public class ActionManager extends HalfminerBotClass {

    public void parseAction(TextMessageEvent event) {

        CommandLine command = new CommandLine(event);
        logger.info("User " + event.getInvokerName() + " issued server command: " + command.getLine());

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
            api.sendPrivateMessage(event.getInvokerId(), e.getError() + " | Verwendung: " + e.getCorrectUsage());
            return;
        }

        if (action == null) {
            api.sendPrivateMessage(event.getInvokerId(),
                    "Unbekanntes Kommando. Verwende !help für eine Befehlsübersicht.");
            return;
        }

        try {
            runAction(action);
        } catch (ActionNotCompletedException e) {
            logger.info(e.getError());
        }
    }

    private void runAction(Action action) throws ActionNotCompletedException {
        action.run();
    }
}
