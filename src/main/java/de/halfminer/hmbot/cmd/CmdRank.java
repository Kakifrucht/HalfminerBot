package de.halfminer.hmbot.cmd;

import de.halfminer.hmbot.storage.HalfClient;
import de.halfminer.hmbot.util.StringArgumentSeparator;

/**
 * - Automatic retrieval of ranks after querying Halfminer REST API for privileges
 */
class CmdRank extends Command {
    public CmdRank(HalfClient client, StringArgumentSeparator command) {
        super(client, command);
    }

    @Override
    void run() throws InvalidCommandException {
        //TODO implement
    }
}
