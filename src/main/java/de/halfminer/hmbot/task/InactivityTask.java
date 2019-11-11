package de.halfminer.hmbot.task;

import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import de.halfminer.hmbot.storage.HalfClient;
import de.halfminer.hmbot.storage.Storage;
import de.halfminer.hmbot.util.MessageBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Will be run at fixed interval and move AFK users to AFK channel.
 */
class InactivityTask extends Task {

    private final Storage storage = componentHolder.getStorage();

    private final int maxClients;
    private Channel afkChannel;

    InactivityTask() {
        maxClients = getTS3Api().getServerInfo().getMaxClients();
    }

    @Override
    boolean checkIfEnabled() {
        try {
            for (Channel channel : getTS3Api().getChannelsByName(config.getString("task.inactivity.channelNameContains"))) {
                if (channel.getNeededTalkPower() > 0) {
                    afkChannel = channel;
                    return true;
                }
            }
        } catch (TS3CommandFailedException ignored) {}

        return false;
    }

    @Override
    public void execute() {

        Map<Client, HalfClient> clientMap = storage.getOnlineClients();
        for (Map.Entry<Client, HalfClient> clientEntry : clientMap.entrySet()) {

            Client client = clientEntry.getKey();
            HalfClient hClient = clientEntry.getValue();

            int idleTimeUntilMove = config.getInt("task.inactivity.idleTimeUntilMove");
            boolean isExempt = hClient.hasPermission("task.inactivity.exempt.move");
            if (client.getChannelId() != afkChannel.getId()
                    && (client.isAway()
                    || (!isExempt && (client.isOutputMuted() && ((client.getIdleTime() / 1000) > idleTimeUntilMove))))) {

                getTS3Api().moveClient(client, afkChannel);
                MessageBuilder.create("taskInactivityMoved").sendMessage(client);
                logger.info("{} is away and has been moved into AFK channel", client.getNickname());
            }
        }

        int currentlyOnline = getTS3Api().getServerInfo().getClientsOnline();
        if (currentlyOnline >= maxClients) {

            List<Client> afkClients = new ArrayList<>();
            int clientsToKick = config.getInt("task.inactivity.clientsToKickIfFull");
            int count = 0;
            for (Map.Entry<Client, HalfClient> clientEntry : clientMap.entrySet()) {
                if (clientEntry.getKey().getChannelId() == afkChannel.getId()
                        && !clientEntry.getValue().hasPermission("task.inactivity.exempt.kick")) {
                    if (count++ >= clientsToKick) break;
                    afkClients.add(clientEntry.getKey());
                }
            }

            String message = MessageBuilder.returnMessage("taskInactivityKicked");
            for (Client afk : afkClients) {
                getTS3Api().sendPrivateMessage(afk.getId(), message);
                getTS3Api().kickClientFromServer(message, afk);
            }
        }
    }
}
