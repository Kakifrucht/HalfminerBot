package de.halfminer.hmbot.task;

import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import de.halfminer.hmbot.storage.Storage;
import de.halfminer.hmbot.util.MessageBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Will be run at fixed interval and move AFK users to AFK channel.
 */
class InactivityTask extends Task {

    private final Storage storage = bot.getStorage();

    private final int maxClients;
    private Channel afkChannel;

    InactivityTask() {
        maxClients = api.getServerInfo().getMaxClients();
    }

    @Override
    boolean checkIfEnabled() {
        // suppose that first channel containing the word afk is the designated one
        for (Channel channel : api.getChannelsByName(config.getString("task.inactivity.channelNameContains"))) {
            if (channel.getNeededTalkPower() > 0) {
                afkChannel = channel;
                return true;
            }
        }

        return false;
    }

    @Override
    public void execute() {

        if (api.getChannelInfo(afkChannel.getId()) == null) {
            setTaskDisabled();
            return;
        }

        List<Client> clients = api.getClients();

        for (Client client : clients) {

            if (!client.isRegularClient()) continue;

            int idleTimeUntilMove = config.getInt("task.inactivity.idleTimeUntilMove");
            boolean isExempt = storage.getClient(client).hasPermission("task.inactivity.exempt.move");
            if (client.getChannelId() != afkChannel.getId()
                    && (client.isAway()
                    || (!isExempt && (client.isOutputMuted() && ((client.getIdleTime() / 1000) > idleTimeUntilMove))))) {

                api.moveClient(client, afkChannel);
                MessageBuilder.create("taskInactivityMoved").sendMessage(client);
                logger.info("{} is away and has been moved into AFK channel", client.getNickname());
            }
        }

        int currentlyOnline = api.getServerInfo().getClientsOnline();
        if (currentlyOnline >= maxClients) {

            List<Client> afkClients = new ArrayList<>();
            int clientsToKick = config.getInt("task.inactivity.clientsToKickIfFull");
            int count = 0;
            for (Client client : clients) {
                if (client.isRegularClient()
                        && client.getChannelId() == afkChannel.getId()
                        && !storage.getClient(client).hasPermission("task.inactivity.exempt.kick")) {
                    if (count++ >= clientsToKick) break;
                    afkClients.add(client);
                }
            }

            for (Client afk : afkClients) {
                String message = MessageBuilder.returnMessage("taskInactivityKicked");
                api.kickClientFromServer(message, afk);
                api.sendPrivateMessage(afk.getId(), message);
            }
        }
    }
}
