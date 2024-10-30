package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.Events;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class FabricEvents implements Events {
    FabricEvents() {
    }

    public void initEvents(VoxelMap map) {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> map.onDisconnect());
        ClientConfigurationConnectionEvents.INIT.register((handler, client) -> map.onConfigurationInit());
        ClientPlayConnectionEvents.INIT.register((handler, client) -> map.onPlayInit());
        ClientLifecycleEvents.CLIENT_STOPPING.register((client) -> map.onClientStopping());
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> VoxelConstants.renderOverlay(drawContext));
    }
}
