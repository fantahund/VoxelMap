package com.mamiyaotaru.voxelmap.entityrender;

import java.util.Set;

final class RadarIconFallback {
    private RadarIconFallback() {
    }

    static Decision decide(boolean hasVisiblePixel, int attemptIndex, int attemptCount) {
        if (hasVisiblePixel) {
            return Decision.ACCEPT;
        }
        return attemptIndex + 1 < attemptCount ? Decision.TRY_NEXT : Decision.ACCEPT_EMPTY;
    }

    static <T> boolean shouldWarn(Decision decision, Set<T> warnedKeys, T key) {
        return decision == Decision.ACCEPT_EMPTY && warnedKeys.add(key);
    }

    enum Decision {
        ACCEPT,
        TRY_NEXT,
        ACCEPT_EMPTY
    }
}
