package de.halfminer.hmbot.exception;

import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import de.halfminer.hmbot.cmd.abs.Command;

@SuppressWarnings("SameParameterValue")
public class CommandNotCompletedException extends Exception {

    private final Command command;
    private final String error;
    private String tellUser = null;

    public CommandNotCompletedException(Command command, String error) {
        this.command = command;
        this.error = error;
    }

    public CommandNotCompletedException(Command command, String error, String tellUser) {
        this(command, error);
        this.tellUser = tellUser;
    }

    public String getError() {
        ClientInfo info = command.getClientInfo();
        return info.getNickname() + " caused error with commandLine \"" + command.getCommand() + "\": " + error;
    }

    public boolean tellUser() {
        return tellUser != null;
    }

    public String toTellUser() {
        return tellUser;
    }
}
