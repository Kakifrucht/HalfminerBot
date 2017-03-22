package de.halfminer.hmbot;

import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode;
import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientMovedEvent;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerQueryInfo;
import de.halfminer.hmbot.cmd.CommandDispatcher;

class HalfminerBotListeners extends TS3EventAdapter {

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
        // If the user joins the bots channel (not when he leaves)
        if (this.channelOfBot == e.getTargetChannelId()) clientEnterMessageAndMove(e.getClientId());
        //TODO move back if bot is moved
    }

    private void clientEnterMessageAndMove(int clientId) {
        if (!bot.getStorage().moveToChannel(clientId)) {
            bot.getApi().sendPrivateMessage(clientId,
                    "Antworte mit deinem gewünschten Passwort, um einen eigenen Channel mit Passwort zu erhalten.");
        }
    }
}
