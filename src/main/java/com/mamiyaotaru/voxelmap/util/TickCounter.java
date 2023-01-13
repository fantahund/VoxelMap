package com.mamiyaotaru.voxelmap.util;

public class TickCounter {
    public static int tickCounter;

    public static void onTick() {
        tickCounter = tickCounter == Integer.MAX_VALUE ? 1 : tickCounter + 1;
    }
}
