package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.Events;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.packets.VoxelmapSettingsS2C;
import com.mamiyaotaru.voxelmap.packets.WorldIdS2C;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NeoForgeEvents implements Events {
    private VoxelMap map;

    NeoForgeEvents() {
    }

    @Override
    public void initEvents(VoxelMap map) {
        this.map = map;
        VoxelmapNeoForgeMod.getModEventBus().addListener(this::preInitClient);
        VoxelmapNeoForgeMod.getModEventBus().addListener(this::registerPackets);
        VoxelmapNeoForgeMod.getModEventBus().addListener(this::registerResourcePacks);
        VoxelmapNeoForgeMod.getModEventBus().addListener(this::registerReloadListener);
        VoxelmapNeoForgeMod.getModEventBus().addListener(this::registerClientPayloadHandlers);
        NeoForge.EVENT_BUS.register(new ForgeEventListener(map));
    }

    private void preInitClient(final FMLClientSetupEvent event) {
        map.onConfigurationInit();
    }

    public void registerPackets(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.optional().commonToClient(VoxelmapSettingsS2C.PACKET_ID, VoxelmapSettingsS2C.PACKET_CODEC, VoxelmapSettingsChannelHandlerNeoForge::handleDataOnMain);
        registrar.optional().commonBidirectional(WorldIdS2C.PACKET_ID, WorldIdS2C.PACKET_CODEC, VoxelmapWorldIdChannelHandlerNeoForge::handleDataOnMain);
    }

    public void registerClientPayloadHandlers(final RegisterClientPayloadHandlersEvent event) {
        event.register(WorldIdS2C.PACKET_ID, VoxelmapWorldIdChannelHandlerNeoForge::handleDataOnMain);
    }

    private void registerResourcePacks(final AddPackFindersEvent event) {
        event.addPackFinders(Identifier.fromNamespaceAndPath("voxelmap", "voxelmap_legacy"), PackType.CLIENT_RESOURCES, Component.translatable("resourcePack.minimap.voxelmapLegacy.title"), PackSource.BUILT_IN, true, Pack.Position.BOTTOM);
    }

    private void registerReloadListener(final AddClientReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath("voxelmap", "reload_listener"), map);
        map.applyResourceManager(VoxelConstants.getMinecraft().getResourceManager());
    }

    private static class ForgeEventListener {
        private final VoxelMap map;

        public ForgeEventListener(VoxelMap map) {
            this.map = map;
        }

        @SubscribeEvent
        public void onRenderGui(RenderGuiLayerEvent.Post event) {
            if (event.getName().equals(VanillaGuiLayers.BOSS_OVERLAY)) {
                VoxelConstants.renderOverlay(event.getGuiGraphics());
            }
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
