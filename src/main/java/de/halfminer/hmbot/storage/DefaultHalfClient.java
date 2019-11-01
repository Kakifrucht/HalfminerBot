package de.halfminer.hmbot.storage;

import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import de.halfminer.hmbot.BotClass;
import de.halfminer.hmbot.util.MessageBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client class managed by an {@link Storage} instance.
 */
class DefaultHalfClient extends BotClass implements HalfClient {

    private final int databaseId;
    private HalfGroup group;

    /**
     * Counts how many clients with this clients database/unique ID are online, as there can be multiple connections.
     */
    private int onlineCount = 0;
    private final Map<Class, Long> commandCooldown = new ConcurrentHashMap<>();

    private int channelId = Integer.MIN_VALUE;

    DefaultHalfClient(int databaseId, HalfGroup group) {
        this.databaseId = databaseId;

        // client is currently online
        if (group != null) {
            this.group = group;
            onlineCount++;
        }
    }

    @Override
    public boolean hasPermission(String permission) {
        return group.hasPermission(permission);
    }

    @Override
    public long getCooldown(Class commandClass) {
        clearCommandCooldown();
        if (!hasPermission("cmd.bypass.cooldown") && commandCooldown.containsKey(commandClass)) {
            return commandCooldown.get(commandClass) - (System.currentTimeMillis() / 1000);
        }
        return 0;
    }

    @Override
    public void addCooldown(Class commandClass, int cooldownSeconds) {
        commandCooldown.put(commandClass, (System.currentTimeMillis() / 1000) + cooldownSeconds);
    }

    @Override
    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    @Override
    public Channel getChannel() {

        if (channelId > Integer.MIN_VALUE) {
            for (Channel channel : getTS3Api().getChannels()) {
                if (channel.getId() == channelId) {
                    return channel;
                }
            }
            // if none was found, remove
            channelId = Integer.MIN_VALUE;
        }

        return null;
    }

    @Override
    public boolean moveToChannel(int clientId) {

        Channel channelOfUser = getChannel();
        if (channelOfUser != null) {
            ClientInfo user = getTS3Api().getClientInfo(clientId);
            // check if user is already in his channel, if not move
            if (user.getChannelId() != channelOfUser.getId()) {
                getTS3Api().moveClient(clientId, channelOfUser.getId());
            }
            MessageBuilder.create("movedToChannel").sendMessage(clientId);
            return true;
        }

        return false;
    }

    @Override
    public int getChannelId() {
        return channelId;
    }

    @Override
    public boolean doSaveToDisk() {
        return getChannel() != null;
    }

    @Override
    public boolean doDispose() {
        return onlineCount == 0 && !doSaveToDisk();
    }

    @Override
    public void clientJoined(HalfGroup group) {
        this.group = group;
        this.onlineCount++;
    }

    @Override
    public void clientLeft() {
        onlineCount--;
    }

    private void clearCommandCooldown() {
        long currentTime = System.currentTimeMillis() / 1000;
        commandCooldown.values().removeIf(timeStamp -> timeStamp < currentTime);
    }

    @Override
    public String toString() {
        return "DatabaseID: " + databaseId + " - Group: [" + (group != null ? group.toString() : "unknown") + "]";
    }
}
