package com.mamiyaotaru.voxelmap.util;

import net.minecraft.util.Identifier;
import net.minecraft.world.dimension.DimensionType;

import java.text.Collator;

public class DimensionContainer implements Comparable<DimensionContainer> {
    public DimensionType type;
    public String name = "notLoaded";
    public Identifier resourceLocation;
    private static final Collator collator = I18nUtils.getLocaleAwareCollator();

    public DimensionContainer(DimensionType type, String name, Identifier resourceLocation) {
        this.type = type;
        this.name = name;
        this.resourceLocation = resourceLocation;
    }

    public String getStorageName() {
        String storageName = null;
        if (this.resourceLocation != null) {
            if (this.resourceLocation.getNamespace().equals("minecraft")) {
                storageName = this.resourceLocation.getPath();
            } else {
                storageName = this.resourceLocation.toString();
            }
        } else {
            storageName = "UNKNOWN";
        }

        return storageName;
    }

    public String getDisplayName() {
        return TextUtils.prettify(this.name);
    }

    public int compareTo(DimensionContainer other) {
        return collator.compare(this.name, other.name);
    }
}
