package com.mamiyaotaru.voxelmap.interfaces;

import com.mamiyaotaru.voxelmap.util.LayoutVariables;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4fStack;

public interface IRadar {
    void onResourceManagerReload(ResourceManager resourceManager);

    void onTickInGame(GuiGraphics drawContext, Matrix4fStack matrixStack, LayoutVariables layoutVariables);
}
