package de.halfminer.hmbot.task;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Managing the {@link java.util.concurrent.ExecutorService ExecutorServices's} associated task's.
 */
public class TaskManager {

    public TaskManager() {

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("tasks-%d")
                .setDaemon(true)
                .build();

        ScheduledExecutorService service = Executors.newScheduledThreadPool(2, threadFactory);

        for (Task task : Arrays.asList(new InactivityTask(), new StatusTask())) {
            service.scheduleAtFixedRate(task, task.getInitialDelay(), task.getPeriod(), task.getUnit());
        }
    }

}
