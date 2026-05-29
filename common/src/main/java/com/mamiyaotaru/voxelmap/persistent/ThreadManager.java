package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;

public final class ThreadManager {
    static final int concurrentThreads = Math.min(Math.max(Runtime.getRuntime().availableProcessors() / 2, 1), 4);
    static final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    public static final ThreadPoolExecutor executorService = new ThreadPoolExecutor(0, concurrentThreads, 60L, TimeUnit.SECONDS, queue);
    public static ThreadPoolExecutor saveExecutorService = new ThreadPoolExecutor(0, concurrentThreads, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    private ThreadManager() {}

    public static void emptyQueue() {
        for (Runnable runnable : queue) {
            if (runnable instanceof FutureTask) {
                ((FutureTask<?>) runnable).cancel(false);
            }
        }

        executorService.purge();
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
        saveExecutorService = new ThreadPoolExecutor(0, concurrentThreads, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        saveExecutorService.setThreadFactory(new NamedThreadFactory("Voxelmap WorldMap Saver Thread", false));
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

    static {
        executorService.setThreadFactory(new NamedThreadFactory("Voxelmap WorldMap Calculation Thread", true));
        saveExecutorService.setThreadFactory(new NamedThreadFactory("Voxelmap WorldMap Saver Thread", false));
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
