package de.halfminer.hmbot;

import de.halfminer.hmbot.config.BotConfig;
import de.halfminer.hmbot.config.BotPasswordConfig;
import de.halfminer.hmbot.storage.Storage;
import de.halfminer.hmbot.task.Scheduler;

/**
 * Interface allows access to globally used components for the bot.
 */
public interface ComponentHolder {

    StateHolder getStateHolder();

    BotPasswordConfig getConfig();

    BotConfig getLocale();

    Scheduler getScheduler();

    TS3ApiWrapper getApiWrapper();

    Storage getStorage();
}
