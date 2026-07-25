package com.mamiyaotaru.voxelmap.forge;

import com.mamiyaotaru.voxelmap.Events;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.versions.forge.ForgeVersion;

public class ForgeEvents implements Events {

    private VoxelMap map;

    ForgeEvents() {
    }

    @Override
    public void initEvents(VoxelMap map) {
        this.map = map;

        BusGroup modBusGroup = VoxelMapForgeMod.getModBusGroup();
        FMLClientSetupEvent.getBus(modBusGroup).addListener(this::preInitClient);
        AddPackFindersEvent.BUS.addListener(this::registerResourcePacks);
        RegisterClientReloadListenersEvent.BUS.addListener(this::registerReloadListener);
        MinecraftForge.EVENT_BUS.register(new ForgeEventListener(map));
    }

    private void preInitClient(final FMLClientSetupEvent event) {
        map.onClientStarted();
        map.onConfigurationInit();
    }

    private void registerResourcePacks(final AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;

        Identifier packLocation = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "resourcepacks/voxelmap_legacy");
        IModFileInfo voxelMapFileInfo = ModList.getModFileById(VoxelConstants.MOD_ID);
        if (voxelMapFileInfo == null) return;

        String packId = "mod/" + packLocation;
        Path packPath = voxelMapFileInfo.getFile().findResource(packLocation.getPath());
        String forgeId = ForgeVersion.MOD_ID;
        String forgeVersion = ForgeVersion.getVersion();

        PackLocationInfo packInfo = new PackLocationInfo(
                packId,
                Component.translatable("resourcePack.minimap.voxelmapLegacy.title"),
                PackSource.BUILT_IN,
                Optional.of(new KnownPack(forgeId, packId, forgeVersion))
        );

        Pack pack = Pack.readMetaAndCreate(
                packInfo,
                new PathPackResources.PathResourcesSupplier(packPath),
                PackType.CLIENT_RESOURCES,
                new PackSelectionConfig(false, Pack.Position.TOP, false)
        );

        if (pack != null) {
            event.addRepositorySource((packConsumer) -> packConsumer.accept(pack));
        }
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
