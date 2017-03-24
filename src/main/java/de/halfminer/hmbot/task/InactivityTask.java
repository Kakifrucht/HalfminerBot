package de.halfminer.hmbot.task;

import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Will be run at fixed interval and move AFK users to AFK channel.
 */
class InactivityTask extends Task {

    private final static int IDLE_TIME_UNTIL_MOVE_IN_SECONDS = 300;

    private int maxClients;
    private Channel afkChannel;

    InactivityTask() {
        super(10, 10, TimeUnit.SECONDS);
    }

    @Override
    boolean checkIfEnabled() {
        maxClients = bot.getApi().getServerInfo().getMaxClients();
        // suppose that first channel containing the word afk is the designated one
        for (Channel channel : bot.getApi().getChannelsByName("AFK")) {
            if (channel.getNeededTalkPower() > 0) {
                afkChannel = channel;
                return true;
            }
        }
        return false;
    }

    @Override
    public void execute() {

        List<Client> clients = api.getClients();

        for (Client client : clients) {
            if (client.getChannelId() != afkChannel.getId()
                    && (client.isAway()
                    || (client.isOutputMuted() && ((client.getIdleTime() / 1000) > IDLE_TIME_UNTIL_MOVE_IN_SECONDS)))) {
                api.moveClient(client, afkChannel);
                api.sendPrivateMessage(client.getId(), "Du wurdest in die AFK Lounge verschoben, da du abwesend warst.");
                logger.info("{} is away and has been moved into AFK channel", client.getNickname());
            }
        }

        int currentlyOnline = api.getServerInfo().getClientsOnline();
        if (currentlyOnline >= maxClients) {
            List<Client> afkClients = new ArrayList<>();
            int count = 0;
            for (Client client : clients) {
                if (client.getChannelId() == afkChannel.getId()) {
                    afkClients.add(client);

                    if (++count == 2) break;
                }
            }
            for (Client afk : afkClients) {
                String message = "Der Server ist aktuell voll. Du wurdest wegen Inaktivität gekickt.";
                api.kickClientFromServer(message, afk);
                api.sendPrivateMessage(afk.getId(), message);
            }
        }
    }
}
