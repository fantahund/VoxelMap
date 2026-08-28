package com.mamiyaotaru.voxelmap.entityrender;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RadarIconFallbackTest {
    @Test
    void transparentCandidatesAdvanceUntilRootAndWarnOnlyOnce() {
        RadarIconFallback.Decision first = RadarIconFallback.decide(false, 0, 3);
        RadarIconFallback.Decision second = RadarIconFallback.decide(false, 1, 3);
        RadarIconFallback.Decision root = RadarIconFallback.decide(false, 2, 3);
        Set<String> warned = new HashSet<>();

        assertTrue(first == RadarIconFallback.Decision.TRY_NEXT);
        assertTrue(second == RadarIconFallback.Decision.TRY_NEXT);
        assertTrue(root == RadarIconFallback.Decision.ACCEPT_EMPTY);
        assertTrue(RadarIconFallback.shouldWarn(root, warned, "hoglin"));
        assertFalse(RadarIconFallback.shouldWarn(root, warned, "hoglin"));
    }

    @Test
    void visibleCandidateIsAcceptedWithoutWarning() {
        RadarIconFallback.Decision decision = RadarIconFallback.decide(true, 0, 3);

        assertTrue(decision == RadarIconFallback.Decision.ACCEPT);
        assertFalse(RadarIconFallback.shouldWarn(decision, new HashSet<>(), "cat"));
    }
}
