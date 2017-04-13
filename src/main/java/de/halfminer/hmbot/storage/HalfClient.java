package de.halfminer.hmbot.storage;

import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import de.halfminer.hmbot.BotClass;
import de.halfminer.hmbot.util.MessageBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client class managed by {@link Storage}.
 */
public class HalfClient extends BotClass {

    private int clientId;
    private HalfGroup group;

    private boolean isOnline = false;
    private final Map<Class, Long> commandCooldown = new ConcurrentHashMap<>();

    private int channelId = Integer.MIN_VALUE;

    HalfClient(int clientId, HalfGroup group) {
        updateClient(clientId, group);
    }

    int getClientId() {
        return clientId;
    }

    public ClientInfo getClientInfo() {
        return api.getClientInfo(clientId);
    }

    public boolean hasPermission(String permission) {
        return group.hasPermission(permission);
    }

    public long getCooldown(Class commandClass) {
        clearCommandCooldown();
        if (!hasPermission("cmd.bypass.cooldown") && commandCooldown.containsKey(commandClass)) {
            return commandCooldown.get(commandClass) - (System.currentTimeMillis() / 1000);
        }
        return 0;
    }

    public void addCooldown(Class commandClass, int cooldownSeconds) {
        commandCooldown.put(commandClass, (System.currentTimeMillis() / 1000) + cooldownSeconds);
    }

    boolean doSaveToDisk() {
        return getChannel() != null;
    }

    boolean isOnline() {
        return isOnline;
    }

    void updateClient(int clientId, HalfGroup group) {
        this.clientId = clientId;
        this.group = group;
        this.isOnline = true;
    }

    void setOffline() {
        isOnline = false;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    int getChannelId() {
        return channelId;
    }

    public Channel getChannel() {

        if (channelId > Integer.MIN_VALUE) {
            for (Channel channel : api.getChannels()) {
                if (channel.getId() == channelId) {
                    return channel;
                }
            }
            // if none was found, remove
            channelId = Integer.MIN_VALUE;
        }

        return null;
    }

    /**
     * Move client to his channel if he created one via {@link de.halfminer.hmbot.cmd.CmdChannel},
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
            MessageBuilder.create("hasChannel").sendMessage(clientId);
            return true;
        }

        return false;
    }

    private void clearCommandCooldown() {
        long currentTime = System.currentTimeMillis() / 1000;
        commandCooldown.values().removeIf(timeStamp -> timeStamp < currentTime);
    }

    @Override
    public String toString() {
        return "ClientID: " + clientId + " - Group: [" + group.toString() + "]";
    }
}
