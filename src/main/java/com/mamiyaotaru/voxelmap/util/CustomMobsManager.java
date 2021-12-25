package com.mamiyaotaru.voxelmap.util;

import java.util.ArrayList;
import java.util.List;

public class CustomMobsManager {
    public static List<CustomMob> mobs = new ArrayList();

    public static void add(String type, boolean enabled) {
        CustomMob mob = getCustomMobByType(type);
        if (mob != null) {
            mob.enabled = enabled;
        } else {
            mobs.add(new CustomMob(type, enabled));
        }

    }

    public static void add(String type, boolean isHostile, boolean isNeutral) {
        CustomMob mob = getCustomMobByType(type);
        if (mob != null) {
            mob.isHostile = isHostile;
            mob.isNeutral = isNeutral;
        } else {
            mobs.add(new CustomMob(type, isHostile, isNeutral));
        }

    }

    public static CustomMob getCustomMobByType(String type) {
        for (CustomMob mob : mobs) {
            if (mob.id.equals(type)) {
                return mob;
            }
        }

        return null;
    }
}
