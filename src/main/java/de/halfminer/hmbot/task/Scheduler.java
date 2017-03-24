package de.halfminer.hmbot.task;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Managing the {@link java.util.concurrent.ExecutorService ExecutorServices's} associated task's.
 */
public class Scheduler {

    private final ScheduledExecutorService service;

    public Scheduler() {

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("tasks-%d")
                .setDaemon(true)
                .build();

        service = Executors.newScheduledThreadPool(2, threadFactory);
    }

    public void registerAllTasks() {
        for (Task task : Arrays.asList(new InactivityTask(), new ReloadConfigTask(), new StatusTask())) {
            service.scheduleAtFixedRate(task, task.getInitialDelay(), task.getPeriod(), task.getUnit());
        }
    }

    public void scheduleRunnable(Runnable toSchedule, int initialDelay, int period, TimeUnit unit) {
        service.scheduleAtFixedRate(toSchedule, initialDelay, period, unit);
    }
}
