package com.mamiyaotaru.voxelmap.util;

import com.google.common.collect.BiMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class BlockStateParser {
    private BlockStateParser() {}

    public static void parseLine(String line, BiMap<BlockState, Integer> map) {
        String[] lineParts = line.split(" ");

        int id = Integer.parseInt(lineParts[0]);
        BlockState blockState = parseStateString(lineParts[1]);

        if (blockState != null) map.forcePut(blockState, id);
    }

    private static BlockState parseStateString(String stateString) {
        int bracketIndex = stateString.indexOf('[');
        String resourceString = stateString.substring(0, bracketIndex == -1 ? stateString.length() : bracketIndex);
        int curlyBracketOpenIndex = resourceString.indexOf('{');
        int curlyBracketCloseIndex = resourceString.indexOf('}');
        resourceString = resourceString.substring(curlyBracketOpenIndex == -1 ? 0 : curlyBracketOpenIndex + 1, curlyBracketCloseIndex == -1 ? resourceString.length() : curlyBracketCloseIndex);
        String[] resourceStringParts = resourceString.split(":");
        Identifier resourceLocation = null;

        if (resourceStringParts.length == 1) resourceLocation = new Identifier(resourceStringParts[0]);
        else if (resourceStringParts.length == 2) resourceLocation = new Identifier(resourceStringParts[0], resourceStringParts[1]);

        Block block = Registries.BLOCK.get(resourceLocation);

        if (!(block != Blocks.AIR || resourceString.equals("minecraft:air"))) return null;

        BlockState blockState = block.getDefaultState();

        if (bracketIndex == -1) return blockState;

        String propertiesString = stateString.substring(stateString.indexOf('[') + 1, stateString.lastIndexOf(']'));
        String[] propertiesStringParts = propertiesString.split(",");

        for (String propertiesStringPart : propertiesStringParts) {
            String[] propertyStringParts = propertiesStringPart.split("=");
            Property<?> property = block.getStateManager().getProperty(propertyStringParts[0]);

            if (property != null) blockState = withValue(blockState, property, propertyStringParts[1]);
        }

        return blockState;
    }

    private static <T extends Comparable<T>> BlockState withValue(BlockState state, Property<T> property, String string) {
        Optional<T> value = property.parse(string);

        return value.isPresent() ? state.with(property, value.get()) : state;
    }
}