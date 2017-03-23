package de.halfminer.hmbot.cmd;

@SuppressWarnings("SameParameterValue")
class InvalidCommandLineException extends Exception {

    private final String error;
    private final String correctUsage;

    InvalidCommandLineException(String error, String correctUsage) {
        this.error = error;
        this.correctUsage = correctUsage;
    }

    String getError() {
        return error;
    }

    String getCorrectUsage() {
        return correctUsage;
    }
}
