package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.Events;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.packets.VoxelmapSettingsS2C;
import com.mamiyaotaru.voxelmap.packets.WorldIdS2C;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ForgeEvents implements Events {
    private VoxelMap map;

    ForgeEvents() {
    }

    @Override
    public void initEvents(VoxelMap map) {
        this.map = map;
        VoxelmapNeoForgeMod.getModEventBus().addListener(this::preInitClient);
        NeoForge.EVENT_BUS.register(new ForgeEventListener(map));
        NeoForge.EVENT_BUS.register(ForgeEventPacketListener.class);
    }

    private void preInitClient(final FMLClientSetupEvent event) {
        map.onConfigurationInit();
    }


    private static class ForgeEventListener {
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

    private static class ForgeEventPacketListener {
        @SubscribeEvent
        public void register(final RegisterPayloadHandlersEvent event) {
            final PayloadRegistrar registrar = event.registrar("1");
            registrar.playBidirectional(VoxelmapSettingsS2C.PACKET_ID, VoxelmapSettingsS2C.PACKET_CODEC, new DirectionalPayloadHandler<>(VoxelmapSettingsChannelHandlerNeoForge::handleDataOnMain, VoxelmapSettingsChannelHandlerNeoForge::handleDataOnMain));
            registrar.playBidirectional(WorldIdS2C.PACKET_ID, WorldIdS2C.PACKET_CODEC, new DirectionalPayloadHandler<>(VoxelmapWorldIdChannelHandlerNeoForge::handleDataOnMain, VoxelmapWorldIdChannelHandlerNeoForge::handleDataOnMain));
        }
    }
}
