package de.halfminer.hmbot.cmd;

import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;

@SuppressWarnings("SameParameterValue")
class CommandNotCompletedException extends Exception {

    private final Command command;
    private final String error;
    private String tellUser = null;

    CommandNotCompletedException(Command command, String error, String tellUser) {
        this.command = command;
        this.error = error;
        this.tellUser = tellUser;
    }

    String getError() {
        ClientInfo info = command.getClientInfo();
        return info.getNickname() + " caused error with commandLine \"" + command.getCommand() + "\": " + error;
    }

    boolean doTellUser() {
        return tellUser != null;
    }

    String toTellUser() {
        return tellUser;
    }
}
