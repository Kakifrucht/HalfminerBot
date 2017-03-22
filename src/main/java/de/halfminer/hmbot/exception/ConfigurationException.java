package de.halfminer.hmbot.exception;

public class ConfigurationException extends Exception {

    private boolean printStacktrace = false;

    public ConfigurationException() {
        printStacktrace = true;
    }

    public ConfigurationException(String error) {
        super(error);
    }

    public ConfigurationException(String error, Throwable cause) {
        super(error, cause);
        printStacktrace = true;
    }

    public boolean shouldPrintStacktrace() {
        return printStacktrace;
    }
}
