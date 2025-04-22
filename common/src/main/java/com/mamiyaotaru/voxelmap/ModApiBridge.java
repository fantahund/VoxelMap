package com.mamiyaotaru.voxelmap;

public interface ModApiBridge {
    default boolean isModEnabled(String modID) {
        return false;
    }
}