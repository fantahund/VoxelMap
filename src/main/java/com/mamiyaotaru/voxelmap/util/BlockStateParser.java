package com.mamiyaotaru.voxelmap.util;

import com.google.common.collect.BiMap;
import java.util.Optional;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.registry.Registry;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;

public class BlockStateParser {
   public static void parseLine(String line, BiMap map) {
      String[] lineParts = line.split(" ");
      int id = Integer.parseInt(lineParts[0]);
      BlockState blockState = parseStateString(lineParts[1]);
      if (blockState != null) {
         map.forcePut(blockState, id);
      }

   }

   private static BlockState parseStateString(String stateString) {
      BlockState blockState = null;
      int bracketIndex = stateString.indexOf("[");
      String resourceString = stateString.substring(0, bracketIndex == -1 ? stateString.length() : bracketIndex);
      int curlyBracketOpenIndex = resourceString.indexOf("{");
      int curlyBracketCloseIndex = resourceString.indexOf("}");
      resourceString = resourceString.substring(
         curlyBracketOpenIndex == -1 ? 0 : curlyBracketOpenIndex + 1, curlyBracketCloseIndex == -1 ? resourceString.length() : curlyBracketCloseIndex
      );
      String[] resourceStringParts = resourceString.split(":");
      Identifier resourceLocation = null;
      if (resourceStringParts.length == 1) {
         resourceLocation = new Identifier(resourceStringParts[0]);
      } else if (resourceStringParts.length == 2) {
         resourceLocation = new Identifier(resourceStringParts[0], resourceStringParts[1]);
      }

      Block block = (Block)Registry.BLOCK.get(resourceLocation);
      if (block != Blocks.AIR || resourceString.equals("minecraft:air")) {
         blockState = block.getDefaultState();
         if (bracketIndex != -1) {
            String propertiesString = stateString.substring(stateString.indexOf("[") + 1, stateString.lastIndexOf("]"));
            String[] propertiesStringParts = propertiesString.split(",");

            for(int t = 0; t < propertiesStringParts.length; ++t) {
               String[] propertyStringParts = propertiesStringParts[t].split("=");
               Property property = block.getStateManager().getProperty(propertyStringParts[0]);
               if (property != null) {
                  blockState = withValue(blockState, property, propertyStringParts[1]);
               }
            }
         }
      }

      return blockState;
   }

   private static BlockState withValue(BlockState blockState, Property property, String valueString) {
      Optional value = property.parse(valueString);
      if (value.isPresent()) {
         blockState = (BlockState)blockState.with(property, (Comparable)value.get());
      }

      return blockState;
   }
}
