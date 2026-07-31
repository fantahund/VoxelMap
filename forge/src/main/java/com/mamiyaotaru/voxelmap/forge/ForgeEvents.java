package com.mamiyaotaru.voxelmap.forge;

import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.multiloader.Events;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ForgeEvents implements Events {

    private VoxelMap map;

    ForgeEvents() {
    }

    @Override
    public void initEvents(VoxelMap map) {
        this.map = map;

        BusGroup modBusGroup = VoxelMapForgeMod.getModBusGroup();
        FMLClientSetupEvent.getBus(modBusGroup).addListener(this::preInitClient);
        AddPackFindersEvent.BUS.addListener(ForgePackRegistrar::registerPacks);
        RegisterClientReloadListenersEvent.BUS.addListener(this::registerReloadListener);
        MinecraftForge.EVENT_BUS.register(new ForgeEventListener(map));
    }

    private void preInitClient(final FMLClientSetupEvent event) {
        map.onClientStarted();
        map.onConfigurationInit();
    }

    private void registerReloadListener(final RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(map);
    }

    private record ForgeEventListener(VoxelMap map) {
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
