package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.Events;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

public class FabricEvents implements Events {
    FabricEvents() {
    }

    @Override
    public void initEvents(VoxelMap map) {
        Identifier voxelMapMinimapLayer = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "minimap");
        HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR, voxelMapMinimapLayer, (context, tickCounter) -> VoxelConstants.renderOverlay(context));

        ClientLifecycleEvents.CLIENT_STARTED.register((client) -> map.onClientStarted());
        ClientLifecycleEvents.CLIENT_STOPPING.register((client) -> map.onClientStopping());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> map.onDisconnect());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> map.onJoinServer());
        ClientConfigurationConnectionEvents.INIT.register((handler, client) -> map.onConfigurationInit());

        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "reload_listener"), map);

        FabricLoader.getInstance().getModContainer(VoxelConstants.MOD_ID).ifPresent(container -> {
            // 1. pack location, 2. mod container, 3. pack title, 4. pack activation type
            ResourceLoader.registerBuiltinPack(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "voxelmap_legacy"), container, Component.translatable("resourcePack.minimap.voxelmapLegacy.title"), PackActivationType.NORMAL);
        });
    }
}