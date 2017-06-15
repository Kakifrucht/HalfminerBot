package de.halfminer.hmbot;

/**
 * Bot state related interface.
 */
public interface StateHolder {

    /**
     * @return the bot's version number.
     */
    String getVersion();

    /**
     * Reload the bots config.
     *
     * @return true if config was reloaded, false if it couldn't be read or no changes were made to it
     */
    boolean reloadConfig();

    /**
     * Stop or restart the bot.
     *
     * @param message will be logged
     * @param restart if true, bot will be restarted after shutdown
     */
    void stop(String message, boolean restart);
}
