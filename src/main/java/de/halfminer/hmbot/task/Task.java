package de.halfminer.hmbot.task;

import de.halfminer.hmbot.HalfminerBotClass;
import de.halfminer.hmbot.util.StringArgumentSeparator;

import java.util.concurrent.TimeUnit;

/**
 * Tasks scheduled by {@link java.util.concurrent.ExecutorService ExecutorService} in {@link Scheduler}.
 */
abstract class Task extends HalfminerBotClass implements Runnable {

    private int initialDelay;
    private int period;
    private TimeUnit timeUnit;
    private boolean delayHasChanged;

    private boolean isEnabled = true;

    Task() {
        configWasReloaded();
        isEnabled = checkIfEnabled();
    }

    void configWasReloaded() {
        String className = this.getClass().getSimpleName();
        String configNode = "task.settings." + className.substring(0, className.length() - 4);

        String toParse = config.getString(configNode);
        StringArgumentSeparator separator = new StringArgumentSeparator(toParse, ',');

        if (separator.meetsLength(3)) {
            int oldInitialDelay = initialDelay;
            initialDelay = separator.getArgumentInt(0);
            int oldPeriod = period;
            period = separator.getArgumentInt(1);

            TimeUnit oldTimeUnit = timeUnit;
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
            if (oldInitialDelay != initialDelay
                    || oldPeriod != period
                    || (timeUnit != null && !timeUnit.equals(oldTimeUnit))) {
                System.out.println(this.getClass().getSimpleName() + " delay changed to " + period);
                delayHasChanged = true;
            } else delayHasChanged = false;

        } else {
            initialDelay = Integer.MIN_VALUE;
            period = Integer.MIN_VALUE;
            timeUnit = null;
        }
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

    void setTaskDisabled() {
        isEnabled = false;
    }

    @Override
    public void run() {
        if (isEnabled) executeWithCatchAll();
        else {
            isEnabled = checkIfEnabled();
            if (isEnabled) executeWithCatchAll();
        }
    }

    private void executeWithCatchAll() {
        try {
            execute();
        } catch (Throwable e) {
            logger.error("Unhandled exception caught upon task execution", e);
        }
    }

    /**
     * Execute the task.
     */
    abstract void execute();

    int getInitialDelay() {
        return initialDelay;
    }

    int getPeriod() {
        return period;
    }

    TimeUnit getTimeUnit() {
        return timeUnit;
    }
}
