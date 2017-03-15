package de.halfminer.hmbot.actions;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import de.halfminer.hmbot.HalfminerBot;
import de.halfminer.hmbot.exception.ActionNotCompletedException;
import de.halfminer.hmbot.exception.InvalidCommandLineException;
import de.halfminer.hmbot.util.CommandLine;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class ActionChannelCreate extends Action {

    private final Channel botChannel;

    public ActionChannelCreate(CommandLine command) throws InvalidCommandLineException {
        super(command);

        botChannel = api.getChannelsByName(config.getChannelMoveName()).get(0);

        if (command.getCommandLine().equals(""))
            throw new InvalidCommandLineException("Bitte gib ein Passwort an.", "!channelcreate <passwort>");
    }

    @Override
    public void run() throws ActionNotCompletedException {

        // move if channel already exists
        if (bot.getStorage().moveToChannel(invoker.getId()))
            throw new ActionNotCompletedException(this, "User already owns a channel.");

        int waitingTime = waitingTime(invoker.getDatabaseId()); // rate limit check
        if (waitingTime >= 0) {
            throw new ActionNotCompletedException(this, "User hit the limit and has to wait " + waitingTime + " seconds.",
                    "Du hast vor Kurzem erst einen Channel erstellt, bitte warte noch " + waitingTime + " Sekunden.");
        }

        bot.getStorage().getMapFloodProtection().put(invoker.getDatabaseId(), System.currentTimeMillis() / 1000L);

        String channelCreateName = invoker.getNickname() + "'s Channel";

        HashMap<ChannelProperty, String> channelCreateProperty = new HashMap<>();
        channelCreateProperty.put(ChannelProperty.CHANNEL_CODEC_QUALITY, "10");
        channelCreateProperty.put(ChannelProperty.CHANNEL_PASSWORD, this.command.getCommandLine());
        channelCreateProperty.put(ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT, "1");
        channelCreateProperty.put(ChannelProperty.CHANNEL_TOPIC,
                "Channel Erstelldatum: " + new SimpleDateFormat("dd.MM.yy / HH:mm").format(new Date()));
        channelCreateProperty.put(ChannelProperty.CPID, Integer.toString(botChannel.getParentChannelId()));

        int channelCreateID = api.createChannel(channelCreateName, channelCreateProperty);

        if (channelCreateID > 0) {

            bot.getStorage().getMapChannelOwner().put(invoker.getDatabaseId(), channelCreateID);
            api.moveClient(command.getClientId(), channelCreateID);
            api.setClientChannelGroup(config.getChannelAdminID(), channelCreateID, invoker.getDatabaseId());
            api.addChannelPermission(channelCreateID, "i_icon_id", (int) botChannel.getIconId());

            // switch to temporary channel with delete delay, since it can't be set upon creation
            channelCreateProperty.clear();
            channelCreateProperty.put(ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT, "0");
            channelCreateProperty.put(ChannelProperty.CHANNEL_FLAG_TEMPORARY, "1");
            channelCreateProperty.put(ChannelProperty.CHANNEL_DELETE_DELAY, "180");
            api.editChannel(channelCreateID, channelCreateProperty);

            api.sendPrivateMessage(command.getClientId(),
                    "Dein Channel wurde erfolgreich erstellt, das Passwort lautet "
                            + '"' + this.command.getCommandLine() + '"' + '.');
            HalfminerBot.getLogger().info("Channel created: " + channelCreateName);

        } else {

            // allow user to create another channel
            bot.getStorage().getMapFloodProtection().remove(invoker.getDatabaseId());
            throw new ActionNotCompletedException(this, "An unknown error has occurred",
                    "Ein unbekannter Fehler ist aufgetreten. Bitte versuche es erneut, oder wende dich an ein Teammitglied.");

        }
    }

    /**
     * Time the given user has to wait until he can create another channel
     *
     * @param userDBID - Users ID in the Database
     * @return Time in seconds the user has to wait, or -1 if no wait is necessary
     */
    private int waitingTime(int userDBID) {

        HashMap<Integer, Long> floodProtection = bot.getStorage().getMapFloodProtection();
        int waitingParamInSeconds = bot.getConfig().getWaitingParamTimeInSeconds();

        if (floodProtection.containsKey(userDBID)) {
            long lastusedTime = floodProtection.get(userDBID);
            long currentTime = System.currentTimeMillis() / 1000L;
            if (lastusedTime < currentTime + waitingParamInSeconds) {
                return (int) (waitingParamInSeconds - (currentTime - lastusedTime));
            } else {
                floodProtection.remove(userDBID);
                return -1;
            }
        } else return -1;
    }
}
