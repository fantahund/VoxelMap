package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.Events;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

public class NeoForgeEvents implements Events {
    private VoxelMap map;

    NeoForgeEvents() {
    }

    @Override
    public void initEvents(VoxelMap map) {
        this.map = map;
        VoxelMapNeoForgeMod.getModEventBus().addListener(this::preInitClient);
        VoxelMapNeoForgeMod.getModEventBus().addListener(NeoForgePacketHandler::initClient);
        VoxelMapNeoForgeMod.getModEventBus().addListener(this::registerResourcePacks);
        VoxelMapNeoForgeMod.getModEventBus().addListener(this::registerReloadListener);
        NeoForge.EVENT_BUS.register(new NeoForgeEventListener(map));
    }

    private void preInitClient(final FMLClientSetupEvent event) {
        map.onClientStarted();
        map.onConfigurationInit();
    }

    private void registerResourcePacks(final AddPackFindersEvent event) {
        event.addPackFinders(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "resourcepacks/voxelmap_legacy"), PackType.CLIENT_RESOURCES, Component.translatable("resourcePack.minimap.voxelmapLegacy.title"), PackSource.BUILT_IN, false, Pack.Position.TOP);
    }

    private void registerReloadListener(final AddClientReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "reload_listener"), map);
    }

    private record NeoForgeEventListener(VoxelMap map) {
        @SubscribeEvent
        public void onJoin(ClientPlayerNetworkEvent.LoggingIn event) {
            map.onJoinServer();
        }

        @SubscribeEvent
        public void onQuit(ClientPlayerNetworkEvent.LoggingOut event) {
            map.onDisconnect();
        }

        @SubscribeEvent
        public void onClientShutdown(GameShuttingDownEvent event) {
            map.onClientStopping();
        }
    }
}
