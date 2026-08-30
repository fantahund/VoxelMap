package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;

public final class ThreadManager {
    static final int CALCULATION_WORKER_COUNT = calculateCalculationWorkerCount(Runtime.getRuntime().availableProcessors());
    static final int SAVE_WORKER_COUNT = 1;
    static final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    public static final ThreadPoolExecutor executorService = createExecutor(
            CALCULATION_WORKER_COUNT, queue, "Voxelmap WorldMap Calculation Thread", true);
    public static ThreadPoolExecutor saveExecutorService = createSaveExecutor();

    private ThreadManager() {}

    public static void emptyQueue() {
        int cancellations = 0;
        for (Runnable runnable : queue) {
            if (runnable instanceof FutureTask) {
                if (((FutureTask<?>) runnable).cancel(false)) {
                    ++cancellations;
                }
            }
        }

        executorService.purge();
        PersistentMapProfiler.recordQueueCancellations(cancellations);
    }

    static boolean cancelQueued(Future<?> future) {
        if (!(future instanceof Runnable runnable) || !queue.remove(runnable)) {
            return false;
        }
        boolean cancelled = future.cancel(false);
        if (cancelled) {
            PersistentMapProfiler.recordQueueCancellations(1);
        }
        return cancelled;
    }

    public static void flushSaveQueue() {
        saveExecutorService.shutdown();
        try {
            while (!saveExecutorService.awaitTermination(240, TimeUnit.SECONDS)) {
                VoxelConstants.getLogger().info("Waiting for map save... (" + saveExecutorService.getQueue().size() + ")");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        saveExecutorService = createSaveExecutor();
        VoxelConstants.getLogger().info("Save queue flushed!");
    }

    public static void shutdownCalculationQueue() {
        emptyQueue();
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();

                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    VoxelConstants.getLogger().warn("Voxelmap WorldMap Calculation Thread pool did not stop within shutdown timeout");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    static int calculateCalculationWorkerCount(int availableProcessors) {
        return Math.min(Math.max(availableProcessors / 2, 1), 4);
    }

    private static ThreadPoolExecutor createSaveExecutor() {
        return createExecutor(
                SAVE_WORKER_COUNT,
                new LinkedBlockingQueue<>(),
                "Voxelmap WorldMap Saver Thread",
                false);
    }

    private static ThreadPoolExecutor createExecutor(
            int workerCount, LinkedBlockingQueue<Runnable> workQueue, String threadName, boolean daemon) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                workerCount,
                workerCount,
                60L,
                TimeUnit.SECONDS,
                workQueue,
                new NamedThreadFactory(threadName, daemon));
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final String name;
        private final AtomicInteger threadCount = new AtomicInteger(1);
        private final boolean daemon;

        private NamedThreadFactory(String name, boolean daemon) {
            this.name = name;
            this.daemon = daemon;
        }

        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread thread = new Thread(r, this.name + " " + this.threadCount.getAndIncrement());
            thread.setDaemon(this.daemon);
            return thread;
        }
    }
}
