package de.halfminer.hmtsbot.scheduled;

import com.github.theholywaffle.teamspeak3.TS3ApiAsync;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import de.halfminer.hmtsbot.HalfminerBot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Will be run at fixed interval and move AFK users to AFK channel
 */
public class InactivityCheck implements Runnable {

    private boolean isEnabled = false;

    private static final HalfminerBot bot = HalfminerBot.getInstance();
    private final static int IDLE_TIME_UNTIL_MOVE_IN_SECONDS = 300;
    private final TS3ApiAsync api = bot.getApiAsync();

    private Channel afkChannel;
    private final int maxClients;

    public InactivityCheck() {

        // Suppose that first channel containing the word afk is the designated one
        maxClients = bot.getApi().getServerInfo().getMaxClients();
        for (Channel channel : bot.getApi().getChannelsByName("AFK")) {
            if (channel.getNeededTalkPower() > 0) {
                afkChannel = channel;
                isEnabled = true;
                return;
            }
        }
    }

    @Override
    public void run() {

        List<Client> clients;
        try {
            clients = api.getClients().get();
        } catch (InterruptedException e) {
            HalfminerBot.getLogger().warning("Could not get Clientlist");
            return;
        }

        for (Client client : clients) {
            if (client.getChannelId() != afkChannel.getId() && (client.isAway() || (client.isOutputMuted() && ((client.getIdleTime() / 1000) > IDLE_TIME_UNTIL_MOVE_IN_SECONDS)))) {
                api.moveClient(client, afkChannel);
                api.sendPrivateMessage(client.getId(), "Du wurdest in die AFK Lounge verschoben, da du abwesend warst.");
                HalfminerBot.getLogger().info(client.getNickname() + " is away and has been moved into AFK channel");
            }
        }

        int currentlyOnline;
        try {
            currentlyOnline = api.getServerInfo().get().getClientsOnline();
        } catch (InterruptedException e) {
            return;
        }

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

    public boolean isEnabled() {
        return isEnabled;
    }
}
