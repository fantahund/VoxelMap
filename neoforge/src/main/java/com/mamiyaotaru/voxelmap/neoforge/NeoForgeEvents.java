package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.multiloader.Events;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
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
        VoxelMapNeoForgeMod.getModEventBus().addListener(NeoForgePackRegistrar::registerPacks);
        VoxelMapNeoForgeMod.getModEventBus().addListener(this::registerReloadListener);
        NeoForge.EVENT_BUS.register(new NeoForgeEventListener(map));
    }

    private void preInitClient(final FMLClientSetupEvent event) {
        map.onClientStarted();
        map.onConfigurationInit();
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
