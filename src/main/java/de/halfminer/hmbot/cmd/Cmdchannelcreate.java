package de.halfminer.hmbot.cmd;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.wrapper.ChannelInfo;
import de.halfminer.hmbot.cmd.abs.Command;
import de.halfminer.hmbot.exception.CommandNotCompletedException;
import de.halfminer.hmbot.exception.InvalidCommandLineException;
import de.halfminer.hmbot.util.StringArgumentSeparator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

@SuppressWarnings("unused")
public class Cmdchannelcreate extends Command {

    private final ChannelInfo botChannel;

    Cmdchannelcreate(int clientId, StringArgumentSeparator command) throws InvalidCommandLineException {
        super(clientId, command);

        if (!commandLine.meetsLength(1)) {
            throw new InvalidCommandLineException("Bitte gib ein Passwort an.", "!channelcreate <passwort>");
        }

        botChannel = api.getChannelInfo(api.whoAmI().getChannelId());
    }

    @Override
    public void run() throws CommandNotCompletedException {

        // move if channel already exists
        if (bot.getStorage().moveToChannel(invoker.getId())) {
            throw new CommandNotCompletedException(this, "User already owns a channel.");
        }

        String channelCreateName = invoker.getNickname() + "'s Channel";
        String password = commandLine.getArgument(0);

        HashMap<ChannelProperty, String> channelCreateProperty = new HashMap<>();
        channelCreateProperty.put(ChannelProperty.CHANNEL_CODEC_QUALITY, "10");
        channelCreateProperty.put(ChannelProperty.CHANNEL_PASSWORD, password);
        channelCreateProperty.put(ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT, "1");
        channelCreateProperty.put(ChannelProperty.CHANNEL_TOPIC,
                "Channel Erstelldatum: " + new SimpleDateFormat("dd.MM.yy / HH:mm").format(new Date()));
        channelCreateProperty.put(ChannelProperty.CPID, Integer.toString(botChannel.getParentChannelId()));

        int channelCreateID = api.createChannel(channelCreateName, channelCreateProperty);

        if (channelCreateID > 0) {

            bot.getStorage().getMapChannelOwner().put(invoker.getDatabaseId(), channelCreateID);
            api.moveClient(invoker.getId(), channelCreateID);
            api.setClientChannelGroup(botConfig.getChannelAdminID(), channelCreateID, invoker.getDatabaseId());
            api.addChannelPermission(channelCreateID, "i_icon_id", (int) botChannel.getIconId());

            // switch to temporary channel with delete delay, since it can't be set upon creation
            channelCreateProperty.clear();
            channelCreateProperty.put(ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT, "0");
            channelCreateProperty.put(ChannelProperty.CHANNEL_FLAG_TEMPORARY, "1");
            channelCreateProperty.put(ChannelProperty.CHANNEL_DELETE_DELAY, "180");
            api.editChannel(channelCreateID, channelCreateProperty);

            api.sendPrivateMessage(clientId,
                    "Dein Channel wurde erfolgreich erstellt, das Passwort lautet \"" + password + "\".");
            logger.info("Channel created: {}", channelCreateName);
        } else {
            throw new CommandNotCompletedException(this, "An unknown error has occurred",
                    "Ein unbekannter Fehler ist aufgetreten. Bitte versuche es erneut, oder wende dich an ein Teammitglied.");
        }
    }
}
