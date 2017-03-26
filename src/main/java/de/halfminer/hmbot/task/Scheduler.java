package de.halfminer.hmbot.task;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Managing the {@link java.util.concurrent.ExecutorService ExecutorServices's} associated task's.
 */
public class Scheduler {

    private final ScheduledExecutorService service;
    private final Map<Task, ScheduledFuture> registeredTasks = new ConcurrentHashMap<>();

    private List<Task> allTasks;

    public Scheduler() {

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("tasks-%d")
                .setDaemon(true)
                .build();

        service = Executors.newScheduledThreadPool(4, threadFactory);
    }

    public void registerAllTasks() {
        allTasks = Arrays.asList(new InactivityTask(), new ReloadConfigTask(), new StatusTask());
        for (Task task : allTasks) {
            registerTask(task);
        }
    }

    @SuppressWarnings("SameParameterValue")
    public void scheduleRunnable(Runnable toSchedule, int initialDelay, int period, TimeUnit unit) {
        service.scheduleAtFixedRate(toSchedule, initialDelay, period, unit);
    }

    public void shutdown() {
        service.shutdownNow();
    }

    void configWasReloaded() {
        for (Task task : allTasks) {
            task.configWasReloaded();
            updateTaskRegister(task);
        }
    }

    private void updateTaskRegister(Task task) {

        if (registeredTasks.containsKey(task)) {

            ScheduledFuture registeredTask = registeredTasks.get(task);
            if (task.shouldRegisterTask()) {
                // check if period was changed and re-register if required
                if (task.shouldReregisterTask()) {
                    registeredTask.cancel(false);
                    registerTask(task);
                }
            } else {
                registeredTask.cancel(false);
            }

        } else registerTask(task);
    }

    private void registerTask(Task task) {
        if (task.shouldRegisterTask()) {
            registeredTasks.put(task,
                    service.scheduleAtFixedRate(task, task.getInitialDelay(), task.getPeriod(), task.getTimeUnit()));
        }
    }
}
