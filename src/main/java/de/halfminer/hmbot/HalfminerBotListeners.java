package de.halfminer.hmbot;

import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode;
import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientMovedEvent;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerQueryInfo;
import de.halfminer.hmbot.cmd.CommandDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HalfminerBotListeners extends TS3EventAdapter {

    private final static Logger logger = LoggerFactory.getLogger(HalfminerBotListeners.class);

    private final HalfminerBot bot = HalfminerBot.getInstance();
    private final CommandDispatcher cmd = new CommandDispatcher();

    private final int idOfBot;
    private final int channelOfBot;

    HalfminerBotListeners() {
        ServerQueryInfo info = bot.getApi().whoAmI();
        idOfBot = info.getId();
        channelOfBot = info.getChannelId();
    }

    @Override
    public void onTextMessage(TextMessageEvent e) {
        // Disregard messages the bot creates and make sure it's in the private chat
        if (e.getInvokerId() != idOfBot && e.getTargetMode().equals(TextMessageTargetMode.CLIENT)) {
            cmd.passCommand(e.getInvokerName(), e.getInvokerId(), e.getMessage());
        }
    }

    @Override
    public void onClientJoin(ClientJoinEvent e) {
        ClientInfo client = bot.getApi().getClientInfo(e.getClientId());
        if (client != null && client.isRegularClient()) clientEnterMessageAndMove(client.getId());
    }

    @Override
    public void onClientMoved(ClientMovedEvent e) {
        // If client joins the bots channel (not when he leaves), also move bot back to his channel if moved out
        if (this.channelOfBot == e.getTargetChannelId()) {
            clientEnterMessageAndMove(e.getClientId());
        } else if (e.getClientId() == idOfBot && e.getTargetChannelId() != this.channelOfBot) {
            boolean moveBackSuccessful = bot.getApi().moveClient(idOfBot, channelOfBot);
            if (!moveBackSuccessful) {
                logger.warn("Couldn't move bot back to his channel");
            }
        }
    }

    private void clientEnterMessageAndMove(int clientId) {
        if (!bot.getStorage().moveToChannel(clientId)) {
            bot.getApi().sendPrivateMessage(clientId,
                    "Antworte mit deinem gewünschten Passwort, um einen eigenen Channel mit Passwort zu erhalten.");
        }
    }
}
