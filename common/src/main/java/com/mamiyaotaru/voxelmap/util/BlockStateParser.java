package com.mamiyaotaru.voxelmap.util;

import com.google.common.collect.BiMap;
import java.util.Optional;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public final class BlockStateParser {
    private BlockStateParser() {}

    public static void parseLine(String line, BiMap<BlockState, Integer> map) {
        String[] lineParts = line.split(" ");

        int id = Integer.parseInt(lineParts[0]);
        BlockState blockState = parseStateString(lineParts[1]);

        if (blockState != null) {
            map.forcePut(blockState, id);
        }
    }

    @Nullable
    private static BlockState parseStateString(String stateString) {
        int bracketIndex = stateString.indexOf('[');
        String resourceString = stateString.substring(0, bracketIndex == -1 ? stateString.length() : bracketIndex);
        int curlyBracketOpenIndex = resourceString.indexOf('{');
        int curlyBracketCloseIndex = resourceString.indexOf('}');
        resourceString = resourceString.substring(curlyBracketOpenIndex == -1 ? 0 : curlyBracketOpenIndex + 1, curlyBracketCloseIndex == -1 ? resourceString.length() : curlyBracketCloseIndex);
        String[] resourceStringParts = resourceString.split(":");
        ResourceLocation identifier = null;

        if (resourceStringParts.length == 1) {
            identifier = new ResourceLocation(resourceStringParts[0]);
        } else if (resourceStringParts.length == 2) {
            identifier = new ResourceLocation(resourceStringParts[0], resourceStringParts[1]);
        }
        Reference<Block> blockRef = BuiltInRegistries.BLOCK.get(identifier).orElse(null);
        if (blockRef == null) {
            return null;
        }
        Block block = blockRef.value();

        if (!(!(block instanceof AirBlock) || resourceString.equals("minecraft:air"))) {
            return null;
        }

        BlockState blockState = block.defaultBlockState();

        if (bracketIndex == -1) {
            return blockState;
        }

        String propertiesString = stateString.substring(stateString.indexOf('[') + 1, stateString.lastIndexOf(']'));
        String[] propertiesStringParts = propertiesString.split(",");

        for (String propertiesStringPart : propertiesStringParts) {
            String[] propertyStringParts = propertiesStringPart.split("=");
            Property<?> property = block.getStateDefinition().getProperty(propertyStringParts[0]);

            if (property != null) {
                blockState = withValue(blockState, property, propertyStringParts[1]);
            }
        }

        return blockState;
    }

    private static <T extends Comparable<T>> BlockState withValue(BlockState state, Property<T> property, String string) {
        Optional<T> value = property.getValue(string);

        return value.isPresent() ? state.setValue(property, value.get()) : state;
    }
}