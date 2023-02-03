package com.mamiyaotaru.voxelmap.util;

import net.minecraft.util.Identifier;
import net.minecraft.world.dimension.DimensionType;

import java.util.Objects;

public class DimensionContainer implements Comparable<DimensionContainer> {
    public DimensionType type;
    public final String name;
    public final Identifier resourceLocation;

    public DimensionContainer(DimensionType type, String name, Identifier resourceLocation) {
        this.type = type;
        this.name = name;
        this.resourceLocation = resourceLocation;
    }

    public String getStorageName() {
        if (resourceLocation == null) return "UNKNOWN";
        return resourceLocation.getNamespace().equals("minecraft") ? resourceLocation.getPath() : resourceLocation.toString();
    }

    public String getDisplayName() { return TextUtils.prettify(this.name); }

    public int compareTo(DimensionContainer o) { return String.CASE_INSENSITIVE_ORDER.compare(this.name, o.name); }

    @Override
    public boolean equals(Object obj) { return obj instanceof DimensionContainer container && compareTo(container) == 0; }

    @Override
    public int hashCode() { return Objects.hash(name, resourceLocation); }
}