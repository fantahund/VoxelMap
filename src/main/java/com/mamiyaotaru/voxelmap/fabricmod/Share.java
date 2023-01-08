package com.mamiyaotaru.voxelmap.fabricmod;

import com.mamiyaotaru.voxelmap.VoxelConstants;

public final class Share {
    private Share() {}

    public static boolean isOldNorth() { return VoxelConstants.getVoxelMapInstance().getMapOptions().oldNorth; }
}