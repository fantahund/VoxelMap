package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import java.util.ArrayList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public class DimensionManager {
    public final ArrayList<DimensionContainer> dimensions;
    private final ArrayList<ResourceKey<Level>> vanillaWorlds = new ArrayList<>();

    public DimensionManager() {
        this.dimensions = new ArrayList<>();
        this.vanillaWorlds.add(Level.OVERWORLD);
        this.vanillaWorlds.add(Level.NETHER);
        this.vanillaWorlds.add(Level.END);
    }

    public ArrayList<DimensionContainer> getDimensions() {
        return this.dimensions;
    }

    public void populateDimensions(Level world) {
        this.dimensions.clear();
        Registry<DimensionType> dimensionTypeRegistry = VoxelConstants.getMinecraft().getConnection().registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE);

        for (ResourceKey<Level> vanillaWorldKey : this.vanillaWorlds) {
            ResourceKey<DimensionType> typeKey = ResourceKey.create(Registries.DIMENSION_TYPE, vanillaWorldKey.location());
            DimensionType dimensionType = dimensionTypeRegistry.get(typeKey).get().value();
            DimensionContainer dimensionContainer = new DimensionContainer(dimensionType, vanillaWorldKey.location().getPath(), vanillaWorldKey.location());
            this.dimensions.add(dimensionContainer);
        }

        this.sort();
    }

    public void enteredWorld(Level world) {
        ResourceLocation resourceLocation = world.dimension().location();
        DimensionContainer dim = this.getDimensionContainerByResourceLocation(resourceLocation);
        if (dim == null) {
            dim = new DimensionContainer(world.dimensionType(), resourceLocation.getPath(), resourceLocation);
            this.dimensions.add(dim);
            this.sort();
        }

        if (dim.type == null) {
            try {
                dim.type = world.dimensionType();
            } catch (RuntimeException ignored) {}
        }

    }

    private void sort() {
        this.dimensions.sort((dim1, dim2) -> {
            if (dim1.resourceLocation.equals(Level.OVERWORLD.location())) {
                return -1;
            } else if (dim1.resourceLocation.equals(Level.NETHER.location()) && !dim2.resourceLocation.equals(Level.OVERWORLD.location())) {
                return -1;
            } else {
                return dim1.resourceLocation.equals(Level.END.location()) && !dim2.resourceLocation.equals(Level.OVERWORLD.location()) && !dim2.resourceLocation.equals(Level.NETHER.location()) ? -1 : String.CASE_INSENSITIVE_ORDER.compare(dim1.name, dim2.name);
            }
        });
    }

    public DimensionContainer getDimensionContainerByWorld(Level world) {
        ResourceLocation resourceLocation = world.dimension().location();
        DimensionContainer dim = this.getDimensionContainerByResourceLocation(resourceLocation);
        if (dim == null) {
            dim = new DimensionContainer(world.dimensionType(), resourceLocation.getPath(), resourceLocation);
            this.dimensions.add(dim);
            this.sort();
        }

        return dim;
    }

    public DimensionContainer getDimensionContainerByIdentifier(String ident) {
        DimensionContainer dim;
        ResourceLocation resourceLocation = ResourceLocation.parse(ident);
        dim = this.getDimensionContainerByResourceLocation(resourceLocation);
        if (dim == null) {
            dim = new DimensionContainer(null, resourceLocation.getPath(), resourceLocation);
            this.dimensions.add(dim);
            this.sort();
        }

        return dim;
    }

    public DimensionContainer getDimensionContainerByResourceLocation(ResourceLocation resourceLocation) {
        return this.dimensions.stream().filter(dim -> resourceLocation.equals(dim.resourceLocation)).findFirst().orElse(null);
    }
}
