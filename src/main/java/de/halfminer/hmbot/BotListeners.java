package de.halfminer.hmbot;

import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode;
import com.github.theholywaffle.teamspeak3.api.event.*;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerQueryInfo;
import de.halfminer.hmbot.cmd.CommandDispatcher;
import de.halfminer.hmbot.config.BotConfig;
import de.halfminer.hmbot.storage.HalfClient;
import de.halfminer.hmbot.storage.Storage;
import de.halfminer.hmbot.util.MessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event listeners for bot. Passes command to {@link CommandDispatcher} and contacts the client on server/channel join.
 */
class BotListeners extends TS3EventAdapter {

    private final static Logger logger = LoggerFactory.getLogger(BotListeners.class);

    private final ComponentHolder componentHolder = HalfminerBot.getComponentHolder();
    private final BotConfig config = componentHolder.getConfig();
    private final Storage storage = componentHolder.getStorage();
    private final CommandDispatcher cmd = new CommandDispatcher();

    private final TS3ApiWrapper apiWrapper;

    private final int idOfBot;
    private final int channelOfBot;

    BotListeners() {
        this.apiWrapper = componentHolder.getApiWrapper();
        ServerQueryInfo info = apiWrapper.getTS3Api().whoAmI();
        idOfBot = info.getId();
        channelOfBot = info.getChannelId();
    }

    @Override
    public void onTextMessage(TextMessageEvent e) {
        // Disregard messages the bot creates and make sure it's in the private chat
        if (e.getInvokerId() != idOfBot
                && e.getTargetMode().equals(TextMessageTargetMode.CLIENT)
                && isRegularClient(e.getInvokerId())) {
            cmd.dispatchCommand(e.getInvokerName(), e.getInvokerId(), e.getMessage());
        }
    }

    @Override
    public void onClientJoin(ClientJoinEvent e) {

        ClientInfo joined;
        try {
            joined = apiWrapper.getTS3Api().getClientInfo(e.getClientId());
        } catch (TS3CommandFailedException ex) {
            logger.warn("ClientInfo after join could not be pulled: {}", ex.getMessage());
            return;
        }

        if (joined.isRegularClient()) {
            storage.clientJoinedOrReloaded(joined);
            if (config.getBoolean("messageOnJoin")) {
                clientEnterMessageAndMove(e.getClientId());
            }
        }
    }

    @Override
    public void onClientLeave(ClientLeaveEvent e) {
        storage.clientLeft(e.getClientId());
    }

    @Override
    public void onClientMoved(ClientMovedEvent e) {

        if (!isRegularClient(e.getClientId()))
            return;

        // If client joins the bots channel (not when he leaves), also move bot back to his channel if moved out
        if (this.channelOfBot == e.getTargetChannelId()) {
            clientEnterMessageAndMove(e.getClientId());
        } else if (e.getClientId() == idOfBot && e.getTargetChannelId() != this.channelOfBot) {
            try {
                componentHolder.getApiWrapper().getTS3Api().moveQuery(channelOfBot);
            } catch (TS3CommandFailedException ex) {
                logger.warn("Couldn't move bot back to his channel");
            }
        }
    }

    private boolean isRegularClient(int clientId) {
        return apiWrapper.getTS3Api().getClientInfo(clientId).isRegularClient();
    }

    private void clientEnterMessageAndMove(int clientId) {
        clientEnterMessageAndMove(apiWrapper.getTS3Api().getClientInfo(clientId));
    }

    private void clientEnterMessageAndMove(ClientInfo client) {
        HalfClient hClient = storage.getClient(client);
        if (hClient.hasPermission("bot.chat")
                && !hClient.moveToChannel(client.getId())) {
            MessageBuilder.create("joinMessage")
                    .addPlaceholderReplace("NICKNAME", client.getNickname())
                    .sendMessage(client.getId());
        }
    }
}
