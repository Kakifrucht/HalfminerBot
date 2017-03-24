package de.halfminer.hmbot.task;

import java.util.concurrent.TimeUnit;

/**
 * Automatically reloads the config file on demand.
 */
class ReloadConfigTask extends Task {

    ReloadConfigTask() {
        super(20, 20, TimeUnit.SECONDS);
    }

    @Override
    void execute() {
        if (config.reloadConfig()) {
            logger.info("Config file was reloaded");
        }
    }
}
