package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.Events;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

public class ForgeEvents implements Events {
    private VoxelMap map;

    ForgeEvents() {
    }

    @Override
    public void initEvents(VoxelMap map) {
        this.map = map;
        VoxelmapNeoForgeMod.getModEventBus().addListener(this::preInitClient);
        NeoForge.EVENT_BUS.register(new ForgeEventListener(map));
    }

    private void preInitClient(final FMLClientSetupEvent event) {
        map.onConfigurationInit();
    }


    public static class ForgeEventListener {
        private final VoxelMap map;

        public ForgeEventListener(VoxelMap map) {
            this.map = map;
        }

        @SubscribeEvent
        public void onRenderGui(RenderGuiEvent.Pre event) {
            VoxelConstants.renderOverlay(event.getGuiGraphics());
        }

        @SubscribeEvent
        public void onJoin(ClientPlayerNetworkEvent.LoggingIn event) {
            map.onPlayInit();
        }

        @SubscribeEvent
        public void onJoin(ClientPlayerNetworkEvent.LoggingOut event) {
            map.onDisconnect();
        }

        @SubscribeEvent
        public void onJoin(GameShuttingDownEvent event) {
            map.onDisconnect();
        }
    }
}
