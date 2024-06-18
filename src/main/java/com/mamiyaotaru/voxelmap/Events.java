package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.fabricmod.FabricModVoxelMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class Events {
    private Events() {
    }

    public static void initEvents(VoxelMap map) {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> map.clearServerSettings());
        ClientConfigurationConnectionEvents.INIT.register((handler, client) -> map.clearServerSettings());
        ClientPlayConnectionEvents.INIT.register((handler, client) -> map.initBeforeWorld());
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> FabricModVoxelMap.instance.renderOverlay(drawContext));
    }
}
