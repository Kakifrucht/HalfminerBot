package de.halfminer.hmbot.storage;

import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import de.halfminer.hmbot.HalfminerBotClass;

import java.util.HashMap;
import java.util.Map;

public class BotStorage extends HalfminerBotClass {

    private final Map<Integer, Integer> channelOwner = new HashMap<>();

    public Map<Integer, Integer> getMapChannelOwner() {
        return channelOwner;
    }

    /**
     * Move user to his channel if he has one already (on join or when joining the bots channel).
     *
     * @param clientID ID of the client
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
