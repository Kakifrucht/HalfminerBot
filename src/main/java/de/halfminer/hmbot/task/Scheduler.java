package de.halfminer.hmbot.task;

import java.util.concurrent.TimeUnit;

/**
 * Interface to schedule {@link Task tasks} and {@link Runnable runnables}.
 */
public interface Scheduler {

    void createNewThreadPool();

    void registerAllTasks();

    void scheduleRunnable(Runnable toSchedule, int initialDelay, int period, TimeUnit unit);

    void shutdown();

    void configWasReloaded();
}
