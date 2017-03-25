package de.halfminer.hmbot.task;

import de.halfminer.hmbot.storage.Storage;

/**
 * Automatically reloads the config file on demand.
 */
class ReloadConfigTask extends Task {

    private final Storage storage = bot.getStorage();

    @Override
    void execute() {
        if (config.reloadConfig()) {
            logger.info("Config file was reloaded");
            scheduler.configWasReloaded();
            storage.configWasReloaded();
        }
    }
}
