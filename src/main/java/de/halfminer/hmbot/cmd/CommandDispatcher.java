package de.halfminer.hmbot.cmd;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.halfminer.hmbot.HalfminerBotClass;
import de.halfminer.hmbot.util.StringArgumentSeparator;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

public class CommandDispatcher extends HalfminerBotClass {

    private final Cache<Integer, Boolean> floodProtection = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.SECONDS)
            .build();

    public void dispatchCommand(String clientName, int clientId, String commandUnparsed) {

        if (floodProtection.getIfPresent(clientId) != null) {
            api.sendPrivateMessage(clientId, "Bitte warte einen kurzen Moment und versuche es danach erneut.");
            return;
        }

        // set default command to !channelcreate
        String commandUnparsedEdit = commandUnparsed;
        if (!commandUnparsed.startsWith("!")) {
            commandUnparsedEdit = "!channelcreate " + commandUnparsed;
        }

        StringArgumentSeparator command = new StringArgumentSeparator(commandUnparsedEdit);
        if (!command.meetsLength(1)) return;

        logger.info("Client {} issued server command: {}", clientName, command.getConcatenatedString());

        try {
            String className = "Cmd" + command.getArgument(0).substring(1).toLowerCase();
            Command cmdInstance = (Command) this.getClass()
                    .getClassLoader()
                    .loadClass("de.halfminer.hmbot.cmd." + className)
                    .getConstructor(int.class, StringArgumentSeparator.class)
                    .newInstance(clientId, command);

            cmdInstance.run();
            floodProtection.put(clientId, true);

        } catch (InvocationTargetException e) {

            if (e.getCause() instanceof InvalidCommandLineException) {
                InvalidCommandLineException ex = (InvalidCommandLineException) e.getCause();
                api.sendPrivateMessage(clientId, ex.getError() + " | Verwendung: " + ex.getCorrectUsage());
            } else {
                errorLogAndTell(e, clientId);
            }

        } catch (CommandNotCompletedException e) {

            if (e.doTellUser()) {
                api.sendPrivateMessage(clientId, e.toTellUser());
            }

            logger.warn(e.getError());

        } catch (ClassNotFoundException e) {
            api.sendPrivateMessage(clientId, "Unbekanntes Kommando. Verwende !help für eine Befehlsübersicht.");
        } catch (Exception e) {
            errorLogAndTell(e, clientId);
        }
    }

    private void errorLogAndTell(Exception e, int clientId) {
        logger.error("Exception during newInstance() of command", e);
        api.sendPrivateMessage(clientId, "Ein unbekannter Fehler ist aufgetreten. Bitte wende dich an ein Teammitglied");
    }
}
