package com.mamiyaotaru.voxelmap.persistent;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

final class OverviewLightingScheduler {
    static final int DEFAULT_CHECKS_PER_FRAME = 16;
    static final int DEFAULT_UPDATES_PER_FRAME = 8;

    private final int checksPerFrame;
    private final int updatesPerFrame;
    private final ArrayDeque<CachedRegion> pendingUpdates = new ArrayDeque<>();
    private final Set<CachedRegion> pendingUpdateSet = Collections.newSetFromMap(new IdentityHashMap<>());
    private CachedRegion[] visibleRegions = new CachedRegion[0];
    private int scanCursor;

    OverviewLightingScheduler() {
        this(DEFAULT_CHECKS_PER_FRAME, DEFAULT_UPDATES_PER_FRAME);
    }

    OverviewLightingScheduler(int checksPerFrame, int updatesPerFrame) {
        if (checksPerFrame < 1 || updatesPerFrame < 1 || updatesPerFrame > checksPerFrame) {
            throw new IllegalArgumentException("Invalid overview lighting frame budgets");
        }
        this.checksPerFrame = checksPerFrame;
        this.updatesPerFrame = updatesPerFrame;
    }

    void process(CachedRegion[] regions, PersistentMap.LightmapSnapshot lightmap) {
        if (regions.length == 0) {
            this.reset(regions);
            return;
        }
        if (regions != this.visibleRegions) {
            this.reset(regions);
        }

        int updatesRemaining = this.updatesPerFrame;
        int queuedUpdatesToProcess = this.pendingUpdates.size();
        while (updatesRemaining > 0 && queuedUpdatesToProcess-- > 0 && !this.pendingUpdates.isEmpty()) {
            CachedRegion region = this.pendingUpdates.removeFirst();
            this.pendingUpdateSet.remove(region);
            CachedRegion.OverviewLightingUpdateResult result = region.updateOverviewLighting(lightmap, true);
            if (result.updated()) {
                --updatesRemaining;
            }
            if (result.updatePending()) {
                this.enqueue(region);
            }
        }

        int checksRemaining = this.checksPerFrame;
        int visited = 0;
        while (visited < regions.length && checksRemaining > 0) {
            CachedRegion region = regions[this.scanCursor];
            this.scanCursor = (this.scanCursor + 1) % regions.length;
            ++visited;
            if (this.pendingUpdateSet.contains(region)) {
                continue;
            }

            CachedRegion.OverviewLightingUpdateResult result = region.updateOverviewLighting(lightmap, updatesRemaining > 0);
            if (result.thresholdChecked()) {
                --checksRemaining;
            }
            if (result.updated()) {
                --updatesRemaining;
            }
            if (result.updatePending()) {
                this.enqueue(region);
            }
        }
    }

    void clear() {
        this.reset(new CachedRegion[0]);
    }

    int pendingUpdateCount() {
        return this.pendingUpdates.size();
    }

    private void enqueue(CachedRegion region) {
        if (this.pendingUpdateSet.add(region)) {
            this.pendingUpdates.addLast(region);
        }
    }

    private void reset(CachedRegion[] regions) {
        this.pendingUpdates.clear();
        this.pendingUpdateSet.clear();
        this.visibleRegions = regions;
        this.scanCursor = 0;
    }
}
