package de.halfminer.hmbot.task;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * {@link java.util.concurrent.ExecutorService} task scheduler implementation.
 */
public class ExecutorScheduler implements Scheduler {

    private ScheduledExecutorService service;
    private Map<Task, ScheduledFuture<?>> registeredTasks;

    private List<Task> allTasks;

    public ExecutorScheduler() {
        createNewThreadPool();
    }

    @Override
    public void createNewThreadPool() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("tasks-%d")
                .setDaemon(true)
                .build();

        service = Executors.newScheduledThreadPool(4, threadFactory);
        registeredTasks = new ConcurrentHashMap<>();
    }

    @Override
    public void registerAllTasks() {
        allTasks = Arrays.asList(new InactivityTask());
        for (Task task : allTasks) {
            registerTask(task);
        }
    }

    @Override
    public void scheduleRunnable(Runnable toSchedule, int initialDelay, int period, TimeUnit unit) {
        service.scheduleAtFixedRate(toSchedule, initialDelay, period, unit);
    }

    @Override
    public void shutdown() {
        service.shutdownNow();
        registeredTasks.clear();
    }

    @Override
    public void configWasReloaded() {
        for (Task task : allTasks) {
            task.configWasReloaded();
            updateTaskRegister(task);
        }
    }

    private void updateTaskRegister(Task task) {

        if (registeredTasks.containsKey(task)) {

            ScheduledFuture<?> registeredTask = registeredTasks.get(task);
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
