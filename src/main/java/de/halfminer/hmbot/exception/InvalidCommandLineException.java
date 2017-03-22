package de.halfminer.hmbot.exception;

@SuppressWarnings("SameParameterValue")
public class InvalidCommandLineException extends Exception {

    private final String error;
    private final String correctUsage;

    public InvalidCommandLineException(String error, String correctUsage) {
        this.error = error;
        this.correctUsage = correctUsage;
    }

    public String getError() {
        return error;
    }

    public String getCorrectUsage() {
        return correctUsage;
    }
}
