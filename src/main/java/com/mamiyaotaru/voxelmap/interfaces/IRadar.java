package com.mamiyaotaru.voxelmap.interfaces;

import com.mamiyaotaru.voxelmap.util.LayoutVariables;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceManager;

public interface IRadar {
    void onResourceManagerReload(ResourceManager resourceManager);

    void onTickInGame(DrawContext drawContext, MatrixStack matrixStack, LayoutVariables layoutVariables);
}
