package com.mamiyaotaru.voxelmap.gui.settings;

import com.mamiyaotaru.voxelmap.util.VoxelMapMobCategory;
import java.util.Locale;
import java.util.Set;
import net.minecraft.resources.Identifier;

public final class EntityTypeSelection {
    private EntityTypeSelection() {
    }

    public static boolean includes(VoxelMapMobCategory category, boolean living, boolean hostilesEnabled, boolean nonHostilesEnabled) {
        if (!living || category == VoxelMapMobCategory.PLAYER)
            return false;
        return category == VoxelMapMobCategory.HOSTILE ? hostilesEnabled : nonHostilesEnabled;
    }

    public static boolean matches(String localizedName, String registryId, String query) {
        String normalizedQuery = query.toLowerCase(Locale.ROOT).trim();
        return normalizedQuery.isEmpty()
                || localizedName.toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || registryId.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    public static boolean toggleHidden(Set<Identifier> hiddenMobs, Identifier id) {
        if (hiddenMobs.remove(id))
            return true;
        hiddenMobs.add(id);
        return false;
    }
}
