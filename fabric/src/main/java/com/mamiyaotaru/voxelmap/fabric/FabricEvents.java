package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.Events;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

public class FabricEvents implements Events {
    FabricEvents() {
    }

    @Override
    public void initEvents(VoxelMap map) {
        ClientLifecycleEvents.CLIENT_STARTED.register((client) -> map.onClientStarted());
        ClientLifecycleEvents.CLIENT_STOPPING.register((client) -> map.onClientStopping());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> map.onDisconnect());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> map.onJoinServer());
        ClientConfigurationConnectionEvents.INIT.register((handler, client) -> map.onConfigurationInit());

        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "reload_listener"), map);
    }
}