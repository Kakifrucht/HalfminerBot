package de.halfminer.hmbot.cmd;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.halfminer.hmbot.BotClass;
import de.halfminer.hmbot.storage.HalfClient;
import de.halfminer.hmbot.storage.Storage;
import de.halfminer.hmbot.util.MessageBuilder;
import de.halfminer.hmbot.util.StringArgumentSeparator;

import java.util.concurrent.TimeUnit;

/**
 * Command dispatcher, parses given chat input and executes the corresponding {@link Command}.
 * Also does permission, flood and command limit checks, logs commands, sets default command
 * and sends correct command usage, if invalid, to client.
 */
public class CommandDispatcher extends BotClass {

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
        if (!command.meetsLength(1)) {
            return;
        }

        logger.info("Client {} issued server command: {}", clientName, command.getConcatenatedString());

        HalfClient sender = storage.getClient(clientId);
        CommandEnum commandEnum = CommandEnum.getCommand(command.getArgument(0));

        if (!sender.hasPermission("cmd.bypass.flood")) {
            floodProtection.put(clientId, true);
        }

        if (commandEnum == null) {
            MessageBuilder.create("cmdDispatcherUnknownCmd").sendMessage(clientId);
            return;
        }

        if (!sender.hasPermission(commandEnum.getPermission())) {
            MessageBuilder.create("cmdDispatcherNoPermission").sendMessage(clientId);
            return;
        }

        try {
            Command cmd = commandEnum.getInstance(sender, command);
            long cooldown = sender.getCooldown(cmd.getClass());
            if (cooldown > 0) {
                MessageBuilder.create("cmdDispatcherCooldown")
                        .addPlaceholderReplace("TIME", String.valueOf(cooldown))
                        .sendMessage(clientId);
                return;
            }

            cmd.run();

        } catch (InvalidCommandException e) {
            if (e.hasCause()) {
                logger.error("Exception during newInstance() of command", e);
                MessageBuilder.create("cmdDispatchedUnknownError").sendMessage(clientId);
            } else {
                MessageBuilder.create("cmdDispatcherUsage")
                        .addPlaceholderReplace("USAGE", e.getUsage())
                        .sendMessage(clientId);
            }
        }
    }
}
