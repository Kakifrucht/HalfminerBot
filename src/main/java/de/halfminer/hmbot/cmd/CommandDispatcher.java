package de.halfminer.hmbot.cmd;

import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
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

    private final Storage storage = componentHolder.getStorage();
    private final Cache<Integer, Boolean> floodProtection = CacheBuilder.newBuilder()
            .concurrencyLevel(2)
            .expireAfterWrite(2, TimeUnit.SECONDS)
            .build();

    public void dispatchCommand(String clientName, int clientId, String commandUnparsed) {

        if (floodProtection.getIfPresent(clientId) != null) {
            MessageBuilder.create("cmdDispatcherFloodLimit").sendMessage(clientId);
            return;
        }

        // set default command
        String commandUnparsedEdited = commandUnparsed;
        if (!commandUnparsed.startsWith("!")) {
            String commandUnparsedFromLocale = MessageBuilder.create("cmdDispatcherDefaultCommand")
                    .addPlaceholderReplace("%MESSAGE%", commandUnparsed)
                    .returnMessage();

            if (commandUnparsedFromLocale.startsWith("!")) {
                commandUnparsedEdited = commandUnparsedFromLocale;
            }
        }

        StringArgumentSeparator command = new StringArgumentSeparator(commandUnparsedEdited);
        if (!command.meetsLength(1)) {
            return;
        }

        String commandLog = command.getConcatenatedString().replace("\n", "\\n");
        logger.info("Client {} issued server command: {}", clientName, commandLog);

        ClientInfo clientInfo = getTS3Api().getClientInfo(clientId);
        HalfClient client = storage.getClient(clientInfo);

        CommandEnum commandEnum = CommandEnum.getCommand(command.getArgument(0));

        if (!client.hasPermission("cmd.bypass.flood")) {
            floodProtection.put(clientId, true);
        }

        if (commandEnum == null) {
            MessageBuilder.create("cmdDispatcherUnknownCmd").sendMessage(clientId);
            return;
        }

        if (!client.hasPermission(commandEnum.getPermission())) {
            MessageBuilder.create("cmdDispatcherNoPermission").sendMessage(clientId);
            return;
        }

        try {
            Command cmd = commandEnum.getInstance(client, clientInfo, command);
            long cooldown = client.getCooldown(cmd.getClass());
            if (cooldown > 0) {
                MessageBuilder.create("cmdDispatcherCooldown")
                        .addPlaceholderReplace("TIME", String.valueOf(cooldown))
                        .sendMessage(clientId);
                return;
            }

            cmd.run();

        } catch (InvalidCommandException e) {
            if (e.hasCause()) {
                logger.error("Exception during newInstance() for command '{}'", commandUnparsedEdited, e);
                MessageBuilder.create("cmdDispatcherUnknownError").sendMessage(clientId);
            } else {
                MessageBuilder.create("cmdDispatcherUsage")
                        .addPlaceholderReplace("USAGE", e.getUsage())
                        .sendMessage(clientId);
            }
        } catch (TS3CommandFailedException e) {
            logger.error("Exception during execution of command '{}'", commandUnparsedEdited, e);
            MessageBuilder.create("cmdDispatcherUnknownError").sendMessage(clientId);
        }
    }
}
