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
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.Unit;

public class FabricEvents implements Events {
    FabricEvents() {
        Identifier voxelMapMinimapLayer = Identifier.fromNamespaceAndPath("voxelmap", "minimap");
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

        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(Identifier.fromNamespaceAndPath("voxelmap", "reload_listener"), (sharedState, executor, preparationBarrier, executor2) -> preparationBarrier.wait(Unit.INSTANCE).thenRunAsync(() -> map.applyResourceManager(sharedState.resourceManager()), executor2));
        map.applyResourceManager(VoxelConstants.getMinecraft().getResourceManager());

        FabricLoader.getInstance().getModContainer("voxelmap").ifPresent(container -> {
            // 1. namespace:pack_name, 2. mod container, 3. pack title, 4. pack activation type
            ResourceLoader.registerBuiltinPack(Identifier.fromNamespaceAndPath("voxelmap", "voxelmap_legacy"), container, Component.translatable("resourcePack.minimap.voxelmapLegacy.title"), PackActivationType.NORMAL);
        });
    }
}