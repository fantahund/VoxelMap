package com.mamiyaotaru.voxelmap.interfaces;

import com.mamiyaotaru.voxelmap.util.LayoutVariables;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.resource.ResourceManager;
import org.joml.Matrix4fStack;

public interface IRadar {
    void onResourceManagerReload(ResourceManager resourceManager);

    void onTickInGame(DrawContext drawContext, Matrix4fStack matrixStack, LayoutVariables layoutVariables);
}
