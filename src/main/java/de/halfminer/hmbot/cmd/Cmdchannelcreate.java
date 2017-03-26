package de.halfminer.hmbot.cmd;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.wrapper.ChannelInfo;
import de.halfminer.hmbot.storage.HalfClient;
import de.halfminer.hmbot.util.StringArgumentSeparator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

@SuppressWarnings("unused")
public class Cmdchannelcreate extends Command {

    private final ChannelInfo botChannel;

    public Cmdchannelcreate(int clientId, StringArgumentSeparator command) throws InvalidCommandLineException {
        super(clientId, command);

        if (!commandLine.meetsLength(1)) {
            throw new InvalidCommandLineException("Bitte gib ein Passwort an.", "!channelcreate <passwort>");
        }

        botChannel = api.getChannelInfo(api.whoAmI().getChannelId());
    }

    @Override
    void run() throws CommandNotCompletedException {

        // move if user has channel already
        HalfClient halfClient = bot.getStorage().getClient(clientId);
        if (halfClient.moveToChannel()) {
            return;
        }

        String channelCreateName = invoker.getNickname() + "'s Channel";
        String password = commandLine.getArgument(0);

        HashMap<ChannelProperty, String> channelCreateProperty = new HashMap<>();
        channelCreateProperty.put(ChannelProperty.CHANNEL_CODEC_QUALITY, "10");
        channelCreateProperty.put(ChannelProperty.CHANNEL_PASSWORD, password);
        channelCreateProperty.put(ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT, "1");
        channelCreateProperty.put(ChannelProperty.CHANNEL_TOPIC,
                "Channel Erstelldatum: " + new SimpleDateFormat("dd.MM / HH:mm").format(new Date()));
        channelCreateProperty.put(ChannelProperty.CPID, Integer.toString(botChannel.getParentChannelId()));

        int channelCreateID = api.createChannel(channelCreateName, channelCreateProperty);

        if (channelCreateID > 0) {

            halfClient.setChannelId(channelCreateID);
            api.moveClient(invoker.getId(), channelCreateID);
            api.setClientChannelGroup(
                    config.getInt("command.channelcreate.channelGroupAdminID"),
                    channelCreateID,
                    invoker.getDatabaseId()
            );
            api.addChannelPermission(channelCreateID, "i_icon_id", (int) botChannel.getIconId());

            // switch to temporary channel with delete delay, since it can't be set upon creation
            channelCreateProperty.clear();
            channelCreateProperty.put(ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT, "0");
            channelCreateProperty.put(ChannelProperty.CHANNEL_FLAG_TEMPORARY, "1");
            channelCreateProperty.put(ChannelProperty.CHANNEL_DELETE_DELAY,
                    config.getString("command.channelcreate.channelDeleteDelay"));
            api.editChannel(channelCreateID, channelCreateProperty);

            sendMessage("Dein Channel wurde erfolgreich erstellt, das Passwort lautet \"" + password + "\".");
            logger.info("Channel created: {}", channelCreateName);
        } else {
            throw new CommandNotCompletedException(this, "Channel already exists",
                    "Der Channel konnte nicht erstellt werden, da es bereits einen Channel mit deinem Namen gibt.");
        }
    }
}
