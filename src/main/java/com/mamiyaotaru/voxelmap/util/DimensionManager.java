package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.interfaces.IDimensionManager;
import com.mamiyaotaru.voxelmap.interfaces.IVoxelMap;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.text.Collator;
import java.util.ArrayList;

public class DimensionManager implements IDimensionManager {
    final IVoxelMap master;
    public final ArrayList<DimensionContainer> dimensions;
    private final ArrayList<RegistryKey<World>> vanillaWorlds = new ArrayList<>();

    public DimensionManager(IVoxelMap master) {
        this.master = master;
        this.dimensions = new ArrayList<>();
        this.vanillaWorlds.add(World.OVERWORLD);
        this.vanillaWorlds.add(World.NETHER);
        this.vanillaWorlds.add(World.END);
    }

    @Override
    public ArrayList<DimensionContainer> getDimensions() {
        return this.dimensions;
    }

    @Override
    public void populateDimensions(World world) {
        this.dimensions.clear();
        Registry<DimensionType> dimensionTypeRegistry = VoxelConstants.getMinecraft().getNetworkHandler().getRegistryManager().get(Registry.DIMENSION_TYPE_KEY);

        for (RegistryKey<World> vanillaWorldKey : this.vanillaWorlds) {
            RegistryKey<DimensionType> typeKey = RegistryKey.of(Registry.DIMENSION_TYPE_KEY, vanillaWorldKey.getValue());
            DimensionType dimensionType = dimensionTypeRegistry.get(typeKey);
            DimensionContainer dimensionContainer = new DimensionContainer(dimensionType, vanillaWorldKey.getValue().getPath(), vanillaWorldKey.getValue());
            this.dimensions.add(dimensionContainer);
        }

        this.sort();
    }

    @Override
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
        final Collator collator = I18nUtils.getLocaleAwareCollator();
        this.dimensions.sort((dim1, dim2) -> {
            if (dim1.resourceLocation.equals(World.OVERWORLD.getValue())) {
                return -1;
            } else if (dim1.resourceLocation.equals(World.NETHER.getValue()) && !dim2.resourceLocation.equals(World.OVERWORLD.getValue())) {
                return -1;
            } else {
                return dim1.resourceLocation.equals(World.END.getValue()) && !dim2.resourceLocation.equals(World.OVERWORLD.getValue()) && !dim2.resourceLocation.equals(World.NETHER.getValue()) ? -1 : collator.compare(dim1.name, dim2.name);
            }
        });
    }

    @Override
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

    @Override
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

    @Override
    public DimensionContainer getDimensionContainerByResourceLocation(Identifier resourceLocation) {
        return this.dimensions.stream().filter(dim -> resourceLocation.equals(dim.resourceLocation)).findFirst().orElse(null);
    }
}
