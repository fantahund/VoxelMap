package com.mamiyaotaru.voxelmap.interfaces;

import com.mamiyaotaru.voxelmap.util.LayoutVariables;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceManager;

public interface IRadar {
    void onResourceManagerReload(ResourceManager var1);

    void onTickInGame(MatrixStack var1, MinecraftClient var2, LayoutVariables var3);
}
