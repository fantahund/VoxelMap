package com.mamiyaotaru.voxelmap.gui.settings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mamiyaotaru.voxelmap.util.VoxelMapMobCategory;
import java.util.HashSet;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class EntityTypeSelectionTest {
    @Test
    void includesHostileAndNonHostileTypesAccordingToTheirParentToggles() {
        assertTrue(EntityTypeSelection.includes(VoxelMapMobCategory.HOSTILE, true, true, false));
        assertFalse(EntityTypeSelection.includes(VoxelMapMobCategory.HOSTILE, true, false, true));
        assertTrue(EntityTypeSelection.includes(VoxelMapMobCategory.NEUTRAL, true, false, true));
        assertFalse(EntityTypeSelection.includes(VoxelMapMobCategory.NEUTRAL, true, true, false));
    }

    @Test
    void excludesPlayersAndNonLivingEntityTypes() {
        assertFalse(EntityTypeSelection.includes(VoxelMapMobCategory.PLAYER, true, true, true));
        assertFalse(EntityTypeSelection.includes(VoxelMapMobCategory.HOSTILE, false, true, true));
        assertFalse(EntityTypeSelection.includes(VoxelMapMobCategory.NEUTRAL, false, true, true));
    }

    @Test
    void searchesLocalizedNameAndRegistryIdCaseInsensitively() {
        assertTrue(EntityTypeSelection.matches("Zombie Piglin", "minecraft:zombified_piglin", "PIGLIN"));
        assertTrue(EntityTypeSelection.matches("Zombie Piglin", "minecraft:zombified_piglin", "zombified_"));
        assertFalse(EntityTypeSelection.matches("Zombie Piglin", "minecraft:zombified_piglin", "creeper"));
    }

    @Test
    void togglingVisibilityAddsAndRemovesTheRegistryId() {
        HashSet<Identifier> hiddenMobs = new HashSet<>();
        Identifier id = Identifier.parse("minecraft:cow");

        assertFalse(EntityTypeSelection.toggleHidden(hiddenMobs, id));
        assertTrue(hiddenMobs.contains(id));
        assertTrue(EntityTypeSelection.toggleHidden(hiddenMobs, id));
        assertFalse(hiddenMobs.contains(id));
    }
}
