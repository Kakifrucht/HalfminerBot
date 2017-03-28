package de.halfminer.hmbot.cmd;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.halfminer.hmbot.HalfminerBotClass;
import de.halfminer.hmbot.storage.Storage;
import de.halfminer.hmbot.util.StringArgumentSeparator;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

public class CommandDispatcher extends HalfminerBotClass {

    private final Storage storage = bot.getStorage();
    private final Cache<Integer, Boolean> floodProtection = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.SECONDS)
            .build();

    public void dispatchCommand(String clientName, int clientId, String commandUnparsed) {

        if (floodProtection.getIfPresent(clientId) != null) {
            api.sendPrivateMessage(clientId, "Bitte warte einen kurzen Moment und versuche es danach erneut.");
            return;
        }

        // set default command to !channel
        String commandUnparsedEdit = commandUnparsed;
        if (!commandUnparsed.startsWith("!")) {
            commandUnparsedEdit = "!channel create " + commandUnparsed;
        }

        StringArgumentSeparator command = new StringArgumentSeparator(commandUnparsedEdit);
        if (!command.meetsLength(1)) return;

        floodProtection.put(clientId, true);
        logger.info("Client {} issued server command: {}", clientName, command.getConcatenatedString());

        try {
            String commandRefl = command.getArgument(0).substring(1).toLowerCase();
            String className = "Cmd" + commandRefl;
            Class<?> classLoaded = this.getClass()
                    .getClassLoader()
                    .loadClass("de.halfminer.hmbot.cmd." + className);

            if (!storage.getClient(clientId).hasPermission("cmd." + commandRefl)) {
                api.sendPrivateMessage(clientId, "Du hast keine Berechtigung dies zu nutzen");
                return;
            }

            Command cmdInstance = (Command) classLoaded
                    .getConstructor(int.class, StringArgumentSeparator.class)
                    .newInstance(clientId, command);

            cmdInstance.run();

        } catch (InvocationTargetException e) {

            if (e.getCause() instanceof InvalidCommandLineException) {
                InvalidCommandLineException ex = (InvalidCommandLineException) e.getCause();
                api.sendPrivateMessage(clientId, "Verwendung: " + ex.getCorrectUsage());
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
        } catch (Throwable e) {
            errorLogAndTell(e, clientId);
        }
    }

    private void errorLogAndTell(Throwable e, int clientId) {
        logger.error("Exception during newInstance() of command", e);
        api.sendPrivateMessage(clientId, "Ein unbekannter Fehler ist aufgetreten. Bitte wende dich an ein Teammitglied.");
    }
}
