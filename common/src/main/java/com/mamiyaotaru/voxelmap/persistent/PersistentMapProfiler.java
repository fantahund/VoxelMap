package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * Low-overhead, opt-in timings for the persistent world map.
 *
 * <p>Enable with {@code -Dvoxelmap.profilePersistentMap=true}. Timings are aggregated instead of
 * logged per region so that profiling does not turn file and console logging into the bottleneck.
 */
final class PersistentMapProfiler {
    static final String ENABLE_PROPERTY = "voxelmap.profilePersistentMap";
    static final int OVERVIEW_COMPARISON_SIGNIFICANT_ERROR = 8;

    private static final long IDLE_REPORT_DELAY_NANOS = TimeUnit.SECONDS.toNanos(1L);
    private static final boolean ENABLED = Boolean.getBoolean(ENABLE_PROPERTY);
    private static final AtomicLong NEXT_SESSION_ID = new AtomicLong();
    private static final AtomicReference<Session> CURRENT_SESSION = new AtomicReference<>();

    private PersistentMapProfiler() {}

    static boolean isActive() {
        return ENABLED && CURRENT_SESSION.get() != null;
    }

    static long startTimer() {
        return isActive() ? System.nanoTime() : 0L;
    }

    static void startSession(int screenWidth, int screenHeight, float zoom, int mapX, int mapZ) {
        if (!ENABLED) {
            return;
        }

        Session session = new Session(NEXT_SESSION_ID.incrementAndGet(), screenWidth, screenHeight, zoom, mapX, mapZ);
        Session previous = CURRENT_SESSION.getAndSet(session);
        if (previous != null) {
            previous.logReport("superseded", zoom, zoom, 0);
        }

        VoxelConstants.getLogger().info(
                "[PersistentMap profile #{}] started; screen={}x{}, zoom={}, center=({}, {}), property=-D{}=true",
                session.id,
                screenWidth,
                screenHeight,
                formatDecimal(zoom),
                mapX,
                mapZ,
                ENABLE_PROPERTY);
    }

    static void finishSession(float zoom, float zoomGoal, int visibleRegions) {
        if (!ENABLED) {
            return;
        }

        Session session = CURRENT_SESSION.getAndSet(null);
        if (session != null) {
            session.logReport("screen-closed", zoom, zoomGoal, visibleRegions);
        }
    }

    static void maybeReportIdle(boolean viewStable, float zoom, float zoomGoal, int visibleRegions) {
        Session session = CURRENT_SESSION.get();
        if (session == null || !viewStable || ThreadManager.executorService.getActiveCount() != 0 || !ThreadManager.executorService.getQueue().isEmpty()) {
            return;
        }

        long activityVersion = session.activityVersion.get();
        if (activityVersion == session.lastReportedActivityVersion || System.nanoTime() - session.lastActivityNanos.get() < IDLE_REPORT_DELAY_NANOS) {
            return;
        }

        synchronized (session) {
            activityVersion = session.activityVersion.get();
            if (activityVersion != session.lastReportedActivityVersion && System.nanoTime() - session.lastActivityNanos.get() >= IDLE_REPORT_DELAY_NANOS) {
                session.lastReportedActivityVersion = activityVersion;
                session.logReport("idle", zoom, zoomGoal, visibleRegions);
            }
        }
    }

    static void recordRegionSelection(long startedNanos, int requestedRegions, int createdRegions, int reusedRegions, int knownEmptyRegions) {
        Session session = CURRENT_SESSION.get();
        if (session == null) {
            return;
        }

        session.regionSelections.increment();
        session.requestedRegions.add(requestedRegions);
        session.createdRegions.add(createdRegions);
        session.reusedRegions.add(reusedRegions);
        session.knownEmptyRegions.add(knownEmptyRegions);
        session.maxRequestedRegions.accumulateAndGet(requestedRegions, Math::max);
        session.record(Stage.REGION_SELECTION, startedNanos, requestedRegions);
    }

    static long recordRefreshScheduled() {
        Session session = CURRENT_SESSION.get();
        if (session == null) {
            return 0L;
        }

        session.refreshScheduled.increment();
        session.touch();
        return System.nanoTime();
    }

    static long recordRefreshStarted(long queuedAtNanos) {
        Session session = CURRENT_SESSION.get();
        if (session == null || queuedAtNanos == 0L) {
            return 0L;
        }

        long startedNanos = System.nanoTime();
        session.refreshStarted.increment();
        session.recordDuration(Stage.REFRESH_QUEUE_WAIT, startedNanos - queuedAtNanos, 0L);
        return startedNanos;
    }

    static void recordRefreshCompleted(long startedNanos) {
        Session session = CURRENT_SESSION.get();
        if (session == null || startedNanos == 0L) {
            return;
        }

        session.refreshCompleted.increment();
        session.record(Stage.REFRESH_TASK, startedNanos, 0L);
    }

    static void recordQueueCancellations(int cancellations) {
        Session session = CURRENT_SESSION.get();
        if (session != null && cancellations > 0) {
            session.queueCancellations.add(cancellations);
            session.touch();
        }
    }

    static void recordRegionLoad(long startedNanos, boolean nonEmpty) {
        Session session = CURRENT_SESSION.get();
        if (session == null) {
            return;
        }

        if (nonEmpty) {
            session.nonEmptyRegionLoads.increment();
        } else {
            session.emptyRegionLoads.increment();
        }
        session.record(Stage.REGION_LOAD, startedNanos, nonEmpty ? 1L : 0L);
    }

    static void recordCacheLookup(long startedNanos, boolean filePresent) {
        Session session = CURRENT_SESSION.get();
        if (session == null) {
            return;
        }

        if (filePresent) {
            session.cacheFilesPresent.increment();
        } else {
            session.cacheFilesMissing.increment();
        }
        session.record(Stage.CACHE_LOOKUP, startedNanos, filePresent ? 1L : 0L);
    }

    static void recordCacheRead(long startedNanos, long decompressedBytes) {
        Session session = CURRENT_SESSION.get();
        if (session == null) {
            return;
        }

        session.decompressedCacheBytes.add(decompressedBytes);
        session.record(Stage.CACHE_READ, startedNanos, decompressedBytes);
    }

    static void recordOverviewRead(long startedNanos, boolean filePresent, boolean hit) {
        Session session = CURRENT_SESSION.get();
        if (session == null) {
            return;
        }
        if (filePresent) {
            session.overviewFilesPresent.increment();
        }
        if (hit) {
            session.overviewHits.increment();
        } else {
            session.overviewMisses.increment();
        }
        session.record(Stage.OVERVIEW_READ, startedNanos, hit ? PersistentMapOverviewCache.RAW_BYTES : 0L);
    }

    static void recordOverviewWrite(long startedNanos, long rawBytes, boolean success) {
        Session session = CURRENT_SESSION.get();
        if (session == null) {
            return;
        }
        if (success) {
            session.overviewWrites.increment();
        } else {
            session.overviewWriteFailures.increment();
        }
        session.record(Stage.OVERVIEW_WRITE, startedNanos, rawBytes);
    }

    static void recordRawRegionLoadSkipped(long overviewBytes) {
        Session session = CURRENT_SESSION.get();
        if (session != null) {
            session.rawRegionLoadsSkipped.increment();
            session.overviewBytesRead.add(overviewBytes);
            session.touch();
        }
    }

    static void recordLightingChange(boolean applied) {
        Session session = CURRENT_SESSION.get();
        if (session != null) {
            if (applied) {
                session.lightingChangesApplied.increment();
            } else {
                session.lightingChangesIgnoredAtLowZoom.increment();
            }
            session.touch();
        }
    }

    static void recordOverviewLightingEvaluation(boolean updated, boolean budgetDeferred) {
        Session session = CURRENT_SESSION.get();
        if (session == null) {
            return;
        }
        session.overviewLightingChecks.increment();
        if (updated) {
            session.overviewLightingUpdates.increment();
        } else if (budgetDeferred) {
            session.overviewLightingBudgetDeferrals.increment();
        } else {
            session.overviewLightingThresholdSkips.increment();
        }
        session.touch();
    }

    static void recordOverviewRelight(long startedNanos) {
        record(Stage.OVERVIEW_RELIGHT, startedNanos, PersistentMapOverviewCache.PIXEL_COUNT);
    }

    static void recordOverviewComparison(long startedNanos, OverviewComparisonMetrics metrics) {
        Session session = CURRENT_SESSION.get();
        if (session == null) {
            return;
        }
        session.overviewComparisons.increment();
        session.v3WaterComparison.add(
                metrics.waterPixels,
                metrics.waterAbsoluteError,
                metrics.waterSignificantPixels,
                metrics.waterMaxError);
        session.v3LandComparison.add(
                metrics.landPixels,
                metrics.landAbsoluteError,
                metrics.landSignificantPixels,
                metrics.landMaxError);
        session.record(Stage.OVERVIEW_COMPARISON, startedNanos, metrics.waterPixels + metrics.landPixels);
    }

    static void recordDisplayChangeRequest() {
        Session session = CURRENT_SESSION.get();
        if (session != null) {
            session.displayChangeFullDetailRequests.increment();
            session.touch();
        }
    }

    static void recordLiveChunkScan(long startedNanos, int chunksChecked, int chunksLoaded) {
        Session session = CURRENT_SESSION.get();
        if (session == null) {
            return;
        }

        session.liveChunksLoaded.add(chunksLoaded);
        session.record(Stage.LIVE_CHUNK_SCAN, startedNanos, chunksChecked);
    }

    static void recordAnvilLoad(long startedNanos) {
        record(Stage.ANVIL_LOAD, startedNanos, 0L);
    }

    static void recordRenderViewCreation(long startedNanos) {
        record(Stage.RENDER_VIEW_CREATION, startedNanos, 0L);
    }

    static void recordImageColoring(long startedNanos, int pixels, boolean partial) {
        Session session = CURRENT_SESSION.get();
        if (session != null) {
            if (partial) {
                session.partialImageRenders.increment();
            } else {
                session.fullImageRenders.increment();
            }
        }
        record(Stage.IMAGE_COLORING, startedNanos, pixels);
    }

    static void recordMipmapGeneration(long startedNanos, int sourcePixels) {
        record(Stage.MIPMAP_GENERATION, startedNanos, sourcePixels);
    }

    static void recordDataCompression(long startedNanos, int uncompressedBytes) {
        record(Stage.DATA_COMPRESSION, startedNanos, uncompressedBytes);
    }

    static void recordDataDecompression(long startedNanos, int uncompressedBytes) {
        record(Stage.DATA_DECOMPRESSION, startedNanos, uncompressedBytes);
    }

    static void recordTextureUpload(long startedNanos, boolean textureCreated) {
        Session session = CURRENT_SESSION.get();
        if (session == null) {
            return;
        }

        if (textureCreated) {
            session.texturesCreated.increment();
        }
        session.record(Stage.TEXTURE_UPLOAD, startedNanos, 0L);
    }

    private static void record(Stage stage, long startedNanos, long units) {
        Session session = CURRENT_SESSION.get();
        if (session != null) {
            session.record(stage, startedNanos, units);
        }
    }

    private static String formatDecimal(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static String formatMillis(long nanos) {
        return formatDecimal(nanos / 1_000_000.0);
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        if (bytes < 1024L * 1024L) {
            return formatDecimal(bytes / 1024.0) + " KiB";
        }
        return formatDecimal(bytes / (1024.0 * 1024.0)) + " MiB";
    }

    private static String formatComparison(ComparisonStats stats) {
        long pixels = stats.pixels.sum();
        if (pixels == 0L) {
            return "n/a";
        }
        double meanAbsoluteError = stats.absoluteError.sum() / (pixels * 3.0);
        double significantPercent = stats.significantPixels.sum() * 100.0 / pixels;
        return formatDecimal(meanAbsoluteError)
                + "/"
                + stats.maxError.get()
                + "/"
                + formatDecimal(significantPercent)
                + "%";
    }

    private enum Stage {
        REGION_SELECTION("region-selection", "regions"),
        REFRESH_QUEUE_WAIT("refresh-queue-wait", null),
        REFRESH_TASK("refresh-task", null),
        REGION_LOAD("region-load", "non-empty"),
        CACHE_LOOKUP("cache-lookup", "files-present"),
        CACHE_READ("cache-read", "raw-bytes"),
        OVERVIEW_READ("overview-read", "raw-bytes"),
        OVERVIEW_WRITE("overview-write", "raw-bytes"),
        OVERVIEW_RELIGHT("overview-relight", "pixels"),
        OVERVIEW_COMPARISON("overview-comparison", "pixels"),
        LIVE_CHUNK_SCAN("live-chunk-scan", "chunks-checked"),
        ANVIL_LOAD("anvil-load", null),
        RENDER_VIEW_CREATION("render-view-creation", null),
        IMAGE_COLORING("image-coloring", "pixels"),
        MIPMAP_GENERATION("mipmap-generation", "source-pixels"),
        DATA_DECOMPRESSION("data-decompression", "raw-bytes"),
        DATA_COMPRESSION("data-compression", "raw-bytes"),
        TEXTURE_UPLOAD("texture-upload", null);

        private final String label;
        private final String unitLabel;

        Stage(String label, String unitLabel) {
            this.label = label;
            this.unitLabel = unitLabel;
        }
    }

    private static final class StageStats {
        private final LongAdder calls = new LongAdder();
        private final LongAdder totalNanos = new LongAdder();
        private final LongAdder units = new LongAdder();
        private final AtomicLong maxNanos = new AtomicLong();

        private void record(long nanos, long unitCount) {
            calls.increment();
            totalNanos.add(nanos);
            units.add(unitCount);
            maxNanos.accumulateAndGet(nanos, Math::max);
        }
    }

    private static final class Session {
        private final long id;
        private final int screenWidth;
        private final int screenHeight;
        private final float initialZoom;
        private final int initialMapX;
        private final int initialMapZ;
        private final long startedNanos = System.nanoTime();
        private final StageStats[] stages = new StageStats[Stage.values().length];

        private final AtomicLong activityVersion = new AtomicLong();
        private final AtomicLong lastActivityNanos = new AtomicLong(startedNanos);
        private volatile long lastReportedActivityVersion = -1L;
        private int reportNumber;

        private final LongAdder regionSelections = new LongAdder();
        private final LongAdder requestedRegions = new LongAdder();
        private final LongAdder createdRegions = new LongAdder();
        private final LongAdder reusedRegions = new LongAdder();
        private final LongAdder knownEmptyRegions = new LongAdder();
        private final AtomicLong maxRequestedRegions = new AtomicLong();

        private final LongAdder refreshScheduled = new LongAdder();
        private final LongAdder refreshStarted = new LongAdder();
        private final LongAdder refreshCompleted = new LongAdder();
        private final LongAdder queueCancellations = new LongAdder();

        private final LongAdder nonEmptyRegionLoads = new LongAdder();
        private final LongAdder emptyRegionLoads = new LongAdder();
        private final LongAdder cacheFilesPresent = new LongAdder();
        private final LongAdder cacheFilesMissing = new LongAdder();
        private final LongAdder decompressedCacheBytes = new LongAdder();
        private final LongAdder overviewFilesPresent = new LongAdder();
        private final LongAdder overviewHits = new LongAdder();
        private final LongAdder overviewMisses = new LongAdder();
        private final LongAdder rawRegionLoadsSkipped = new LongAdder();
        private final LongAdder overviewBytesRead = new LongAdder();
        private final LongAdder overviewWrites = new LongAdder();
        private final LongAdder overviewWriteFailures = new LongAdder();
        private final LongAdder lightingChangesApplied = new LongAdder();
        private final LongAdder lightingChangesIgnoredAtLowZoom = new LongAdder();
        private final LongAdder overviewLightingChecks = new LongAdder();
        private final LongAdder overviewLightingUpdates = new LongAdder();
        private final LongAdder overviewLightingThresholdSkips = new LongAdder();
        private final LongAdder overviewLightingBudgetDeferrals = new LongAdder();
        private final LongAdder displayChangeFullDetailRequests = new LongAdder();
        private final LongAdder liveChunksLoaded = new LongAdder();
        private final LongAdder texturesCreated = new LongAdder();
        private final LongAdder fullImageRenders = new LongAdder();
        private final LongAdder partialImageRenders = new LongAdder();
        private final LongAdder overviewComparisons = new LongAdder();
        private final ComparisonStats v3WaterComparison = new ComparisonStats();
        private final ComparisonStats v3LandComparison = new ComparisonStats();

        private Session(long id, int screenWidth, int screenHeight, float initialZoom, int initialMapX, int initialMapZ) {
            this.id = id;
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
            this.initialZoom = initialZoom;
            this.initialMapX = initialMapX;
            this.initialMapZ = initialMapZ;
            for (int i = 0; i < stages.length; ++i) {
                stages[i] = new StageStats();
            }
        }

        private void record(Stage stage, long startedNanos, long units) {
            if (startedNanos == 0L) {
                return;
            }
            recordDuration(stage, System.nanoTime() - startedNanos, units);
        }

        private void recordDuration(Stage stage, long durationNanos, long units) {
            stages[stage.ordinal()].record(durationNanos, units);
            touch();
        }

        private void touch() {
            lastActivityNanos.set(System.nanoTime());
            activityVersion.incrementAndGet();
        }

        private synchronized void logReport(String reason, float zoom, float zoomGoal, int visibleRegions) {
            ++reportNumber;
            long wallNanos = System.nanoTime() - startedNanos;
            long scheduled = refreshScheduled.sum();
            long started = refreshStarted.sum();
            long completed = refreshCompleted.sum();

            VoxelConstants.getLogger().info(
                    "[PersistentMap profile #{} report {}] reason={}, wall={} ms, screen={}x{}, initialZoom={}, zoom={}->{}, initialCenter=({}, {}), visibleRegions={}, executorWorkers(configured/largest/active)={}/{}/{}, executorQueued={}, saveWorkers(configured/active/queued)={}/{}/{}",
                    id,
                    reportNumber,
                    reason,
                    formatMillis(wallNanos),
                    screenWidth,
                    screenHeight,
                    formatDecimal(initialZoom),
                    formatDecimal(zoom),
                    formatDecimal(zoomGoal),
                    initialMapX,
                    initialMapZ,
                    visibleRegions,
                    ThreadManager.CALCULATION_WORKER_COUNT,
                    ThreadManager.executorService.getLargestPoolSize(),
                    ThreadManager.executorService.getActiveCount(),
                    ThreadManager.executorService.getQueue().size(),
                    ThreadManager.SAVE_WORKER_COUNT,
                    ThreadManager.saveExecutorService.getActiveCount(),
                    ThreadManager.saveExecutorService.getQueue().size());
            VoxelConstants.getLogger().info(
                    "[PersistentMap profile #{} report {}] selections={}, requestedRegions(total/max)={}/{}, regions(created/reused/known-empty)={}/{}/{}, loads(non-empty/empty)={}/{}",
                    id,
                    reportNumber,
                    regionSelections.sum(),
                    requestedRegions.sum(),
                    maxRequestedRegions.get(),
                    createdRegions.sum(),
                    reusedRegions.sum(),
                    knownEmptyRegions.sum(),
                    nonEmptyRegionLoads.sum(),
                    emptyRegionLoads.sum());
            VoxelConstants.getLogger().info(
                    "[PersistentMap profile #{} report {}] refreshTasks(scheduled/started/completed/not-started)={}/{}/{}/{}, queueCancellations={}, imageRenders(full/partial)={}/{}, cacheFiles(present/missing)={}/{}, cacheRaw={}, liveChunksLoaded={}, texturesCreated={}",
                    id,
                    reportNumber,
                    scheduled,
                    started,
                    completed,
                    Math.max(0L, scheduled - started),
                    queueCancellations.sum(),
                    fullImageRenders.sum(),
                    partialImageRenders.sum(),
                    cacheFilesPresent.sum(),
                    cacheFilesMissing.sum(),
                    formatBytes(decompressedCacheBytes.sum()),
                    liveChunksLoaded.sum(),
                    texturesCreated.sum());
            VoxelConstants.getLogger().info(
                    "[PersistentMap profile #{} report {}] overview(files/hits/misses)={}/{}/{}, rawLoadsSkipped={}, overviewRawRead={}, writes(success/failed)={}/{}, lightingChanges(applied/ignored)={}/{}, overviewRelights(checks/updated/threshold-skipped/budget-deferred)={}/{}/{}/{}, displayChangeFullDetailRequests={}",
                    id,
                    reportNumber,
                    overviewFilesPresent.sum(),
                    overviewHits.sum(),
                    overviewMisses.sum(),
                    rawRegionLoadsSkipped.sum(),
                    formatBytes(overviewBytesRead.sum()),
                    overviewWrites.sum(),
                    overviewWriteFailures.sum(),
                    lightingChangesApplied.sum(),
                    lightingChangesIgnoredAtLowZoom.sum(),
                    overviewLightingChecks.sum(),
                    overviewLightingUpdates.sum(),
                    overviewLightingThresholdSkips.sum(),
                    overviewLightingBudgetDeferrals.sum(),
                    displayChangeFullDetailRequests.sum());
            if (overviewComparisons.sum() > 0L) {
                VoxelConstants.getLogger().info(
                        "[PersistentMap profile #{} report {}] overviewComparison(tiles/waterCells/landCells)={}/{}/{}, metric=rgb-mae/max/pct(max-channel-error>={}), v3(water/land)={}/{}",
                        id,
                        reportNumber,
                        overviewComparisons.sum(),
                        v3WaterComparison.pixels.sum(),
                        v3LandComparison.pixels.sum(),
                        OVERVIEW_COMPARISON_SIGNIFICANT_ERROR,
                        formatComparison(v3WaterComparison),
                        formatComparison(v3LandComparison));
            }

            for (Stage stage : Stage.values()) {
                StageStats stats = stages[stage.ordinal()];
                long calls = stats.calls.sum();
                if (calls == 0L) {
                    continue;
                }

                long totalNanos = stats.totalNanos.sum();
                String units = stage.unitLabel == null ? "" : ", " + stage.unitLabel + "=" + stats.units.sum();
                VoxelConstants.getLogger().info(
                        "[PersistentMap profile #{} report {}] stage={} calls={}, summed={} ms, avg={} ms, max={} ms{}",
                        id,
                        reportNumber,
                        stage.label,
                        calls,
                        formatMillis(totalNanos),
                        formatMillis(totalNanos / calls),
                        formatMillis(stats.maxNanos.get()),
                        units);
            }
        }
    }

    static final class OverviewComparisonMetrics {
        private long waterPixels;
        private long landPixels;
        private long waterAbsoluteError;
        private long landAbsoluteError;
        private int waterSignificantPixels;
        private int landSignificantPixels;
        private int waterMaxError;
        private int landMaxError;

        void record(
                boolean water,
                int referenceRed,
                int referenceGreen,
                int referenceBlue,
                int overviewRed,
                int overviewGreen,
                int overviewBlue) {
            int redError = Math.abs(referenceRed - overviewRed);
            int greenError = Math.abs(referenceGreen - overviewGreen);
            int blueError = Math.abs(referenceBlue - overviewBlue);
            int maxError = Math.max(redError, Math.max(greenError, blueError));
            long absoluteError = (long) redError + greenError + blueError;
            if (water) {
                ++this.waterPixels;
                this.waterAbsoluteError += absoluteError;
                this.waterMaxError = Math.max(this.waterMaxError, maxError);
                if (maxError >= OVERVIEW_COMPARISON_SIGNIFICANT_ERROR) {
                    ++this.waterSignificantPixels;
                }
            } else {
                ++this.landPixels;
                this.landAbsoluteError += absoluteError;
                this.landMaxError = Math.max(this.landMaxError, maxError);
                if (maxError >= OVERVIEW_COMPARISON_SIGNIFICANT_ERROR) {
                    ++this.landSignificantPixels;
                }
            }
        }
    }

    private static final class ComparisonStats {
        private final LongAdder pixels = new LongAdder();
        private final LongAdder absoluteError = new LongAdder();
        private final LongAdder significantPixels = new LongAdder();
        private final AtomicLong maxError = new AtomicLong();

        private void add(long pixels, long absoluteError, long significantPixels, long maxError) {
            this.pixels.add(pixels);
            this.absoluteError.add(absoluteError);
            this.significantPixels.add(significantPixels);
            this.maxError.accumulateAndGet(maxError, Math::max);
        }
    }
}
