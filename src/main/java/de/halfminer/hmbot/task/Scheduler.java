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

        service = Executors.newScheduledThreadPool(2, threadFactory);
    }

    public void registerAllTasks() {
        allTasks = Arrays.asList(new InactivityTask(), new ReloadConfigTask(), new StatusTask());
        for (Task task : allTasks) {
            updateTaskRegister(task);
        }
    }

    public void scheduleRunnable(Runnable toSchedule, int initialDelay, int period, TimeUnit unit) {
        service.scheduleAtFixedRate(toSchedule, initialDelay, period, unit);
    }

    void configWasReloaded() {
        for (Task task : allTasks) {
            task.configWasReloaded();
            updateTaskRegister(task);
        }
    }

    private void updateTaskRegister(Task task) {
        //TODO reregister task only if line was actually changed
        if (task.shouldRegisterTask() && !registeredTasks.containsKey(task)) {
            ScheduledFuture future =
                    service.scheduleAtFixedRate(task, task.getInitialDelay(), task.getPeriod(), task.getUnit());
            registeredTasks.put(task, future);
        } else if (!task.shouldRegisterTask() && registeredTasks.containsKey(task)) {
            registeredTasks.get(task).cancel(false);
            registeredTasks.remove(task);
        }
    }
}
