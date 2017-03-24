package de.halfminer.hmbot.task;

/**
 * Automatically reloads the config file on demand.
 */
class ReloadConfigTask extends Task {

    @Override
    void execute() {
        if (config.reloadConfig()) {
            logger.info("Config file was reloaded");
            scheduler.configWasReloaded();
        }
    }
}
