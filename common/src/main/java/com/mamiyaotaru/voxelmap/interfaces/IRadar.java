package com.mamiyaotaru.voxelmap.interfaces;

import com.mamiyaotaru.voxelmap.util.LayoutVariables;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4fStack;

public interface IRadar {
    void onResourceManagerReload(ResourceManager resourceManager);

    void onTickInGame(Matrix4fStack matrixStack, MultiBufferSource.BufferSource bufferSource, LayoutVariables layoutVariables, float scaleProj);
}
