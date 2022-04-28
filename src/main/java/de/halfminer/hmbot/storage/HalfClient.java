package de.halfminer.hmbot.storage;

import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;

/**
 * Access bot related client information.
 */
public interface HalfClient {

    boolean hasPermission(String permission);

    long getCooldown(Class<?> commandClass);

    void addCooldown(Class<?> commandClass, int cooldownSeconds);

    void setChannelId(int channelId);

    Channel getChannel();

    /**
     * Move client to his channel, to be used on join, when joining the bots channel or
     * when trying to create a channel new channel.
     *
     * @param clientId id of exact client (one {@link HalfClient} can map to multiple clients online)
     * @return true if user was moved to own channel, false if he doesn't have a channel
     */
    boolean moveToChannel(int clientId);

    int getChannelId();

    boolean doSaveToDisk();

    boolean doDispose();

    void clientJoined(HalfGroup group);

    void clientLeft();
}
