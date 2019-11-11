package de.halfminer.hmbot.config;

/**
 * Configuration interface, to reload and grab data from.
 */
public interface BotConfig {

    /**
     * Reloads the configuration file. Will only run if file was modified since last
     * reload and won't reload if configuration is broken.
     *
     * @return true if reload was successful, false if not modified/written or a config exception was thrown
     */
    boolean reloadConfig();

    boolean isUsingDefaultConfig();

    boolean getBoolean(String path);

    int getInt(String path);

    String getString(String path);

    Object get(String path, Class<?> instanceOf);
}
