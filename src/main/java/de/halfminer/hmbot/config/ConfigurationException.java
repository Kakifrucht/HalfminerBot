package de.halfminer.hmbot.config;

public class ConfigurationException extends Exception {

    private boolean printStacktrace = false;

    public ConfigurationException() {
        printStacktrace = false;
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
