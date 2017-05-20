package de.halfminer.hmbot.cmd;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.ChannelInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import de.halfminer.hmbot.storage.HalfClient;
import de.halfminer.hmbot.util.MessageBuilder;
import de.halfminer.hmbot.util.StringArgumentSeparator;

import java.text.SimpleDateFormat;
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
class CmdChannel extends Command {

    private final ChannelInfo botChannel;
    private String channelDeleteDelay;
    private int channelGroupAdminId;

    public CmdChannel(HalfClient client, ClientInfo clientInfo, StringArgumentSeparator command) throws InvalidCommandException {
        super(client, clientInfo, command);

        if (!this.command.meetsLength(2)) {
            throw new InvalidCommandException(CommandEnum.CHANNEL);
        }

        botChannel = api.getChannelInfo(api.whoAmI().getChannelId());
    }

    @Override
    void run() throws InvalidCommandException {

        boolean isDonator = client.hasPermission("cmd.channel.donator");
        channelDeleteDelay = config.getString("command.channel.channelDeleteDelay" + (isDonator ? "Donator" : ""));
        channelGroupAdminId = config.getInt("command.channel.channelGroupAdminID" + (isDonator ? "Donator" : ""));
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
        if (client.moveToChannel(clientId)) {
            return;
        }

        String channelCreateName = getChannelName();
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

        int channelCreateID;
        try {
            channelCreateID = api.createChannel(channelCreateName, channelCreateProperty);
            client.setChannelId(channelCreateID);

            // switch to temporary channel with delete delay, since it can't be set upon creation
            channelCreateProperty.clear();
            channelCreateProperty.put(ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT, "0");
            channelCreateProperty.put(ChannelProperty.CHANNEL_FLAG_TEMPORARY, "1");
            channelCreateProperty.put(ChannelProperty.CHANNEL_DELETE_DELAY, channelDeleteDelay);
            api.editChannel(channelCreateID, channelCreateProperty);

            api.moveClient(clientInfo.getId(), channelCreateID);

            sendMessage("cmdChannelCreateSuccess", "PASSWORD", password);
            logger.info("Channel created: {}", channelCreateName);

        } catch (TS3CommandFailedException e) {
            sendMessage("cmdChannelCreateError");
            return;
        }

        api.setClientChannelGroup(channelGroupAdminId, channelCreateID, clientInfo.getDatabaseId());
        api.addChannelPermission(channelCreateID, "i_icon_id", (int) botChannel.getIconId());
    }

    private void updateChannel() {
        Channel channel = client.getChannel();
        if (channel == null) {
            sendMessage("cmdChannelUpdateError");
            return;
        }

        Map<ChannelProperty, String> editMap = new HashMap<>();
        String newChannelName = getChannelName();
        String password = getPassword();

        editMap.put(ChannelProperty.CHANNEL_PASSWORD, password);
        if (!newChannelName.equals(channel.getName())) {
            editMap.put(ChannelProperty.CHANNEL_NAME, newChannelName);
        }

        api.editChannel(channel.getId(), editMap);

        // kick if not admin before changing password
        for (Map.Entry<Client, HalfClient> clientEntry : storage.getOnlineClients().entrySet()) {
            Client client = clientEntry.getKey();
            HalfClient hClient = clientEntry.getValue();
            if (client.getChannelId() == channel.getId()
                    && client.getChannelGroupId() != channelGroupAdminId
                    && !hClient.hasPermission("cmd.channel.update.exempt.kick")) {
                api.kickClientFromChannel(client.getId());
                MessageBuilder.create("cmdChannelUpdateKicked")
                        .addPlaceholderReplace("NICKNAME", clientInfo.getNickname())
                        .sendMessage(client);
            }
        }

        sendMessage("cmdChannelUpdateSuccess", "PASSWORD", password);
        addCooldown(300);
    }

    private String getChannelName() {
        String channelName = MessageBuilder.create("cmdChannelCreateFormat")
                .addPlaceholderReplace("NICKNAME", clientInfo.getNickname())
                .returnMessage();

        if (channelName.length() > 40) {
            channelName = channelName.substring(0, 40);
        }

        return channelName;
    }

    private String getPassword() {
        String password = command.getArgument(1);
        if (password.length() > 20) {
            password = password.substring(0, 20);
        }
        return password;
    }
}
