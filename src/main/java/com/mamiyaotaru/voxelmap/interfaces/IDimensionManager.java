package com.mamiyaotaru.voxelmap.interfaces;

import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.ArrayList;

public interface IDimensionManager {
    ArrayList<DimensionContainer> getDimensions();

    DimensionContainer getDimensionContainerByWorld(World var1);

    DimensionContainer getDimensionContainerByIdentifier(String var1);

    void enteredWorld(World var1);

    void populateDimensions(World var1);

    DimensionContainer getDimensionContainerByResourceLocation(Identifier var1);
}
