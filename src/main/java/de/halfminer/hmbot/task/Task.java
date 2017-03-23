package de.halfminer.hmbot.task;

import de.halfminer.hmbot.HalfminerBotClass;

import java.util.concurrent.TimeUnit;

/**
 * Tasks scheduled by {@link java.util.concurrent.ExecutorService ExecutorService} in {@link TaskManager}.
 */
abstract class Task extends HalfminerBotClass implements Runnable {

    private final int initialDelay;
    private final int period;
    private final TimeUnit unit;

    private boolean isEnabled = true;

    Task(int initialDelay, int period, TimeUnit unit) {
        this.initialDelay = initialDelay;
        this.period = period;
        this.unit = unit;
        isEnabled = checkIfEnabled();
    }

    /**
     * Check if task should be run.
     *
     * @return true if it should, else false
     */
    abstract boolean checkIfEnabled();

    @Override
    public void run() {
        if (isEnabled) execute();
        else {
            isEnabled = checkIfEnabled();
            run();
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

    TimeUnit getUnit() {
        return unit;
    }
}
