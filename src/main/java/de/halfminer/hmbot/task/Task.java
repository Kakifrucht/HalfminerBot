package de.halfminer.hmbot.task;

import de.halfminer.hmbot.BotClass;
import de.halfminer.hmbot.util.StringArgumentSeparator;

import java.util.concurrent.TimeUnit;

/**
 * Tasks scheduled by {@link java.util.concurrent.ExecutorService ExecutorService} in {@link Scheduler}.
 */
abstract class Task extends BotClass implements Runnable {

    private int initialDelay;
    private int period;
    private TimeUnit timeUnit;
    private boolean delayHasChanged;

    Task() {
        configWasReloaded();
    }

    void configWasReloaded() {
        String className = this.getClass().getSimpleName();
        String configNode = "task.settings." + className.substring(0, className.length() - 4);

        String toParse = config.getString(configNode);
        StringArgumentSeparator separator = new StringArgumentSeparator(toParse, ',');

        if (separator.meetsLength(3)) {
            int oldInitialDelay = initialDelay;
            int oldPeriod = period;
            TimeUnit oldTimeUnit = timeUnit;

            initialDelay = separator.getArgumentInt(0);
            period = separator.getArgumentInt(1);

            String timeUnitString = separator.getArgument(2).toUpperCase().trim();
            if (timeUnitString.equals("DISABLED")) {
                timeUnit = null;
            } else {
                try {
                    timeUnit = TimeUnit.valueOf(timeUnitString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    timeUnit = null;
                }
            }

            // if values have changed, mark to be reloaded
            delayHasChanged = oldInitialDelay != initialDelay
                    || oldPeriod != period
                    || (timeUnit != null && !timeUnit.equals(oldTimeUnit));

        } else {
            initialDelay = Integer.MIN_VALUE;
            period = Integer.MIN_VALUE;
            timeUnit = null;
        }
    }

    int getInitialDelay() {
        return initialDelay;
    }

    int getPeriod() {
        return period;
    }

    TimeUnit getTimeUnit() {
        return timeUnit;
    }

    boolean shouldRegisterTask() {
        return initialDelay >= 0 && period > 0 && timeUnit != null;
    }

    boolean shouldReregisterTask() {
        boolean delayHasChanged = this.delayHasChanged;
        this.delayHasChanged = false;
        return shouldRegisterTask() && delayHasChanged;
    }

    /**
     * Check if task should be run. True by default.
     *
     * @return true if it should, else false
     */
    boolean checkIfEnabled() {
        return true;
    }

    @Override
    public void run() {
        if (checkIfEnabled()) {
            try {
                execute();
            } catch (Exception e) {
                logger.error("Unhandled exception caught upon task execution", e);
            }
        }
    }

    /**
     * Execute the task. Only called if {@link #checkIfEnabled()} returned true.
     */
    abstract void execute();
}
