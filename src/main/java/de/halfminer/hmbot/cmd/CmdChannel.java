package de.halfminer.hmbot.cmd;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.ChannelInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import de.halfminer.hmbot.storage.HalfClient;
import de.halfminer.hmbot.util.MessageBuilder;
import de.halfminer.hmbot.util.StringArgumentSeparator;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * - Create channels for users
 *   - Gives channel admin to the creating user
 *     - Set group ID via config
 *   - Adds username to the channelname
 *   - Sets the channel as temporary
 *     - Stays persistent for set amount of seconds (config)
 *     - Detect if user already has a channel, automatically move user to his channel on join/chat/move
 * - Update a users channel password
 *   - Will kick all players from channel after changing password
 */
@SuppressWarnings("unused")
public class CmdChannel extends Command {

    private final ChannelInfo botChannel;
    private int channelGroupAdminId;

    public CmdChannel(HalfClient client, StringArgumentSeparator command) throws InvalidCommandException {
        super(client, command);

        if (!this.command.meetsLength(2)) {
            throw new InvalidCommandException(CommandEnum.CHANNEL);
        }

        botChannel = api.getChannelInfo(api.whoAmI().getChannelId());
    }

    @Override
    void run() throws InvalidCommandException {

        channelGroupAdminId = config.getInt("command.channel.channelGroupAdminID");
        switch (command.getArgument(0).toLowerCase()) {
            case "create":
                createChannel();
                break;
            case "update":
                updateChannel();
                break;
            default:
                throw new InvalidCommandException(CommandEnum.CHANNEL);
        }
    }

    private void createChannel() {

        // move if user has channel already
        if (client.moveToChannel()) {
            return;
        }

        String channelCreateName = MessageBuilder.create("cmdChannelCreateFormat")
                .addPlaceholderReplace("NICKNAME", clientInfo.getNickname())
                .returnMessage();

        if (channelCreateName.length() > 40) {
            channelCreateName = channelCreateName.substring(0, 40);
        }

        String password = getPassword();

        Map<ChannelProperty, String> channelCreateProperty = new HashMap<>();
        channelCreateProperty.put(ChannelProperty.CPID, Integer.toString(botChannel.getParentChannelId()));
        channelCreateProperty.put(ChannelProperty.CHANNEL_CODEC_QUALITY, "10");
        channelCreateProperty.put(ChannelProperty.CHANNEL_PASSWORD, password);
        channelCreateProperty.put(ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT, "1");

        String dateFormat = new SimpleDateFormat(
                MessageBuilder.returnMessage("cmdChannelCreateTopicFormat")).format(new Date());
        String channelTopic = MessageBuilder.create("cmdChannelCreateTopic")
                .addPlaceholderReplace("FORMAT", dateFormat)
                .returnMessage();

        channelCreateProperty.put(ChannelProperty.CHANNEL_TOPIC, channelTopic);

        int channelCreateID = api.createChannel(channelCreateName, channelCreateProperty);

        if (channelCreateID > 0) {

            client.setChannelId(channelCreateID);
            api.moveClient(clientInfo.getId(), channelCreateID);
            api.setClientChannelGroup(channelGroupAdminId, channelCreateID, clientInfo.getDatabaseId());
            api.addChannelPermission(channelCreateID, "i_icon_id", (int) botChannel.getIconId());

            // switch to temporary channel with delete delay, since it can't be set upon creation
            channelCreateProperty.clear();
            channelCreateProperty.put(ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT, "0");
            channelCreateProperty.put(ChannelProperty.CHANNEL_FLAG_TEMPORARY, "1");
            channelCreateProperty.put(ChannelProperty.CHANNEL_DELETE_DELAY,
                    config.getString("command.channel.channelDeleteDelay"));
            api.editChannel(channelCreateID, channelCreateProperty);

            sendMessage("cmdChannelCreateSuccess", "PASSWORD", password);
            logger.info("Channel created: {}", channelCreateName);
        } else {
            sendMessage("cmdChannelCreateError");
        }
    }

    private void updateChannel() {
        Channel channel = client.getChannel();
        if (channel == null) {
            sendMessage("cmdChannelUpdateError");
            return;
        }

        String password = getPassword();
        api.editChannel(channel.getId(), Collections.singletonMap(ChannelProperty.CHANNEL_PASSWORD, password));

        for (Client client : api.getClients()) {
            // kick if not admin before changing password
            if (client.getChannelId() == channel.getId()
                    && client.getChannelGroupId() != channelGroupAdminId
                    && !storage.getClient(client).hasPermission("cmd.channel.update.exempt.kick")) {
                api.kickClientFromChannel(client.getId());
            }
        }

        sendMessage("cmdChannelUpdateSuccess", "PASSWORD", password);
    }

    private String getPassword() {
        String password = command.getArgument(1);
        if (password.length() > 20) {
            password = password.substring(0, 20);
        }
        return password;
    }
}
