package de.halfminer.hmbot.cmd;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.halfminer.hmbot.HalfminerBotClass;
import de.halfminer.hmbot.storage.Storage;
import de.halfminer.hmbot.util.MessageBuilder;
import de.halfminer.hmbot.util.StringArgumentSeparator;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

public class CommandDispatcher extends HalfminerBotClass {

    private final Storage storage = bot.getStorage();
    private final Cache<Integer, Boolean> floodProtection = CacheBuilder.newBuilder()
            .concurrencyLevel(2)
            .expireAfterWrite(2, TimeUnit.SECONDS)
            .build();

    public void dispatchCommand(String clientName, int clientId, String commandUnparsed) {

        if (floodProtection.getIfPresent(clientId) != null) {
            MessageBuilder.create("cmdDispatcherFloodLimit").sendMessage(clientId);
            return;
        }

        // set default command to !channel create
        String commandUnparsedEdit = commandUnparsed;
        if (!commandUnparsed.startsWith("!")) {
            commandUnparsedEdit = "!channel create " + commandUnparsed;
        }

        StringArgumentSeparator command = new StringArgumentSeparator(commandUnparsedEdit);
        if (!command.meetsLength(1)) return;

        floodProtection.put(clientId, true);
        logger.info("Client {} issued server command: {}", clientName, command.getConcatenatedString());
        CommandEnum commandEnum = CommandEnum.getCommand(command.getArgument(0));

        if (commandEnum == null) {
            MessageBuilder.create("cmdDispatcherUnknownCmd").sendMessage(clientId);
            return;
        }

        try {
            Class<?> classLoaded = this.getClass()
                    .getClassLoader()
                    .loadClass(commandEnum.getReflectionPath());

            if (!storage.getClient(clientId).hasPermission(commandEnum.getPermission())) {
                MessageBuilder.create("cmdDispatcherNoPermission").sendMessage(clientId);
                return;
            }

            Command cmdInstance = (Command) classLoaded
                    .getConstructor(int.class, StringArgumentSeparator.class)
                    .newInstance(clientId, command);

            cmdInstance.run();

        } catch (InvocationTargetException e) {

            if (e.getCause() instanceof InvalidCommandException) {
                sendUsage((InvalidCommandException) e.getCause(), clientId);
            } else {
                logErrorAndMessage(e, clientId);
            }

        } catch (InvalidCommandException e) {
            sendUsage(e, clientId);
        } catch (Throwable e) {
            logErrorAndMessage(e, clientId);
        }
    }

    private void sendUsage(InvalidCommandException e, int clientId) {
        MessageBuilder.create("cmdDispatcherUsage")
                .addPlaceholderReplace("USAGE", e.getUsage())
                .sendMessage(clientId);
    }

    private void logErrorAndMessage(Throwable e, int clientId) {
        logger.error("Exception during newInstance() of command", e);
        MessageBuilder.create("cmdDispatchedUnknownError").sendMessage(clientId);
    }
}
