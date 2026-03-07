package com.mamiyaotaru.voxelmap.util;

import net.minecraft.core.Direction;

import java.util.LinkedHashMap;

public class PropertyParser {
    public static LinkedHashMap<Direction.Axis, Float> parseVector(String property) {
        if (!property.startsWith("{") || !property.endsWith("}")) return null;

        LinkedHashMap<Direction.Axis, Float> vector = new LinkedHashMap<>();
        for (String entry : property.substring(1, property.length() - 1).split(",")) {
            String[] kv = entry.split(":", 2);
            if (kv.length < 2) continue;

            Direction.Axis axis = Direction.Axis.byName(kv[0].trim().toLowerCase());
            if (axis != null) {
                vector.put(axis, Float.parseFloat(kv[1].trim()));
            }
        }

        return vector.isEmpty() ? null : vector;
    }
}
