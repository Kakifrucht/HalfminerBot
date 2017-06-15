package de.halfminer.hmbot.config;

/**
 * {@link BotConfig} with added method to grab a password, that may have been passed via command line interface.
 */
public interface BotPasswordConfig extends BotConfig {

    String getPassword();
}
