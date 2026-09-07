package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.world.level.chunk.LevelChunk;

/** Coalesces durable persistent-map updates independently from cancellable image refreshes. */
final class PersistentMapChunkUpdateScheduler {
    static final long QUIET_TICKS = 40L;
    static final long MAXIMUM_TICKS = 80L;

    private final PersistentMap persistentMap;
    private final Object stateLock = new Object();
    private final Map<Long, PendingUpdate> pending = new HashMap<>();
    private final Map<Long, List<PendingUpdate>> inFlight = new HashMap<>();
    private final PriorityQueue<Deadline> deadlines = new PriorityQueue<>(Comparator.comparingLong(Deadline::tick));
    private final ConcurrentHashMap<Long, RegionQueue> regionQueues = new ConcurrentHashMap<>();
    private long currentTick;
    private long nextGeneration;
    private volatile long worldEpoch;

    PersistentMapChunkUpdateScheduler(PersistentMap persistentMap) {
        this.persistentMap = persistentMap;
    }

    void enqueue(LevelChunk chunk) {
        int chunkX = chunk.getPos().x();
        int chunkZ = chunk.getPos().z();
        long key = chunkKey(chunkX, chunkZ);
        boolean coalesced;
        synchronized (this.stateLock) {
            PendingUpdate update = this.pending.get(key);
            coalesced = update != null;
            if (update == null) {
                update = new PendingUpdate(key, chunkX, chunkZ, chunk, this.currentTick, ++this.nextGeneration, this.worldEpoch);
                this.pending.put(key, update);
            } else {
                update.chunk = chunk;
                update.lastTick = this.currentTick;
                update.generation = ++this.nextGeneration;
                update.snapshot = null;
            }
            update.deadline = calculateDeadline(update.firstTick, update.lastTick);
            this.deadlines.add(new Deadline(update.deadline, update.generation, update));
        }
        PersistentMapProfiler.recordBackgroundChunkReceived(coalesced);
    }

    void tick() {
        Map<Long, List<PendingUpdate>> dueByRegion = new HashMap<>();
        synchronized (this.stateLock) {
            ++this.currentTick;
            this.discardStaleDeadlinesLocked();
            while (!this.deadlines.isEmpty() && this.deadlines.peek().tick <= this.currentTick) {
                Deadline deadline = this.deadlines.remove();
                PendingUpdate update = deadline.update;
                if (!this.isCurrentDeadlineLocked(deadline)) {
                    continue;
                }
                this.pending.remove(update.key);
                this.inFlight.computeIfAbsent(update.key, ignored -> new ArrayList<>()).add(update);
                long regionKey = regionKey(Math.floorDiv(update.chunkX, 16), Math.floorDiv(update.chunkZ, 16));
                dueByRegion.computeIfAbsent(regionKey, ignored -> new ArrayList<>()).add(update);
            }
        }

        for (Map.Entry<Long, List<PendingUpdate>> entry : dueByRegion.entrySet()) {
            this.enqueueRegionBatch(entry.getKey(), entry.getValue());
        }
        this.reportBackgroundIdleIfNeeded();
    }

    boolean hasDueOrRunningWork() {
        synchronized (this.stateLock) {
            this.discardStaleDeadlinesLocked();
            return !this.inFlight.isEmpty() || !this.deadlines.isEmpty() && this.deadlines.peek().tick <= this.currentTick;
        }
    }

    void captureBeforeUnload(LevelChunk chunk) {
        List<PendingUpdate> updates = this.findUpdates(chunk.getPos().x(), chunk.getPos().z());
        if (updates.isEmpty()) {
            return;
        }

        long startedNanos = PersistentMapProfiler.startBackgroundTimer();
        long waitStartedNanos = System.nanoTime();
        updates.sort(Comparator.comparingLong(update -> update.generation));
        for (PendingUpdate update : updates) {
            update.captureLock.lock();
        }
        long waitedNanos = System.nanoTime() - waitStartedNanos;

        try {
            boolean snapshotRequired = false;
            for (PendingUpdate update : updates) {
                snapshotRequired |= update.snapshot == null;
            }
            if (snapshotRequired) {
                PendingChunkSnapshot snapshot = PendingChunkSnapshot.capture(this.persistentMap, this.persistentMap.getWorld(), chunk);
                for (PendingUpdate update : updates) {
                    if (update.epoch == this.worldEpoch && update.snapshot == null) {
                        update.snapshot = snapshot;
                    }
                }
            }
            if (snapshotRequired) {
                PersistentMapProfiler.recordBackgroundUnloadSnapshot(startedNanos, waitedNanos, updates.size());
            }
        } finally {
            for (int index = updates.size() - 1; index >= 0; --index) {
                updates.get(index).captureLock.unlock();
            }
        }
    }

    void captureAllPendingBeforeStorageChange() {
        Map<Long, LevelChunk> chunks = new HashMap<>();
        synchronized (this.stateLock) {
            for (PendingUpdate update : this.pending.values()) {
                if (update.epoch == this.worldEpoch && update.snapshot == null && this.persistentMap.isChunkAvailable(update.chunk)) {
                    chunks.put(update.key, update.chunk);
                }
            }
            for (List<PendingUpdate> active : this.inFlight.values()) {
                for (PendingUpdate update : active) {
                    if (update.epoch == this.worldEpoch && update.snapshot == null && this.persistentMap.isChunkAvailable(update.chunk)) {
                        chunks.put(update.key, update.chunk);
                    }
                }
            }
        }
        for (LevelChunk chunk : chunks.values()) {
            this.captureBeforeUnload(chunk);
        }
    }

    void flushBeforeWorldChange() {
        this.captureAllPendingBeforeStorageChange();
        List<PendingUpdate> updates = new ArrayList<>();
        synchronized (this.stateLock) {
            updates.addAll(this.pending.values());
            for (List<PendingUpdate> active : this.inFlight.values()) {
                updates.addAll(active);
            }
        }

        Map<Long, List<PendingUpdate>> byRegion = new HashMap<>();
        for (PendingUpdate update : updates) {
            if (update.snapshot != null && update.epoch == this.worldEpoch) {
                long key = regionKey(Math.floorDiv(update.chunkX, 16), Math.floorDiv(update.chunkZ, 16));
                byRegion.computeIfAbsent(key, ignored -> new ArrayList<>()).add(update);
            }
        }
        for (Map.Entry<Long, List<PendingUpdate>> entry : byRegion.entrySet()) {
            this.processBatch(entry.getKey(), entry.getValue());
        }

        synchronized (this.stateLock) {
            ++this.worldEpoch;
            this.pending.clear();
            this.inFlight.clear();
            this.deadlines.clear();
            this.regionQueues.clear();
        }
    }

    long currentEpoch() {
        synchronized (this.stateLock) {
            return this.worldEpoch;
        }
    }

    private void enqueueRegionBatch(long regionKey, List<PendingUpdate> updates) {
        RegionQueue regionQueue = this.regionQueues.computeIfAbsent(regionKey, ignored -> new RegionQueue());
        boolean submit;
        synchronized (regionQueue) {
            regionQueue.updates.addAll(updates);
            submit = !regionQueue.scheduled;
            regionQueue.scheduled = true;
        }
        if (!submit) {
            return;
        }

        Executor executor = this.persistentMap.isMapScreenOpen() ? ThreadManager.executorService : ThreadManager::executeBackground;
        executor.execute(() -> this.runRegionQueue(regionKey, regionQueue));
    }

    private void runRegionQueue(long regionKey, RegionQueue regionQueue) {
        while (true) {
            List<PendingUpdate> updates = new ArrayList<>();
            synchronized (regionQueue) {
                PendingUpdate update;
                while ((update = regionQueue.updates.poll()) != null) {
                    updates.add(update);
                }
                if (updates.isEmpty()) {
                    regionQueue.scheduled = false;
                    return;
                }
            }
            this.processBatch(regionKey, updates);
        }
    }

    private void processBatch(long regionKey, List<PendingUpdate> updates) {
        long epoch = this.currentEpoch();
        List<CapturedUpdate> captured = new ArrayList<>(updates.size());
        for (PendingUpdate update : updates) {
            if (update.epoch != epoch) {
                this.complete(update);
                continue;
            }

            update.captureLock.lock();
            try {
                if (update.snapshot == null && this.persistentMap.isChunkAvailable(update.chunk)) {
                    long startedNanos = PersistentMapProfiler.startBackgroundTimer();
                    update.snapshot = PendingChunkSnapshot.capture(this.persistentMap, this.persistentMap.getWorld(), update.chunk);
                    PersistentMapProfiler.recordBackgroundChunkCapture(startedNanos, false);
                }
                if (update.snapshot != null) {
                    captured.add(new CapturedUpdate(update.snapshot, update.generation));
                }
            } catch (RuntimeException exception) {
                VoxelConstants.getLogger().error("Failed to capture persistent-map chunk {},{}", update.chunkX, update.chunkZ, exception);
            } finally {
                update.captureLock.unlock();
            }
        }

        if (!captured.isEmpty() && epoch == this.currentEpoch()) {
            int regionX = unpackX(regionKey);
            int regionZ = unpackZ(regionKey);
            long startedNanos = PersistentMapProfiler.startBackgroundTimer();
            boolean coldRegionLoad = this.persistentMap.applyChunkSnapshots(regionX, regionZ, captured, epoch);
            PersistentMapProfiler.recordBackgroundRegionBatch(
                    startedNanos, captured.size(), !this.persistentMap.isMapScreenOpen(), coldRegionLoad);
        }

        for (PendingUpdate update : updates) {
            if (update.snapshot == null && update.epoch == this.currentEpoch()) {
                this.requeueUntilReload(update);
            } else {
                this.complete(update);
            }
        }
        this.persistentMap.requestChunkMaintenance();
        this.reportBackgroundIdleIfNeeded();
    }

    private void complete(PendingUpdate update) {
        synchronized (this.stateLock) {
            this.removeInFlightLocked(update);
            this.stateLock.notifyAll();
        }
    }

    private void requeueUntilReload(PendingUpdate update) {
        synchronized (this.stateLock) {
            this.removeInFlightLocked(update);
            PendingUpdate newer = this.pending.get(update.key);
            if (newer == null || newer.generation < update.generation) {
                update.deadline = Long.MAX_VALUE;
                this.pending.put(update.key, update);
            }
            this.stateLock.notifyAll();
        }
    }

    private void removeInFlightLocked(PendingUpdate update) {
        List<PendingUpdate> active = this.inFlight.get(update.key);
        if (active != null) {
            active.remove(update);
            if (active.isEmpty()) {
                this.inFlight.remove(update.key);
            }
        }
    }

    private List<PendingUpdate> findUpdates(int chunkX, int chunkZ) {
        long key = chunkKey(chunkX, chunkZ);
        synchronized (this.stateLock) {
            List<PendingUpdate> result = new ArrayList<>();
            PendingUpdate waiting = this.pending.get(key);
            if (waiting != null && waiting.epoch == this.worldEpoch) {
                result.add(waiting);
            }
            List<PendingUpdate> active = this.inFlight.get(key);
            if (active != null) {
                for (PendingUpdate update : active) {
                    if (update.epoch == this.worldEpoch) {
                        result.add(update);
                    }
                }
            }
            return result;
        }
    }

    private void discardStaleDeadlinesLocked() {
        while (!this.deadlines.isEmpty() && !this.isCurrentDeadlineLocked(this.deadlines.peek())) {
            this.deadlines.remove();
        }
    }

    private void reportBackgroundIdleIfNeeded() {
        int waiting;
        boolean idle;
        synchronized (this.stateLock) {
            waiting = this.pending.size() + this.inFlight.values().stream().mapToInt(List::size).sum();
            idle = waiting == 0;
        }
        PersistentMapProfiler.maybeReportBackgroundIdle(
                idle && ThreadManager.backgroundExecutorService.getActiveCount() == 0 && ThreadManager.backgroundExecutorService.getQueue().isEmpty(),
                waiting);
    }

    private boolean isCurrentDeadlineLocked(Deadline deadline) {
        PendingUpdate update = deadline.update;
        return this.pending.get(update.key) == update && update.generation == deadline.generation && update.deadline == deadline.tick;
    }

    static long calculateDeadline(long firstTick, long lastTick) {
        return Math.min(firstTick + MAXIMUM_TICKS, lastTick + QUIET_TICKS);
    }

    private static long chunkKey(int x, int z) {
        return (long) x << 32 ^ z & 0xFFFFFFFFL;
    }

    private static long regionKey(int x, int z) {
        return chunkKey(x, z);
    }

    private static int unpackX(long key) {
        return (int) (key >> 32);
    }

    private static int unpackZ(long key) {
        return (int) key;
    }

    record CapturedUpdate(PendingChunkSnapshot snapshot, long generation) {}

    private static final class PendingUpdate {
        private final long key;
        private final int chunkX;
        private final int chunkZ;
        private final long firstTick;
        private final long epoch;
        private final ReentrantLock captureLock = new ReentrantLock();
        private volatile LevelChunk chunk;
        private volatile long lastTick;
        private volatile long deadline;
        private volatile long generation;
        private volatile PendingChunkSnapshot snapshot;

        private PendingUpdate(long key, int chunkX, int chunkZ, LevelChunk chunk, long tick, long generation, long epoch) {
            this.key = key;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.chunk = chunk;
            this.firstTick = tick;
            this.lastTick = tick;
            this.generation = generation;
            this.epoch = epoch;
        }
    }

    private record Deadline(long tick, long generation, PendingUpdate update) {}

    private static final class RegionQueue {
        private final ConcurrentLinkedQueue<PendingUpdate> updates = new ConcurrentLinkedQueue<>();
        private boolean scheduled;
    }
}
