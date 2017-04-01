package de.halfminer.hmbot.cmd;

import de.halfminer.hmbot.storage.HalfClient;
import de.halfminer.hmbot.util.MessageBuilder;
import de.halfminer.hmbot.util.StringArgumentSeparator;

/**
 * - Broadcast a given message to all client
 * - Optional talk power requirement can be passed as flag
 *   - Example: "!broadcast -200 hi this will be broadcast" will broadcast to everybody with at least 200 talk power
 * - Broadcast format configurable via locale file
 *   - Adds server group and broadcaster to message
 */
class CmdBroadcast extends Command {

    public CmdBroadcast(HalfClient client, StringArgumentSeparator command) throws InvalidCommandException {
        super(client, command);
        if (!command.meetsLength(1)) {
            throw new InvalidCommandException(CommandEnum.BROADCAST);
        }
    }

    @Override
    void run() {
        int minimumTalkPower = 0;
        String messageToBroadcast;

        String arg = command.getArgument(0);
        if (arg.startsWith("-") && command.meetsLength(2)) {

            messageToBroadcast = command.getConcatenatedString(1);
            try {
                minimumTalkPower = Integer.parseInt(arg.substring(1));
            } catch (NumberFormatException ignored) {}

        } else {
            messageToBroadcast = command.getConcatenatedString();
        }

        MessageBuilder.create("cmdBroadcastFormat")
                .addPlaceholderReplace("GROUP", api.getServerGroupsByClient(clientInfo).get(0).getName())
                .addPlaceholderReplace("NICKNAME", clientInfo.getNickname())
                .addPlaceholderReplace("MESSAGE", messageToBroadcast)
                .broadcastMessage(false, minimumTalkPower);
    }
}
