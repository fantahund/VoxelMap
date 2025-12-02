package com.mamiyaotaru.voxelmap.util;

import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.dimension.DimensionType;

public class DimensionContainer implements Comparable<DimensionContainer> {
    public DimensionType type;
    public final String name;
    public final Identifier Identifier;

    public DimensionContainer(DimensionType type, String name, Identifier Identifier) {
        this.type = type;
        this.name = name;
        this.Identifier = Identifier;
    }

    public String getStorageName() {
        if (Identifier == null) {
            return "UNKNOWN";
        }
        return Identifier.getNamespace().equals("minecraft") ? Identifier.getPath() : Identifier.toString();
    }

    public String getDisplayName() { return TextUtils.prettify(this.name); }

    @Override
    public int compareTo(DimensionContainer o) { return String.CASE_INSENSITIVE_ORDER.compare(this.name, o.name); }

    @Override
    public boolean equals(Object obj) { return obj instanceof DimensionContainer container && compareTo(container) == 0; }

    @Override
    public int hashCode() { return Objects.hash(name, Identifier); }
}