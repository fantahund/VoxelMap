package com.mamiyaotaru.voxelmap.util;

import java.util.concurrent.ArrayBlockingQueue;

public class MutableBlockPosCache {
    private static final ArrayBlockingQueue<MutableBlockPos> unused = new ArrayBlockingQueue<>(20);

    public static MutableBlockPos get() {
        MutableBlockPos pos = unused.poll();
        if (pos == null) {
            pos = new MutableBlockPos(0, 0, 0);
        }
        return pos;
    }

    public static void release(MutableBlockPos pos) {
        unused.offer(pos);
    }
}
