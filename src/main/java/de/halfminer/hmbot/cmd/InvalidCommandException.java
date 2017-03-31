package de.halfminer.hmbot.cmd;

import de.halfminer.hmbot.util.MessageBuilder;

@SuppressWarnings("SameParameterValue")
class InvalidCommandException extends Exception {

    private final String usageKey;

    InvalidCommandException(String usageKey) {
        this.usageKey = usageKey;
    }

    String getUsage() {
        return MessageBuilder.returnMessage(usageKey);
    }
}
