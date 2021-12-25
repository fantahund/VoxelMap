package com.mamiyaotaru.voxelmap.fabricmod;

import com.mamiyaotaru.voxelmap.interfaces.AbstractVoxelMap;

public class Share {
    public static boolean isOldNorth() {
        return AbstractVoxelMap.getInstance().getMapOptions().oldNorth;
    }
}
