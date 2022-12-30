package com.mamiyaotaru.voxelmap.fabricmod;

import com.mamiyaotaru.voxelmap.VoxelMap;

public class Share {
    public static boolean isOldNorth() {
        return VoxelMap.getInstance().getMapOptions().oldNorth;
    }
}
