package com.mamiyaotaru.voxelmap.persistent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class OverviewLightingSchedulerTest {
    @Test
    void checksEachRegionOnlyOnceWhileUpdatesWaitForTheirBudget() {
        OverviewLightingScheduler scheduler = new OverviewLightingScheduler(4, 1);
        FakeRegion[] fakeRegions = createRegions(10);
        CachedRegion[] regions = fakeRegions;
        PersistentMap.LightmapSnapshot lightmap = lightmap(1L);

        scheduler.process(regions, lightmap);
        assertEquals(4, Arrays.stream(fakeRegions).mapToInt(region -> region.checks).sum());
        assertEquals(1, Arrays.stream(fakeRegions).mapToInt(region -> region.updates).sum());
        assertEquals(3, scheduler.pendingUpdateCount());

        for (int frame = 1; frame < 10; ++frame) {
            scheduler.process(regions, lightmap);
        }

        assertEquals(10, Arrays.stream(fakeRegions).mapToInt(region -> region.checks).sum());
        assertEquals(10, Arrays.stream(fakeRegions).mapToInt(region -> region.updates).sum());
        assertEquals(0, Arrays.stream(fakeRegions).mapToInt(region -> region.repeatedPendingAttempts).sum());
        assertEquals(0, scheduler.pendingUpdateCount());
    }

    @Test
    void queuedUpdateUsesLatestRevisionWithoutRepeatingThresholdCheck() {
        OverviewLightingScheduler scheduler = new OverviewLightingScheduler(2, 1);
        FakeRegion[] regions = createRegions(2);

        scheduler.process(regions, lightmap(1L));
        assertEquals(1, scheduler.pendingUpdateCount());

        scheduler.process(regions, lightmap(2L));

        assertEquals(1, regions[1].checks);
        assertEquals(1, regions[1].updates);
        assertEquals(2L, regions[1].appliedRevision);
    }

    @Test
    void updateThatCannotCompleteIsRetriedOnlyOnNextFrame() {
        OverviewLightingScheduler scheduler = new OverviewLightingScheduler(1, 1);
        AlwaysPendingRegion region = new AlwaysPendingRegion();
        CachedRegion[] regions = {region};

        scheduler.process(regions, lightmap(1L));
        scheduler.process(regions, lightmap(1L));

        assertEquals(2, region.attempts);
        assertEquals(1, scheduler.pendingUpdateCount());
    }

    private static FakeRegion[] createRegions(int count) {
        FakeRegion[] regions = new FakeRegion[count];
        for (int index = 0; index < count; ++index) {
            regions[index] = new FakeRegion();
        }
        return regions;
    }

    private static PersistentMap.LightmapSnapshot lightmap(long revision) {
        return new PersistentMap.LightmapSnapshot(new int[256], revision);
    }

    private static final class FakeRegion extends CachedRegion {
        private long evaluatedRevision;
        private boolean updatePending;
        private int checks;
        private int updates;
        private int repeatedPendingAttempts;
        private long appliedRevision;

        @Override
        OverviewLightingUpdateResult updateOverviewLighting(PersistentMap.LightmapSnapshot lightmap, boolean allowUpdate) {
            if (this.updatePending) {
                if (!allowUpdate) {
                    ++this.repeatedPendingAttempts;
                    return new OverviewLightingUpdateResult(false, false, true);
                }
                this.updatePending = false;
                ++this.updates;
                this.evaluatedRevision = lightmap.revision();
                this.appliedRevision = lightmap.revision();
                return new OverviewLightingUpdateResult(false, true, false);
            }
            if (this.evaluatedRevision == lightmap.revision()) {
                return new OverviewLightingUpdateResult(false, false, false);
            }

            ++this.checks;
            this.evaluatedRevision = lightmap.revision();
            if (!allowUpdate) {
                this.updatePending = true;
                return new OverviewLightingUpdateResult(true, false, true);
            }
            ++this.updates;
            this.appliedRevision = lightmap.revision();
            return new OverviewLightingUpdateResult(true, true, false);
        }
    }

    private static final class AlwaysPendingRegion extends CachedRegion {
        private int attempts;

        @Override
        OverviewLightingUpdateResult updateOverviewLighting(PersistentMap.LightmapSnapshot lightmap, boolean allowUpdate) {
            ++this.attempts;
            return new OverviewLightingUpdateResult(false, false, true);
        }
    }
}
