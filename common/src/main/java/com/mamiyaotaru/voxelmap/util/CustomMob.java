package com.mamiyaotaru.voxelmap.util;

public class CustomMob {
    public final String id;
    public boolean enabled = true;
    public boolean isHostile;
    public boolean isNeutral;

    public CustomMob(String id, boolean enabled) {
        this.id = id;
        this.enabled = enabled;
    }

    public CustomMob(String id, boolean isHostile, boolean isNeutral) {
        this.id = id;
        this.isHostile = isHostile;
        this.isNeutral = isNeutral;
    }
}