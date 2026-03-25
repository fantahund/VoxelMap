package com.mamiyaotaru.voxelmap.forge;

import com.mamiyaotaru.voxelmap.Events;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.maven.artifact.versioning.ArtifactVersion;

import java.nio.file.Path;
import java.util.Optional;

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
        RegisterClientReloadListenersEvent.BUS.addListener(this::registerReloadListener);
        MinecraftForge.EVENT_BUS.register(new ForgeEventListener(map));
    }

    private void preInitClient(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ForgeSettingsPacketHandler.register();
            ForgeWorldIdPacketHandler.register();
            map.onClientStarted();
            map.onConfigurationInit();
        });
    }

    private void registerResourcePacks(final AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;

        Identifier packLocation = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "resourcepacks/voxelmap_legacy");
        Component packNameDisplay = Component.translatable("resourcePack.minimap.voxelmapLegacy.title");

        IModFileInfo modFileInfo = ModList.getModFileById(packLocation.getNamespace());
        if (modFileInfo == null || modFileInfo.getMods().isEmpty()) {
            return;
        }

        IModInfo modInfo = modFileInfo.getMods().get(0);
        ArtifactVersion version = modInfo.getVersion();
        String prefix = packLocation.getPath();
        String packId = "mod/" + packLocation;

        Path modPath = modFileInfo.getFile().findResource(prefix);

        PackLocationInfo locationInfo = new PackLocationInfo(
                packId,
                packNameDisplay,
                PackSource.BUILT_IN,
                Optional.of(new KnownPack("forge", packId, version.toString()))
        );

        Pack.ResourcesSupplier resourcesSupplier = new PathPackResources.PathResourcesSupplier(modPath);

        Pack pack = Pack.readMetaAndCreate(
                locationInfo,
                resourcesSupplier,
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

    private static class ForgeEventListener {
        private final VoxelMap map;

        public ForgeEventListener(VoxelMap map) {
            this.map = map;
        }

        @SubscribeEvent
        public void onRenderGui(AddGuiOverlayLayersEvent event) {
            Identifier voxelMapMinimapLayer = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "minimap");
            event.getLayeredDraw().add(voxelMapMinimapLayer, (graphics, deltaTracker) -> VoxelConstants.renderOverlay(graphics));
        }

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
