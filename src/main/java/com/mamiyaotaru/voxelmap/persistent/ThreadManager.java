package com.mamiyaotaru.voxelmap.persistent;

import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadManager {
    static final int concurrentThreads = Math.min(Math.max(Runtime.getRuntime().availableProcessors() - 1, 1), 4);
    static final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue();
    public static ThreadPoolExecutor executorService = new ThreadPoolExecutor(concurrentThreads, concurrentThreads, 0L, TimeUnit.MILLISECONDS, queue);

    public static void emptyQueue() {
        for (Runnable runnable : queue) {
            if (runnable instanceof FutureTask) {
                ((FutureTask) runnable).cancel(false);
            }
        }

        executorService.purge();
    }

    static {
        executorService.setThreadFactory(new NamedThreadFactory("Voxelmap WorldMap Calculation Thread"));
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private final String name;
        private final AtomicInteger threadCount = new AtomicInteger(1);

        public NamedThreadFactory(String name) {
            this.name = name;
        }

        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, this.name + " " + this.threadCount.getAndIncrement());
        }
    }
}
