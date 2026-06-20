package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.Events;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.packets.SettingsPayload;
import com.mamiyaotaru.voxelmap.packets.WorldIdPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
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
        VoxelMapNeoForgeMod.getModEventBus().addListener(this::preInitClient);
        VoxelMapNeoForgeMod.getModEventBus().addListener(this::registerPackets);
        VoxelMapNeoForgeMod.getModEventBus().addListener(this::registerReloadListener);
        VoxelMapNeoForgeMod.getModEventBus().addListener(this::registerClientPayloadHandlers);
        VoxelMapNeoForgeMod.getModEventBus().addListener(this::registerResourcePacks);
        NeoForge.EVENT_BUS.register(new NeoForgeEventListener(map));
    }

    public void preInitClient(final FMLClientSetupEvent event) {
        map.onClientStarted();
        map.onConfigurationInit();
    }

    public void registerPackets(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.commonToClient(SettingsPayload.PACKET_ID, SettingsPayload.PACKET_CODEC, NeoForgeSettingsPacketHandler::receive);
        registrar.commonBidirectional(WorldIdPayload.PACKET_ID, WorldIdPayload.PACKET_CODEC, NeoForgeWorldIdPacketHandler::receive);
    }

    public void registerClientPayloadHandlers(final RegisterClientPayloadHandlersEvent event) {
        event.register(WorldIdPayload.PACKET_ID, NeoForgeWorldIdPacketHandler::receive);
    }

    public void registerReloadListener(final AddClientReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "reload_listener"), map);
    }

    public void registerResourcePacks(final AddPackFindersEvent event) {
        event.addPackFinders(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "resourcepacks/voxelmap_legacy"), PackType.CLIENT_RESOURCES, Component.translatable("resourcePack.minimap.voxelmapLegacy.title"), PackSource.BUILT_IN, false, Pack.Position.TOP);
    }

    public static class NeoForgeEventListener {
        private final VoxelMap map;

        public NeoForgeEventListener(VoxelMap map) {
            this.map = map;
        }

        @SubscribeEvent
        public void onJoin(final ClientPlayerNetworkEvent.LoggingIn event) {
            map.onJoinServer();
        }

        @SubscribeEvent
        public void onQuit(final ClientPlayerNetworkEvent.LoggingOut event) {
            map.onDisconnect();
        }

        @SubscribeEvent
        public void onClientShutdown(final GameShuttingDownEvent event) {
            map.onClientStopping();
        }
    }
}
