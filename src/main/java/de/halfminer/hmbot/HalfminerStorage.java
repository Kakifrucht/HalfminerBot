package de.halfminer.hmbot;

import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerQueryInfo;

import java.util.HashMap;

public class HalfminerStorage extends HalfminerBotClass {

    private final HashMap<Integer, Integer> channelOwner = new HashMap<>();
    private final HashMap<Integer, Long> floodProtection = new HashMap<>();
    private ServerQueryInfo botInformation;

    public HashMap<Integer, Integer> getMapChannelOwner() {
        return channelOwner;
    }

    public HashMap<Integer, Long> getMapFloodProtection() {
        return floodProtection;
    }

    public ServerQueryInfo getBotInformation() {
        if (botInformation == null) botInformation = api.whoAmI();
        return botInformation;
    }

    /**
     * Move user to his channel if he has one already (on join or when joining the bots channel)
     *
     * @param clientID - ID of the client
     */
    public boolean moveToChannel(int clientID) {

        ClientInfo user = api.getClientInfo(clientID);

        Channel channelOfUser = null;
        if (this.channelOwner.containsKey(user.getDatabaseId())) {

            int channelID = channelOwner.get(user.getDatabaseId());
            for (Channel channel : api.getChannels()) {
                if (channel.getId() == channelID) channelOfUser = channel;
            }

        }

        if (channelOfUser == null) channelOwner.remove(user.getDatabaseId());
        else {
            if (user.getChannelId() != channelOfUser.getId()) { //check if user is already in his channel, if not move
                api.moveClient(clientID, channelOfUser.getId());
            }
            api.sendPrivateMessage(clientID, "Du hast bereits einen privaten Channel.");
            return true;
        }

        return false;
    }
}
