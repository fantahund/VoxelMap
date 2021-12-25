package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.interfaces.IDimensionManager;
import com.mamiyaotaru.voxelmap.interfaces.IVoxelMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;

public class DimensionManager implements IDimensionManager {
    IVoxelMap master;
    public ArrayList<DimensionContainer> dimensions;
    private ArrayList<RegistryKey> vanillaWorlds = new ArrayList();

    public DimensionManager(IVoxelMap master) {
        this.master = master;
        this.dimensions = new ArrayList();
        this.vanillaWorlds.add(World.OVERWORLD);
        this.vanillaWorlds.add(World.NETHER);
        this.vanillaWorlds.add(World.END);
    }

    @Override
    public ArrayList getDimensions() {
        return this.dimensions;
    }

    @Override
    public void populateDimensions(World world) {
        this.dimensions.clear();
        Registry dimensionTypeRegistry = MinecraftClient.getInstance().getNetworkHandler().getRegistryManager().get(Registry.DIMENSION_TYPE_KEY);

        for (RegistryKey vanillaWorldKey : this.vanillaWorlds) {
            RegistryKey typeKey = RegistryKey.of(Registry.DIMENSION_TYPE_KEY, vanillaWorldKey.getValue());
            DimensionType dimensionType = (DimensionType) dimensionTypeRegistry.get(typeKey);
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
            } catch (Exception var5) {
            }
        }

    }

    private void sort() {
        final Collator collator = I18nUtils.getLocaleAwareCollator();
        Collections.sort(this.dimensions, (dim1, dim2) -> {
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
        DimensionContainer dim = null;
        Identifier resourceLocation = new Identifier(ident);
        dim = this.getDimensionContainerByResourceLocation(resourceLocation);
        if (dim == null) {
            dim = new DimensionContainer((DimensionType) null, resourceLocation.getPath(), resourceLocation);
            this.dimensions.add(dim);
            this.sort();
        }

        return dim;
    }

    @Override
    public DimensionContainer getDimensionContainerByResourceLocation(Identifier resourceLocation) {
        for (DimensionContainer dim : this.dimensions) {
            if (resourceLocation.equals(dim.resourceLocation)) {
                return dim;
            }
        }

        return null;
    }
}
