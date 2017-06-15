package de.halfminer.hmbot.cmd;

import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import de.halfminer.hmbot.BotClass;
import de.halfminer.hmbot.storage.HalfClient;
import de.halfminer.hmbot.storage.Storage;
import de.halfminer.hmbot.util.MessageBuilder;
import de.halfminer.hmbot.util.StringArgumentSeparator;

/**
 * Command base class dispatched by {@link CommandDispatcher}.
 */
abstract class Command extends BotClass {

    final Storage storage = componentHolder.getStorage();

    final HalfClient client;
    final ClientInfo clientInfo;
    final int clientId;

    final StringArgumentSeparator command;

    @SuppressWarnings("WeakerAccess")
    public Command(HalfClient client, ClientInfo clientInfo, StringArgumentSeparator command) {

        this.client = client;
        this.clientInfo = clientInfo;
        this.clientId = clientInfo.getId();

        this.command = command.removeFirstElement();
    }

    /**
     * Execute command.
     */
    abstract void run() throws InvalidCommandException;

    void sendMessage(String messageKey, String... placeholders) {
        MessageBuilder builder = MessageBuilder.create(messageKey);
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            builder.addPlaceholderReplace(placeholders[i], placeholders[i + 1]);
        }
        builder.sendMessage(clientId);
    }

    void addCooldown(int cooldownSeconds) {
        client.addCooldown(getClass(), cooldownSeconds);
    }
}
