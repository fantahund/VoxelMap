package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.interfaces.IRadar;
import com.mamiyaotaru.voxelmap.packets.WorldIdC2S;
import com.mamiyaotaru.voxelmap.persistent.PersistentMap;
import com.mamiyaotaru.voxelmap.persistent.PersistentMapSettingsManager;
import com.mamiyaotaru.voxelmap.persistent.ThreadManager;
import com.mamiyaotaru.voxelmap.util.BiomeRepository;
import com.mamiyaotaru.voxelmap.util.DimensionManager;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.MapUtils;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import com.mamiyaotaru.voxelmap.util.WorldUpdateListener;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;

public class VoxelMap implements PreparableReloadListener {
    public static MapSettingsManager mapOptions;
    public static RadarSettingsManager radarOptions;
    private PersistentMapSettingsManager persistentMapOptions;
    private Map map;
    private IRadar radar;
    private IRadar radarSimple;
    private PersistentMap persistentMap;
    private SettingsAndLightingChangeNotifier settingsAndLightingChangeNotifier;
    private WorldUpdateListener worldUpdateListener;
    private ColorManager colorManager;
    private WaypointManager waypointManager;
    private DimensionManager dimensionManager;
    private ClientLevel world;
    private String worldName = "";
    private static String passMessage;
    private ArrayDeque<Runnable> runOnWorldSet = new ArrayDeque();
    VoxelMap() {}

    public void lateInit(boolean showUnderMenus, boolean isFair) {
        mapOptions = new MapSettingsManager();
        mapOptions.showUnderMenus = showUnderMenus;
        radarOptions = new RadarSettingsManager();
        mapOptions.addSecondaryOptionsManager(radarOptions);
        this.persistentMapOptions = new PersistentMapSettingsManager();
        mapOptions.addSecondaryOptionsManager(this.persistentMapOptions);
        BiomeRepository.loadBiomeColors();
        this.colorManager = new ColorManager();
        this.waypointManager = new WaypointManager();
        this.dimensionManager = new DimensionManager();
        this.persistentMap = new PersistentMap();
        mapOptions.loadAll();

        try {
            if (isFair) {
                radarOptions.radarAllowed = false;
                radarOptions.radarMobsAllowed = false;
                radarOptions.radarPlayersAllowed = false;
            } else {
                radarOptions.radarAllowed = true;
                radarOptions.radarMobsAllowed = true;
                radarOptions.radarPlayersAllowed = true;
                this.radar = new Radar();
                this.radarSimple = new RadarSimple();
            }
        } catch (RuntimeException var4) {
            VoxelConstants.getLogger().error("Failed creating radar " + var4.getLocalizedMessage(), var4);
            radarOptions.radarAllowed = false;
            radarOptions.radarMobsAllowed = false;
            radarOptions.radarPlayersAllowed = false;
            this.radar = null;
            this.radarSimple = null;
        }

        Events.initEvents(this);
        this.map = new Map();
        this.settingsAndLightingChangeNotifier = new SettingsAndLightingChangeNotifier();
        this.worldUpdateListener = new WorldUpdateListener();
        this.worldUpdateListener.addListener(this.map);
        this.worldUpdateListener.addListener(this.persistentMap);
        ReloadableResourceManager resourceManager = (ReloadableResourceManager) VoxelConstants.getMinecraft().getResourceManager();
        resourceManager.registerReloadListener(this);
        this.apply(resourceManager);
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier synchronizer, ResourceManager manager, ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler, Executor prepareExecutor, Executor applyExecutor) {
        return synchronizer.wait((Object) Unit.INSTANCE).thenRunAsync(() -> this.apply(manager), applyExecutor);
    }

    private void apply(ResourceManager resourceManager) {
        this.waypointManager.onResourceManagerReload(resourceManager);
        if (this.radar != null) {
            this.radar.onResourceManagerReload(resourceManager);
        }

        if (this.radarSimple != null) {
            this.radarSimple.onResourceManagerReload(resourceManager);
        }

        this.colorManager.onResourceManagerReload(resourceManager);
    }

    public void onTickInGame(GuiGraphics drawContext) {
        this.map.onTickInGame(drawContext);
        if (passMessage != null) {
            VoxelConstants.getMinecraft().gui.getChat().addMessage(Component.literal(passMessage));
            passMessage = null;
        }

    }

    public void onTick() {
        if (!Objects.equals(this.world, GameVariableAccessShim.getWorld())) {
            this.world = GameVariableAccessShim.getWorld();
            this.waypointManager.newWorld(this.world);
            this.persistentMap.newWorld(this.world);
            if (this.world != null) {
                MapUtils.reset();
                // send "new" world_id packet
                ByteBuf wIdRequestBuf = Unpooled.buffer(3);
                wIdRequestBuf.writeByte(0);
                wIdRequestBuf.writeByte(42);
                wIdRequestBuf.writeByte(0);
                if (ClientPlayNetworking.canSend(WorldIdC2S.PACKET_ID)) {
                    ClientPlayNetworking.send(new WorldIdC2S());
                }

                //FIXME 1.20.2 VoxelConstants.getPlayer().getSkinTexture();
                /*java.util.Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> skinMap = VoxelConstants.getMinecraft().getSkinProvider().getTextures(VoxelConstants.getPlayer().getGameProfile());
                if (skinMap.containsKey(MinecraftProfileTexture.Type.SKIN)) {
                    VoxelConstants.getMinecraft().getSkinProvider().loadSkin(skinMap.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
                }*/

                if (!this.worldName.equals(this.waypointManager.getCurrentWorldName())) {
                    this.worldName = this.waypointManager.getCurrentWorldName();
                }

                this.map.newWorld(this.world);
                while (!runOnWorldSet.isEmpty()) {
                    runOnWorldSet.removeFirst().run();
                }
            }
        }

        VoxelConstants.tick();
        this.persistentMap.onTick();
    }

    public static void checkPermissionMessages(Component message) {
        String msg = TextUtils.asFormattedString(message);
        msg = msg.replaceAll("§r", "");

        if (msg.contains("§3 §6 §3 §6 §3 §6 §d")) {
            mapOptions.cavesAllowed = false;
            VoxelConstants.getLogger().info("Server disabled cavemapping.");
        }

        if (msg.contains("§3 §6 §3 §6 §3 §6 §e")) {
            radarOptions.radarAllowed = false;
            radarOptions.radarPlayersAllowed = false;
            radarOptions.radarMobsAllowed = false;
            VoxelConstants.getLogger().info("Server disabled radar.");
        }

        if (msg.contains("§3 §6 §3 §6 §3 §6 §f")) {
            mapOptions.cavesAllowed = true;
            VoxelConstants.getLogger().info("Server enabled cavemapping.");
        }

        if (msg.contains("§3 §6 §3 §6 §3 §6 §0")) {
            radarOptions.radarAllowed = true;
            radarOptions.radarPlayersAllowed = true;
            radarOptions.radarMobsAllowed = true;
            VoxelConstants.getLogger().info("Server enabled radar.");
        }

    }

    public MapSettingsManager getMapOptions() {
        return mapOptions;
    }

    public RadarSettingsManager getRadarOptions() {
        return radarOptions;
    }

    public PersistentMapSettingsManager getPersistentMapOptions() {
        return this.persistentMapOptions;
    }

    public Map getMap() {
        return this.map;
    }

    public SettingsAndLightingChangeNotifier getSettingsAndLightingChangeNotifier() {
        return this.settingsAndLightingChangeNotifier;
    }

    public IRadar getRadar() {
        if (radarOptions.showRadar) {
            if (radarOptions.radarMode == 1) {
                return this.radarSimple;
            }

            if (radarOptions.radarMode == 2) {
                return this.radar;
            }
        }

        return null;
    }

    public ColorManager getColorManager() {
        return this.colorManager;
    }

    public WaypointManager getWaypointManager() {
        return this.waypointManager;
    }

    public DimensionManager getDimensionManager() {
        return this.dimensionManager;
    }

    public PersistentMap getPersistentMap() {
        return this.persistentMap;
    }

    public void setPermissions(boolean hasFullRadarPermission, boolean hasPlayersOnRadarPermission, boolean hasMobsOnRadarPermission, boolean hasCavemodePermission) {
        radarOptions.radarAllowed = hasFullRadarPermission;
        radarOptions.radarPlayersAllowed = hasPlayersOnRadarPermission;
        radarOptions.radarMobsAllowed = hasMobsOnRadarPermission;
        mapOptions.cavesAllowed = hasCavemodePermission;
    }

    public synchronized void newSubWorldName(String name, boolean fromServer) {
        Runnable run = new Runnable() {
            @Override
            public void run() {
                VoxelMap.this.waypointManager.setSubworldName(name, fromServer);
                VoxelMap.this.map.newWorldName();
            }
        };
        if (world == null) {
            runOnWorldSet.addLast(run);
        } else {
            run.run();
        }
    }

    public String getWorldSeed() {
        return waypointManager.getWorldSeed().isEmpty() ? VoxelConstants.getWorldByKey(Level.OVERWORLD).map(value -> Long.toString(((ServerLevel) value).getSeed())).orElse("") : waypointManager.getWorldSeed();
    }

    public void setWorldSeed(String newSeed) {
        waypointManager.setWorldSeed(newSeed);
    }

    public void sendPlayerMessageOnMainThread(String s) {
        passMessage = s;
    }

    public WorldUpdateListener getWorldUpdateListener() {
        return this.worldUpdateListener;
    }

    public void clearServerSettings() {
        radarOptions.radarAllowed = true;
        radarOptions.radarPlayersAllowed = true;
        radarOptions.radarMobsAllowed = true;
        mapOptions.cavesAllowed = true;
        mapOptions.serverTeleportCommand = null;

        mapOptions.worldmapAllowed = true;
        mapOptions.minimapAllowed = true;
        mapOptions.waypointsAllowed = true;
        mapOptions.deathWaypointAllowed = true;
    }

    public void onPlayInit() {
        // registries are ready, but no world
    }

    public void onDisconnect() {
        clearServerSettings();
    }

    public void onConfigurationInit() {
        clearServerSettings();
    }

    public void onClientStopping() {
        VoxelConstants.onShutDown();
        ThreadManager.flushSaveQueue();
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, Executor executor, Executor executor2) {
        return null; //FIXME 1.21.2
    }
}
