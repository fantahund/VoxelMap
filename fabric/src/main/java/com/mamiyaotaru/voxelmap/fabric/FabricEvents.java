package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.Events;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class FabricEvents implements Events {
    FabricEvents() {
        ResourceLocation voxelMapMinimapLayer = ResourceLocation.parse("voxelmap:minimap");
        HudLayerRegistrationCallback.EVENT.register(layeredDrawer -> {
            layeredDrawer.attachLayerAfter(IdentifiedLayer.EXPERIENCE_LEVEL, new IdentifiedLayer() {
                @Override
                public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
                    VoxelConstants.renderOverlay(guiGraphics);
                }

                @Override
                public ResourceLocation id() {
                    return voxelMapMinimapLayer;
                }
            });
        });
    }

    @Override
    public void initEvents(VoxelMap map) {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> map.onDisconnect());
        ClientConfigurationConnectionEvents.INIT.register((handler, client) -> map.onConfigurationInit());
        ClientPlayConnectionEvents.INIT.register((handler, client) -> map.onPlayInit());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> map.onJoinServer());
        ClientLifecycleEvents.CLIENT_STOPPING.register((client) -> map.onClientStopping());
    }
}
