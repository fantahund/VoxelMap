package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;

public final class ThreadManager {
    static final int CALCULATION_WORKER_COUNT = calculateCalculationWorkerCount(Runtime.getRuntime().availableProcessors());
    static final int BACKGROUND_WORKER_COUNT = 1;
    static final int SAVE_WORKER_COUNT = 1;
    static final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private static final AtomicInteger BACKGROUND_SEQUENCE = new AtomicInteger();
    private static final PriorityBlockingQueue<Runnable> backgroundQueue = new PriorityBlockingQueue<>();
    public static final ThreadPoolExecutor executorService = createExecutor(
            CALCULATION_WORKER_COUNT, queue, "Voxelmap WorldMap Calculation Thread", true, Thread.NORM_PRIORITY);
    static final ThreadPoolExecutor backgroundExecutorService = createExecutor(
            BACKGROUND_WORKER_COUNT,
            backgroundQueue,
            "Voxelmap WorldMap Background Thread",
            true,
            Thread.NORM_PRIORITY - 1);
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
        backgroundExecutorService.shutdown();

        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();

                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    VoxelConstants.getLogger().warn("Voxelmap WorldMap Calculation Thread pool did not stop within shutdown timeout");
                }
            }
            if (!backgroundExecutorService.awaitTermination(10, TimeUnit.SECONDS)) {
                backgroundExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            backgroundExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    static void executeBackground(Runnable runnable) {
        backgroundExecutorService.execute(new PrioritizedTask(runnable, false, BACKGROUND_SEQUENCE.getAndIncrement()));
    }

    static void executeBackgroundMaintenance(Runnable runnable) {
        backgroundExecutorService.execute(new PrioritizedTask(runnable, true, BACKGROUND_SEQUENCE.getAndIncrement()));
    }

    static int calculateCalculationWorkerCount(int availableProcessors) {
        return Math.min(Math.max(availableProcessors / 2, 1), 4);
    }

    private static ThreadPoolExecutor createSaveExecutor() {
        return createExecutor(
                SAVE_WORKER_COUNT,
                new LinkedBlockingQueue<>(),
                "Voxelmap WorldMap Saver Thread",
                false,
                Thread.NORM_PRIORITY - 1);
    }

    private static ThreadPoolExecutor createExecutor(
            int workerCount, BlockingQueue<Runnable> workQueue, String threadName, boolean daemon, int priority) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                workerCount,
                workerCount,
                60L,
                TimeUnit.SECONDS,
                workQueue,
                new NamedThreadFactory(threadName, daemon, priority));
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final String name;
        private final AtomicInteger threadCount = new AtomicInteger(1);
        private final boolean daemon;
        private final int priority;

        private NamedThreadFactory(String name, boolean daemon, int priority) {
            this.name = name;
            this.daemon = daemon;
            this.priority = priority;
        }

        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread thread = new Thread(r, this.name + " " + this.threadCount.getAndIncrement());
            thread.setDaemon(this.daemon);
            thread.setPriority(this.priority);
            return thread;
        }
    }

    private record PrioritizedTask(Runnable delegate, boolean maintenance, int sequence) implements Runnable, Comparable<PrioritizedTask> {
        @Override
        public void run() {
            this.delegate.run();
        }

        @Override
        public int compareTo(PrioritizedTask other) {
            int priorityComparison = Boolean.compare(this.maintenance, other.maintenance);
            return priorityComparison != 0 ? priorityComparison : Integer.compareUnsigned(this.sequence, other.sequence);
        }
    }
}
