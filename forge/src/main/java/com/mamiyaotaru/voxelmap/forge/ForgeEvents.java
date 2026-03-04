package com.mamiyaotaru.voxelmap.forge;

import com.mamiyaotaru.voxelmap.Events;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class ForgeEvents implements Events {

    private VoxelMap map;

    ForgeEvents() {
    }

    @Override
    public void initEvents(VoxelMap map) {
        this.map = map;

        BusGroup modBusGroup = VoxelMapForgeMod.getModBusGroup();
        FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::preInitClient);
        AddPackFindersEvent.BUS.addListener(this::registerResourcePacks);
        AddReloadListenerEvent.BUS.addListener(this::registerReloadListener);
        MinecraftForge.EVENT_BUS.register(new ForgeEventListener(map));
    }

    private void preInitClient(final FMLCommonSetupEvent event) {
        map.onClientStarted();
        map.onConfigurationInit();

        event.enqueueWork(() -> {
            ForgeSettingsPacketHandler.register();
            ForgeWorldIdPacketHandler.register();
        });
    }

    private void registerResourcePacks(final AddPackFindersEvent event) {
    }

    private void registerReloadListener(final AddReloadListenerEvent event) {
        event.addListener(map);
    }

    private static class ForgeEventListener {
        private final VoxelMap map;

        public ForgeEventListener(VoxelMap map) {
            this.map = map;
        }

        @SubscribeEvent
        public void onRenderGui(CustomizeGuiOverlayEvent.Chat event) {
            VoxelConstants.renderOverlay(event.getGuiGraphics());
        }

        @SubscribeEvent
        public void onJoin(ClientPlayerNetworkEvent.LoggingIn event) {
            map.onPlayInit();
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
