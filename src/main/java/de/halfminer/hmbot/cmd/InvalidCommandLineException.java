package de.halfminer.hmbot.cmd;

@SuppressWarnings("SameParameterValue")
class InvalidCommandLineException extends Exception {

    private final String correctUsage;

    InvalidCommandLineException(String correctUsage) {
        this.correctUsage = correctUsage;
    }

    String getCorrectUsage() {
        return correctUsage;
    }
}
