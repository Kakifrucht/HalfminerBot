package de.halfminer.hmbot.cmd;

import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import de.halfminer.hmbot.HalfminerBotClass;
import de.halfminer.hmbot.storage.HalfClient;
import de.halfminer.hmbot.storage.Storage;
import de.halfminer.hmbot.util.MessageBuilder;
import de.halfminer.hmbot.util.StringArgumentSeparator;

/**
 * Command base class dispatched by {@link CommandDispatcher}.
 */
abstract class Command extends HalfminerBotClass {

    final Storage storage = bot.getStorage();

    final HalfClient client;
    final int clientId;
    final ClientInfo clientInfo;

    final StringArgumentSeparator command;

    @SuppressWarnings("WeakerAccess")
    public Command(HalfClient client, StringArgumentSeparator command) {

        this.client = client;
        this.clientInfo = client.getClientInfo();
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
}
