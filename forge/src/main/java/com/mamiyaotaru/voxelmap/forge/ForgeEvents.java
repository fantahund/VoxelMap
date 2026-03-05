package com.mamiyaotaru.voxelmap.forge;

import com.mamiyaotaru.voxelmap.Events;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayer;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
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
        AddGuiOverlayLayersEvent.BUS.addListener(this::registerGuiLayer);
        AddPackFindersEvent.BUS.addListener(this::registerResourcePacks);
        AddReloadListenerEvent.BUS.addListener(this::registerReloadListener);
        ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(this::onJoin);
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(this::onQuit);
        GameShuttingDownEvent.BUS.addListener(this::onClientShutdown);
    }

    private void preInitClient(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ForgeSettingsPacketHandler.register();
            ForgeWorldIdPacketHandler.register();
            map.onClientStarted();
            map.onConfigurationInit();
        });
    }

    private void registerGuiLayer(AddGuiOverlayLayersEvent event) {
        Identifier voxelMapMinimapLayer = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "minimap");
        event.getLayeredDraw().add(voxelMapMinimapLayer, new ForgeLayer() {
            @Override
            public void render(GuiGraphics graphics, DeltaTracker dt) {
                map.onTickInGame(graphics);
            }
        });
    }

    private void registerResourcePacks(AddPackFindersEvent event) {
    }

    private void registerReloadListener(AddReloadListenerEvent event) {
        event.addListener(map);
    }

    private void onJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        map.onPlayInit();
    }

    private void onQuit(ClientPlayerNetworkEvent.LoggingOut event) {
        map.onDisconnect();
    }

    private void onClientShutdown(GameShuttingDownEvent event) {
        map.onClientStopping();
    }
}
