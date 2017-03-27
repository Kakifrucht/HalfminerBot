package de.halfminer.hmbot.cmd;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.ChannelInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import de.halfminer.hmbot.storage.HalfClient;
import de.halfminer.hmbot.util.StringArgumentSeparator;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class Cmdchannel extends Command {

    private final ChannelInfo botChannel;
    private HalfClient client;

    private int channelGroupAdminId;

    public Cmdchannel(int clientId, StringArgumentSeparator command) throws InvalidCommandLineException {
        super(clientId, command);

        if (!commandLine.meetsLength(2)) {
            throw new InvalidCommandLineException("!channel <create|update> <password>");
        }

        botChannel = api.getChannelInfo(api.whoAmI().getChannelId());
    }

    @Override
    void run() throws CommandNotCompletedException {

        client = storage.getClient(clientId);

        channelGroupAdminId = config.getInt("command.channel.channelGroupAdminID");
        switch (commandLine.getArgument(0).toLowerCase()) {
            case "create":
                createChannel();
                break;
            case "update":
                updateChannel();
                break;
            default:
                sendMessage("Verwendung: !channel <create|update> <password>");
        }
    }

    private void updateChannel() {
        Channel channel = client.getChannel();
        if (channel == null) {
            sendMessage("Du hast keinen eigenen Channel, erstelle einen indem du dein gewünschtes Passwort eingibst.");
            return;
        }

        String password = commandLine.getArgument(1);
        api.editChannel(channel.getId(), Collections.singletonMap(ChannelProperty.CHANNEL_PASSWORD, password));

        for (Client client : api.getClients()) {
            // kick if not admin before changing password
            if (client.getChannelId() == channel.getId() && client.getChannelGroupId() != channelGroupAdminId) {
                api.kickClientFromChannel(client.getId());
            }
        }

        sendMessage("Der Channel wurde geleert und das Passwort zu \"" + password + "\" geändert.");
    }

    private void createChannel() throws CommandNotCompletedException {

        // move if user has channel already
        if (client.moveToChannel()) {
            return;
        }

        String channelCreateName = invoker.getNickname() + "'s Channel";
        String password = commandLine.getArgument(1);

        Map<ChannelProperty, String> channelCreateProperty = new HashMap<>();
        channelCreateProperty.put(ChannelProperty.CHANNEL_CODEC_QUALITY, "10");
        channelCreateProperty.put(ChannelProperty.CHANNEL_PASSWORD, password);
        channelCreateProperty.put(ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT, "1");
        channelCreateProperty.put(ChannelProperty.CHANNEL_TOPIC,
                "Channel Erstelldatum: " + new SimpleDateFormat("dd.MM / HH:mm").format(new Date()));
        channelCreateProperty.put(ChannelProperty.CPID, Integer.toString(botChannel.getParentChannelId()));

        int channelCreateID = api.createChannel(channelCreateName, channelCreateProperty);

        if (channelCreateID > 0) {

            client.setChannelId(channelCreateID);
            api.moveClient(invoker.getId(), channelCreateID);
            api.setClientChannelGroup(channelGroupAdminId, channelCreateID, invoker.getDatabaseId());
            api.addChannelPermission(channelCreateID, "i_icon_id", (int) botChannel.getIconId());

            // switch to temporary channel with delete delay, since it can't be set upon creation
            channelCreateProperty.clear();
            channelCreateProperty.put(ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT, "0");
            channelCreateProperty.put(ChannelProperty.CHANNEL_FLAG_TEMPORARY, "1");
            channelCreateProperty.put(ChannelProperty.CHANNEL_DELETE_DELAY,
                    config.getString("command.channel.channelDeleteDelay"));
            api.editChannel(channelCreateID, channelCreateProperty);

            sendMessage("Dein Channel wurde erfolgreich erstellt, das Passwort lautet \"" + password + "\". " +
                    "Du kannst das Passwort mit dem Kommando \"!channel update <passwort>\" ändern (kickt alle Spieler).");
            logger.info("Channel created: {}", channelCreateName);
        } else {
            throw new CommandNotCompletedException(this, "Channel already exists",
                    "Der Channel konnte nicht erstellt werden, da es bereits einen Channel mit deinem Namen gibt.");
        }
    }
}
