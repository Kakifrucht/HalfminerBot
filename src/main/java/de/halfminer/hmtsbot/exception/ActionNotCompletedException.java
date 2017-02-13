package de.halfminer.hmtsbot.exception;

import de.halfminer.hmtsbot.HalfminerBot;
import de.halfminer.hmtsbot.actions.Action;

public class ActionNotCompletedException extends Exception {

    private final Action action;
    private final String error;

    public ActionNotCompletedException(Action action, String error) {
        this.action = action;
        this.error = error;
    }

    public ActionNotCompletedException(Action action, String error, String tellUser) {
        this(action, error);
        HalfminerBot.getInstance().getApi().sendPrivateMessage(action.getClientInfo().getId(), tellUser);
    }

    public String getError() {
        return action.getClientInfo().getNickname() + " caused error with command " + '"' + action.getCommand().getLine() + '"' + ": " + error;
    }

}
