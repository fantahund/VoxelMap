package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.Events;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.packets.VoxelmapSettingsS2C;
import com.mamiyaotaru.voxelmap.packets.WorldIdC2S;
import com.mamiyaotaru.voxelmap.packets.WorldIdS2C;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.GameShuttingDownEvent;

import java.util.function.Supplier;

public class ForgeEvents implements Events {
    private VoxelMap map;
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("voxelmap", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    ForgeEvents() {
    }

    @Override
    public void initEvents(VoxelMap map) {
        this.map = map;
        VoxelmapNeoForgeMod.getModEventBus().addListener(this::preInitClient);
        VoxelmapNeoForgeMod.getModEventBus().addListener(this::registerPackets);
        MinecraftForge.EVENT_BUS.register(new ForgeEventListener(map));
    }

    private void preInitClient(final FMLClientSetupEvent event) {
        map.onConfigurationInit();
    }

    public void registerPackets(final FMLClientSetupEvent event) {
        int id = 0;
        CHANNEL.registerMessage(id++, VoxelmapSettingsS2C.class,
            VoxelmapSettingsS2C::write,
            VoxelmapSettingsS2C::new,
            (msg, ctx) -> VoxelmapSettingsChannelHandlerNeoForge.handleDataOnMain(msg, ctx),
            () -> NetworkDirection.PLAY_TO_CLIENT
        );
        CHANNEL.registerMessage(id++, WorldIdS2C.class,
            WorldIdS2C::write,
            WorldIdS2C::new,
            (msg, ctx) -> VoxelmapWorldIdChannelHandlerNeoForge.handleDataOnMain(msg, ctx),
            () -> NetworkDirection.PLAY_TO_CLIENT
        );
        CHANNEL.registerMessage(id++, WorldIdC2S.class,
            WorldIdC2S::write,
            WorldIdC2S::new,
            (msg, ctx) -> { ctx.get().setPacketHandled(true); },
            () -> NetworkDirection.PLAY_TO_SERVER
        );
    }

    private static class ForgeEventListener {
        private final VoxelMap map;

        public ForgeEventListener(VoxelMap map) {
            this.map = map;
        }

        @SubscribeEvent
        public void onRenderGui(RenderGuiOverlayEvent.Pre event) {
            // In 1.20.1, render on all overlays or check specific overlay name
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
