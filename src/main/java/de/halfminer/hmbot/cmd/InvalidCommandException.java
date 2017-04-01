package de.halfminer.hmbot.cmd;

import de.halfminer.hmbot.util.MessageBuilder;

/**
 * Exception thrown when a command wasn't executed with the proper syntax.
 */
@SuppressWarnings("SameParameterValue")
class InvalidCommandException extends Exception {

    private final CommandEnum callingCommand;
    private final boolean hasCause;

    InvalidCommandException(CommandEnum callingCommand) {
        this.callingCommand = callingCommand;
        this.hasCause = false;
    }

    InvalidCommandException(CommandEnum callingCommand, Throwable cause) {
        super(cause);
        this.hasCause = true;
        this.callingCommand = callingCommand;
    }

    String getUsage() {
        return MessageBuilder.returnMessage(callingCommand.getUsageKey());
    }

    boolean hasCause() {
        return hasCause;
    }
}
