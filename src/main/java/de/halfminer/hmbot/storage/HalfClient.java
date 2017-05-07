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

    private final int databaseId;
    private HalfGroup group;

    /**
     * Counts how many clients with this clients database/unique ID are online, as there can be multiple connections.
     */
    private int onlineCount = 0;
    private final Map<Class, Long> commandCooldown = new ConcurrentHashMap<>();

    private int channelId = Integer.MIN_VALUE;

    HalfClient(int databaseId, HalfGroup group) {
        this.databaseId = databaseId;

        // client is currently online
        if (group != null) {
            this.group = group;
            onlineCount++;
        }
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

    public void setChannelId(int channelId) {
        this.channelId = channelId;
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
     * used on join, when joining the bots channel or when trying to create a channel new channel.
     *
     * @param clientId id of exact client (one {@link HalfClient} can map to multiple clients online)
     * @return true if user was moved to own channel, false if he doesn't have a channel
     */
    public boolean moveToChannel(int clientId) {

        Channel channelOfUser = getChannel();
        if (channelOfUser != null) {
            ClientInfo user = api.getClientInfo(clientId);
            // check if user is already in his channel, if not move
            if (user.getChannelId() != channelOfUser.getId()) {
                api.moveClient(clientId, channelOfUser.getId());
            }
            MessageBuilder.create("movedToChannel").sendMessage(clientId);
            return true;
        }

        return false;
    }

    int getChannelId() {
        return channelId;
    }

    boolean doSaveToDisk() {
        return getChannel() != null;
    }

    boolean doDispose() {
        return onlineCount == 0 && !doSaveToDisk();
    }

    void clientJoined(HalfGroup group) {
        this.group = group;
        this.onlineCount++;
    }

    void clientLeft() {
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
