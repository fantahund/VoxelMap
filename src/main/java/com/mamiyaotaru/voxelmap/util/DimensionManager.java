package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.text.Collator;
import java.util.ArrayList;

public class DimensionManager {
    public final ArrayList<DimensionContainer> dimensions;
    private final ArrayList<RegistryKey<World>> vanillaWorlds = new ArrayList<>();

    public DimensionManager() {
        this.dimensions = new ArrayList<>();
        this.vanillaWorlds.add(World.OVERWORLD);
        this.vanillaWorlds.add(World.NETHER);
        this.vanillaWorlds.add(World.END);
    }

    public ArrayList<DimensionContainer> getDimensions() {
        return this.dimensions;
    }

    public void populateDimensions(World world) {
        this.dimensions.clear();
        Registry<DimensionType> dimensionTypeRegistry = VoxelConstants.getMinecraft().getNetworkHandler().getRegistryManager().get(RegistryKeys.DIMENSION_TYPE);

        for (RegistryKey<World> vanillaWorldKey : this.vanillaWorlds) {
            RegistryKey<DimensionType> typeKey = RegistryKey.of(RegistryKeys.DIMENSION_TYPE, vanillaWorldKey.getValue());
            DimensionType dimensionType = dimensionTypeRegistry.get(typeKey);
            DimensionContainer dimensionContainer = new DimensionContainer(dimensionType, vanillaWorldKey.getValue().getPath(), vanillaWorldKey.getValue());
            this.dimensions.add(dimensionContainer);
        }

        this.sort();
    }

    public void enteredWorld(World world) {
        Identifier resourceLocation = world.getRegistryKey().getValue();
        DimensionContainer dim = this.getDimensionContainerByResourceLocation(resourceLocation);
        if (dim == null) {
            dim = new DimensionContainer(world.getDimension(), resourceLocation.getPath(), resourceLocation);
            this.dimensions.add(dim);
            this.sort();
        }

        if (dim.type == null) {
            try {
                dim.type = world.getDimension();
            } catch (Exception ignored) {}
        }

    }

    private void sort() {
        this.dimensions.sort((dim1, dim2) -> {
            if (dim1.resourceLocation.equals(World.OVERWORLD.getValue())) {
                return -1;
            } else if (dim1.resourceLocation.equals(World.NETHER.getValue()) && !dim2.resourceLocation.equals(World.OVERWORLD.getValue())) {
                return -1;
            } else {
                return dim1.resourceLocation.equals(World.END.getValue()) && !dim2.resourceLocation.equals(World.OVERWORLD.getValue()) && !dim2.resourceLocation.equals(World.NETHER.getValue()) ? -1 : String.CASE_INSENSITIVE_ORDER.compare(dim1.name, dim2.name);
            }
        });
    }

    public DimensionContainer getDimensionContainerByWorld(World world) {
        Identifier resourceLocation = world.getRegistryKey().getValue();
        DimensionContainer dim = this.getDimensionContainerByResourceLocation(resourceLocation);
        if (dim == null) {
            dim = new DimensionContainer(world.getDimension(), resourceLocation.getPath(), resourceLocation);
            this.dimensions.add(dim);
            this.sort();
        }

        return dim;
    }

    public DimensionContainer getDimensionContainerByIdentifier(String ident) {
        DimensionContainer dim;
        Identifier resourceLocation = new Identifier(ident);
        dim = this.getDimensionContainerByResourceLocation(resourceLocation);
        if (dim == null) {
            dim = new DimensionContainer(null, resourceLocation.getPath(), resourceLocation);
            this.dimensions.add(dim);
            this.sort();
        }

        return dim;
    }

    public DimensionContainer getDimensionContainerByResourceLocation(Identifier resourceLocation) {
        return this.dimensions.stream().filter(dim -> resourceLocation.equals(dim.resourceLocation)).findFirst().orElse(null);
    }
}
