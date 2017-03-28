package de.halfminer.hmbot.storage;

import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import de.halfminer.hmbot.HalfminerBotClass;
import de.halfminer.hmbot.cmd.Cmdchannel;

import java.util.List;

/**
 * Client class managed by {@link Storage}.
 */
public class HalfClient extends HalfminerBotClass {

    private int clientId;
    private HalfGroup group;

    private int channelId = Integer.MIN_VALUE;

    HalfClient(int clientId, HalfGroup group) {
        this.clientId = clientId;
        this.group = group;
    }

    public boolean hasPermission(String permission) {
        return group.hasPermission(permission);
    }

    boolean canBeEvicted(List<Client> clients) {
        boolean isOnline = clients.stream()
                .map(Client::getId)
                .anyMatch(id -> id == clientId);
        return !isOnline && getChannel() == null;
    }

    void updateClient(int clientId, HalfGroup group) {
        this.clientId = clientId;
        this.group = group;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public Channel getChannel() {
        for (Channel channel : api.getChannels()) {
            if (channel.getId() == channelId) {
                return channel;
            }
        }
        return null;
    }

    /**
     * Move client to his channel if he created one via {@link Cmdchannel},
     * used on join, when joining the bots channel or when trying to create a channel if player already has one.
     *
     * @return true if user was moved to own channel, false if he doesn't have a channel
     */
    public boolean moveToChannel() {

        Channel channelOfUser = getChannel();
        if (channelOfUser != null) {
            ClientInfo user = api.getClientInfo(clientId);
            if (user.getChannelId() != channelOfUser.getId()) { //check if user is already in his channel, if not move
                api.moveClient(clientId, channelOfUser.getId());
            }
            api.sendPrivateMessage(clientId, "Du hast bereits einen privaten Channel.");
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return "ClientID: " + clientId + " - Group: [" + group.toString() + "]";
    }
}
