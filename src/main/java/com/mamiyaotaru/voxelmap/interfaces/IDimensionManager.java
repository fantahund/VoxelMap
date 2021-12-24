package com.mamiyaotaru.voxelmap.interfaces;

import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import java.util.ArrayList;
import net.minecraft.world.World;
import net.minecraft.util.Identifier;

public interface IDimensionManager {
   ArrayList<DimensionContainer> getDimensions();

   DimensionContainer getDimensionContainerByWorld(World var1);

   DimensionContainer getDimensionContainerByIdentifier(String var1);

   void enteredWorld(World var1);

   void populateDimensions(World var1);

   DimensionContainer getDimensionContainerByResourceLocation(Identifier var1);
}
