package de.halfminer.hmtsbot;

import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode;
import com.github.theholywaffle.teamspeak3.api.event.*;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import de.halfminer.hmtsbot.actions.ActionManager;

class HalfminerBotListener extends TS3EventAdapter {

    private final HalfminerBot bot;
    private final ActionManager actions;

    private final int channelOfBot;

    public HalfminerBotListener() {

        this.bot = HalfminerBot.getInstance();
        this.actions = bot.getActionmanager();

        this.channelOfBot = bot.getApi().whoAmI().getChannelId();
    }

    @Override
    public void onClientJoin(ClientJoinEvent e) {
        // Make sure a user joins (not a query)
        ClientInfo client = bot.getApi().getClientInfo(e.getClientId());
        if (client != null && client.isRegularClient()) messageUser(client.getId());
    }

    @Override
    public void onClientMoved(ClientMovedEvent e) {
        // If the user joins the bots channel (not when he leaves)
        if (this.channelOfBot == e.getClientTargetId()) messageUser(e.getClientId());
    }

    @Override
    public void onTextMessage(TextMessageEvent e) {
        // Disregard messages the bot creates and make sure it's in the private chat
        if (e.getInvokerId() != bot.getStorage().getBotInformation().getId()
                && e.getTargetMode().equals(TextMessageTargetMode.CLIENT))
            actions.parseAction(e);
    }

    private void messageUser(int clientId) {
        if (!bot.getStorage().moveToChannel(clientId))
            bot.getApi().sendPrivateMessage(clientId,
                    "Antworte mit deinem gewünschten Passwort, um einen eigenen Channel mit Passwort zu erhalten.");
    }
}
