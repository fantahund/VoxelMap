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
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ForgeEvents implements Events {
    private VoxelMap map;

    ForgeEvents() {
    }

    @Override
    public void initEvents(VoxelMap map) {
        this.map = map;
        VoxelmapNeoForgeMod.getModEventBus().addListener(this::preInitClient);
        VoxelmapNeoForgeMod.getModEventBus().addListener(this::registerPackets);
        NeoForge.EVENT_BUS.register(new ForgeEventListener(map));
    }

    private void preInitClient(final FMLClientSetupEvent event) {
        map.onConfigurationInit();
    }

    public void registerPackets(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.optional().playToClient(VoxelmapSettingsS2C.PACKET_ID, VoxelmapSettingsS2C.PACKET_CODEC, VoxelmapSettingsChannelHandlerNeoForge::handleDataOnMain);
        registrar.optional().playToClient(WorldIdS2C.PACKET_ID, WorldIdS2C.PACKET_CODEC, VoxelmapWorldIdChannelHandlerNeoForge::handleDataOnMain);
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
        public void onQuit(ClientPlayerNetworkEvent.LoggingOut event) {
            map.onDisconnect();
        }

        @SubscribeEvent
        public void onClientShutdown(GameShuttingDownEvent event) {
            map.onClientStopping();
        }
    }
}
