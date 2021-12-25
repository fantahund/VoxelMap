package com.mamiyaotaru.voxelmap.interfaces;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;

public interface IMap extends IChangeObserver {
    void forceFullRender(boolean var1);

    void drawMinimap(MatrixStack var1, MinecraftClient var2);

    float getPercentX();

    float getPercentY();

    void newWorld(ClientWorld var1);

    void onTickInGame(MatrixStack var1, MinecraftClient var2);

    int[] getLightmapArray();

    void newWorldName();
}
