package de.halfminer.hmbot.cmd;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.halfminer.hmbot.HalfminerBotClass;
import de.halfminer.hmbot.cmd.abs.Command;
import de.halfminer.hmbot.exception.CommandNotCompletedException;
import de.halfminer.hmbot.exception.InvalidCommandLineException;
import de.halfminer.hmbot.util.StringArgumentSeparator;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

public class CommandDispatcher extends HalfminerBotClass {

    private final Cache<Integer, Boolean> floodProtection = CacheBuilder.newBuilder()
            .expireAfterAccess(2, TimeUnit.SECONDS)
            .build();

    public void passCommand(String clientName, int clientId, String commandUnparsed) {

        if (floodProtection.getIfPresent(clientId) != null) {
            api.sendPrivateMessage(clientId, "Bitte warte einen Moment.");
            return;
        }

        StringArgumentSeparator command = new StringArgumentSeparator(commandUnparsed);
        if (!command.meetsLength(1)) return;

        logger.info("User " + clientName + " issued server command: " + command.getConcatenatedString());

        try {
            Command cmdInstance = (Command) this.getClass()
                    .getClassLoader()
                    .loadClass("de.halfminer.hmbot.cmdInstance.Cmd" + command.getArgument(0).substring(1))
                    .getConstructor(Integer.class, StringArgumentSeparator.class)
                    .newInstance(clientId, command);

            cmdInstance.run();
            floodProtection.put(clientId, true);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof InvalidCommandLineException) {
                InvalidCommandLineException ex = (InvalidCommandLineException) e.getCause();
                api.sendPrivateMessage(clientId, ex.getError() + " | Verwendung: " + ex.getCorrectUsage());
            } else {
                logger.error("Unknown exception during newInstance() on class.", e);
            }
        } catch (CommandNotCompletedException e) {
            if (e.tellUser()) {
                api.sendPrivateMessage(clientId, e.toTellUser());
            }
            logger.warn(e.getError());
        } catch (Exception e) {
            api.sendPrivateMessage(clientId, "Unbekanntes Kommando. Verwende !help für eine Befehlsübersicht.");
        }
    }
}
