package de.halfminer.hmbot.cmd;

import de.halfminer.hmbot.util.MessageBuilder;

/**
 * Exception thrown when a command wasn't executed with the proper syntax.
 */
@SuppressWarnings("SameParameterValue")
class InvalidCommandException extends Exception {

    private final CommandEnum callingCommand;

    InvalidCommandException(CommandEnum callingCommand) {
        this.callingCommand = callingCommand;
    }

    String getUsage() {
        return MessageBuilder.returnMessage(callingCommand.getUsageKey());
    }
}
