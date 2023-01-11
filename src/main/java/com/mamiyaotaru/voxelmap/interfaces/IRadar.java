package com.mamiyaotaru.voxelmap.interfaces;

import com.mamiyaotaru.voxelmap.util.LayoutVariables;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceManager;

public interface IRadar {
    void onResourceManagerReload(ResourceManager resourceManager);

    void onTickInGame(MatrixStack matrixStack, LayoutVariables layoutVariables);
}
