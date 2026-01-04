package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.Events;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.Unit;

public class FabricEvents implements Events {
    FabricEvents() {
        Identifier voxelMapMinimapLayer = Identifier.parse("voxelmap:minimap");
        HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR, voxelMapMinimapLayer, new HudElement() {
            @Override
            public void render(GuiGraphics context, DeltaTracker tickCounter) {
                VoxelConstants.renderOverlay(context);
            }
        });
    }

    @Override
    public void initEvents(VoxelMap map) {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> map.onDisconnect());
        ClientConfigurationConnectionEvents.INIT.register((handler, client) -> map.onConfigurationInit());
        ClientPlayConnectionEvents.INIT.register((handler, client) -> map.onPlayInit());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> map.onJoinServer());
        ClientLifecycleEvents.CLIENT_STOPPING.register((client) -> map.onClientStopping());

        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(Identifier.parse("voxelmap:reload_listener"), (sharedState, executor, preparationBarrier, executor2) -> preparationBarrier.wait(Unit.INSTANCE).thenRunAsync(() -> map.applyResourceManager(sharedState.resourceManager()), executor2));
        map.applyResourceManager(VoxelConstants.getMinecraft().getResourceManager());
    }
}