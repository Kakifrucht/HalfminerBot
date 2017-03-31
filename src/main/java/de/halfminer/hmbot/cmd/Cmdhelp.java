package de.halfminer.hmbot.cmd;

import com.github.theholywaffle.teamspeak3.api.wrapper.Permission;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerGroup;
import de.halfminer.hmbot.HalfminerBot;
import de.halfminer.hmbot.util.StringArgumentSeparator;

/**
 * - Sends list containing every available command
 * - When calling !help < querypassword> highest available group will be granted (can be disabled)
 */
@SuppressWarnings("unused")
public class Cmdhelp extends Command {

    public Cmdhelp(int clientId, StringArgumentSeparator command) throws InvalidCommandException {
        super(clientId, command);
    }

    @Override
    void run() {

        // hidden command !help <password> sets sender
        if (command.meetsLength(1)
                && config.getBoolean("command.help.enableGroupGrant")
                && command.getArgument(0).equals(config.getString("credentials.password"))) {

            ServerGroup highestGroup = null;
            int highestTalkPower = 0;
            for (ServerGroup serverGroup : api.getServerGroups()) {
                for (Permission perm : api.getServerGroupPermissions(serverGroup.getId())) {
                    if (perm.getName().equals("i_client_talk_power")
                            && perm.getValue() >= highestTalkPower) {
                        highestGroup = serverGroup;
                        highestTalkPower = perm.getValue();
                        break;
                    }
                }
            }

            if (highestGroup != null) {
                boolean hasGroup = false;
                for (ServerGroup group : api.getServerGroupsByClient(clientInfo)) {
                    if (group.getId() == highestGroup.getId()) {
                        hasGroup = true;
                        break;
                    }
                }

                if (!hasGroup) {
                    api.addClientToServerGroup(highestGroup.getId(), clientInfo.getDatabaseId());
                    sendMessage("cmdHelpSetGroup", "GROUP", highestGroup.getName());
                    return;
                }
            }
        }

        api.sendPrivateMessage(clientId,
                "[B]HalfminerBot[/B] v" + HalfminerBot.getVersion() + " - © halfminer.de | Kakifrucht");

        //TODO get command list programmatically
        api.sendPrivateMessage(clientId,
                "Verfügbare Kommandos: \n!channel <create|update> <passwort> -> erstelle einen eigenen Channel");
    }
}
