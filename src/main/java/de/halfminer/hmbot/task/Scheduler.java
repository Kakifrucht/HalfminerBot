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

    private ScheduledExecutorService service;
    private Map<Task, ScheduledFuture> registeredTasks;

    private List<Task> allTasks;

    public Scheduler() {
        createNewThreadPool();
    }

    public void createNewThreadPool() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("tasks-%d")
                .setDaemon(true)
                .build();

        service = Executors.newScheduledThreadPool(4, threadFactory);
        registeredTasks = new ConcurrentHashMap<>();
    }

    public void registerAllTasks() {
        allTasks = Arrays.asList(new InactivityTask(), new StatusTask());
        for (Task task : allTasks) {
            registerTask(task);
        }
    }

    public void scheduleRunnable(Runnable toSchedule, int initialDelay, int period, TimeUnit unit) {
        service.scheduleAtFixedRate(toSchedule, initialDelay, period, unit);
    }

    public void shutdown() {
        service.shutdownNow();
        registeredTasks.clear();
    }

    public void configWasReloaded() {
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
