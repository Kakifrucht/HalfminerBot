package de.halfminer.hmbot.cmd;

import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.Permission;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerGroup;
import de.halfminer.hmbot.storage.HalfClient;
import de.halfminer.hmbot.util.MessageBuilder;
import de.halfminer.hmbot.util.StringArgumentSeparator;

/**
 * - Sends list containing every available command per client
 *   - Checks if client has permission
 *   - Sends correct usage and description
 * - When calling !help < querypassword> highest available group will be granted (can be disabled)
 */
class CmdHelp extends Command {

    public CmdHelp(HalfClient client, ClientInfo clientInfo, StringArgumentSeparator command) {
        super(client, clientInfo, command);
    }

    @Override
    void run() {

        // hidden command !help <password> sets sender to highest server group
        if (command.meetsLength(1)
                && config.getBoolean("command.help.enableGroupGrant")
                && config.getPassword().equals(command.getArgument(0))) {

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

        String version = componentHolder.getStateHolder().getVersion();
        MessageBuilder.create("[B]HalfminerBot[/B] v" + version + " - © Kakifrucht@halfminer.de | [url=https://github.com/Kakifrucht/HalfminerBot]GitHub[/url]")
                .setDirectString()
                .sendMessage(clientId);

        StringBuilder allCommands = new StringBuilder(MessageBuilder.returnMessage("cmdHelpCommandsTitle"));
        for (CommandEnum cmd : CommandEnum.values()) {

            if (cmd.equals(CommandEnum.HELP))
                continue;

            if (client.hasPermission(cmd.getPermission())) {

                String description = MessageBuilder.returnMessage(cmd.getDescriptionKey());
                String usage = MessageBuilder.returnMessage(cmd.getUsageKey());

                String toAppend = MessageBuilder.create("cmdHelpFormat")
                        .addPlaceholderReplace("DESCRIPTION", description)
                        .addPlaceholderReplace("USAGE", usage)
                        .returnMessage();

                allCommands.append(toAppend).append("\n");
            }
        }

        MessageBuilder.create(allCommands.toString()).setDirectString().sendMessage(clientId);
    }
}
