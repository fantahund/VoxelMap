package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.interfaces.AbstractRadar;
import com.mamiyaotaru.voxelmap.persistent.PersistentMap;
import com.mamiyaotaru.voxelmap.persistent.PersistentMapSettingsManager;
import com.mamiyaotaru.voxelmap.persistent.ThreadManager;
import com.mamiyaotaru.voxelmap.util.BiomeRepository;
import com.mamiyaotaru.voxelmap.util.DimensionManager;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.MapUtils;
import com.mamiyaotaru.voxelmap.util.ModrinthUpdateChecker;
import com.mamiyaotaru.voxelmap.util.WorldUpdateListener;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Unit;
import net.minecraft.world.level.Level;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class VoxelMap implements PreparableReloadListener {
    private static final boolean SHOW_UNDER_MENUS = true;
    private static final boolean IS_FAIR = false;
    private MapSettingsManager mapOptions;
    private RadarSettingsManager radarOptions;
    private PersistentMapSettingsManager persistentMapOptions;
    private boolean initialized = false;
    private Map map;
    private Radar radar;
    private RadarSimple radarSimple;
    private PersistentMap persistentMap;
    private SettingsAndLightingChangeNotifier settingsAndLightingChangeNotifier;
    private WorldUpdateListener worldUpdateListener;
    private ColorManager colorManager;
    private WaypointManager waypointManager;
    private DimensionManager dimensionManager;
    private ClientLevel world;
    private String worldName = "";
    private String passMessage;
    private Properties imageProperties;
    private final ArrayDeque<Runnable> runOnWorldSet = new ArrayDeque<>();
    private final ArrayDeque<Runnable> runOnInitialized = new ArrayDeque<>();

    VoxelMap() {}

    private void lateInit(boolean showUnderMenus, boolean isFair) {
        mapOptions = new MapSettingsManager();
        radarOptions = new RadarSettingsManager();
        persistentMapOptions = new PersistentMapSettingsManager();

        mapOptions.showUnderMenus = showUnderMenus;
        radarOptions.radarAllowed = !isFair;
        radarOptions.radarMobsAllowed = !isFair;
        radarOptions.radarPlayersAllowed = !isFair;

        mapOptions.addSubSettingsManager(radarOptions);
        mapOptions.addSubSettingsManager(persistentMapOptions);
        mapOptions.loadAll();

        colorManager = new ColorManager();
        waypointManager = new WaypointManager();
        dimensionManager = new DimensionManager();
        persistentMap = new PersistentMap();

        try {
            boolean radarAllowed = radarOptions.radarAllowed;
            boolean mobsAllowed = radarOptions.radarMobsAllowed;
            boolean playersAllowed = radarOptions.radarPlayersAllowed;

            if (radarAllowed || mobsAllowed || playersAllowed) {
                radar = new Radar();
                radarSimple = new RadarSimple();
            }
        } catch (RuntimeException var4) {
            VoxelConstants.getLogger().error("Failed creating radar " + var4.getLocalizedMessage(), var4);
            radarOptions.radarAllowed = false;
            radarOptions.radarMobsAllowed = false;
            radarOptions.radarPlayersAllowed = false;
            radar = null;
            radarSimple = null;
        }

        map = new Map();
        settingsAndLightingChangeNotifier = new SettingsAndLightingChangeNotifier();
        worldUpdateListener = new WorldUpdateListener();
        worldUpdateListener.addListener(map);
        worldUpdateListener.addListener(persistentMap);

        initialized = true;
    }

    public synchronized void runAfterWorldSet(Runnable task) {
        if (world == null) {
            runOnWorldSet.addLast(task);
        } else {
            task.run();
        }
    }

    public synchronized void runAfterInitialized(Runnable task) {
        if (!initialized) {
            runOnInitialized.addLast(task);
        } else {
            task.run();
        }
    }

    @Override
    public CompletableFuture<Void> reload(SharedState sharedState, Executor executor, PreparationBarrier preparationBarrier, Executor executor2) {
        return preparationBarrier.wait((Object) Unit.INSTANCE).thenRunAsync(() -> apply(sharedState.resourceManager()), executor2);
    }

    public void applyResourceManager(ResourceManager resourceManager) {
        apply(resourceManager);
    }

    protected void apply(ResourceManager resourceManager) {
        runAfterInitialized(() -> {
            loadImageProperties();

            waypointManager.onResourceManagerReload(resourceManager);
            if (radar != null) {
                radar.onResourceManagerReload(resourceManager);
            }

            colorManager.onResourceManagerReload(resourceManager);
            BiomeRepository.loadBiomeColors();

            if (map != null) {
                map.onResourceManagerReload(resourceManager);
            }
        });
    }

    public void onEventsSet(Events events) {
        events.initEvents(this);
    }

    public void onTickInGame(GuiGraphics guiGraphics) {
        if (!initialized) return;

        map.onTickInGame(guiGraphics);
        if (passMessage != null) {
            VoxelConstants.getMinecraft().gui.getChat().addClientSystemMessage(Component.literal(passMessage));
            passMessage = null;
        }

    }

    public void onTick() {
        if (!initialized) {
            lateInit(SHOW_UNDER_MENUS, IS_FAIR);
            return;
        }

        while (!runOnInitialized.isEmpty()) {
            runOnInitialized.removeFirst().run();
        }

        ClientLevel newWorld = GameVariableAccessShim.getWorld();
        if (world != newWorld) {
            world = newWorld;
            waypointManager.newWorld(world);
            persistentMap.newWorld(world);
            if (world != null) {
                MapUtils.reset();
                // send "new" world_id packet

                VoxelConstants.getPacketBridge().sendWorldIDPacket();

                if (!worldName.equals(waypointManager.getCurrentWorldName())) {
                    worldName = waypointManager.getCurrentWorldName();
                }

                map.newWorld(world);
                while (!runOnWorldSet.isEmpty()) {
                    runOnWorldSet.removeFirst().run();
                }
            }
        }

        VoxelConstants.tick();
        persistentMap.onTick();
    }

    public void checkPermissionMessages(Component message) {
        String msg = message.getString().replaceAll("§r", "");

        runAfterInitialized(() -> {
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
        });
    }

    public MapSettingsManager getMapOptions() {
        return mapOptions;
    }

    public RadarSettingsManager getRadarOptions() {
        return radarOptions;
    }

    public PersistentMapSettingsManager getPersistentMapOptions() {
        return persistentMapOptions;
    }

    public Map getMap() {
        return map;
    }

    public SettingsAndLightingChangeNotifier getSettingsAndLightingChangeNotifier() {
        return settingsAndLightingChangeNotifier;
    }

    public AbstractRadar getRadar() {
        if (radarOptions.showRadar) {
            if (radarOptions.radarMode == 1) {
                return radarSimple;
            }

            if (radarOptions.radarMode == 2) {
                return radar;
            }
        }

        return null;
    }

    public Radar getFullRadar() {
        return radar;
    }

    public ColorManager getColorManager() {
        return colorManager;
    }

    public WaypointManager getWaypointManager() {
        return waypointManager;
    }

    public DimensionManager getDimensionManager() {
        return dimensionManager;
    }

    public PersistentMap getPersistentMap() {
        return persistentMap;
    }

    public void setPermissions(boolean hasFullRadarPermission, boolean hasPlayersOnRadarPermission, boolean hasMobsOnRadarPermission, boolean hasCavemodePermission) {
        runAfterInitialized(() -> {
            radarOptions.radarAllowed = hasFullRadarPermission;
            radarOptions.radarPlayersAllowed = hasPlayersOnRadarPermission;
            radarOptions.radarMobsAllowed = hasMobsOnRadarPermission;
            mapOptions.cavesAllowed = hasCavemodePermission;
        });
    }

    public synchronized void newSubWorldName(String name, boolean fromServer) {
        runAfterWorldSet(() -> {
            waypointManager.setSubworldName(name, fromServer);
            map.newWorldName();
        });
    }

    public String getWorldSeed() {
        if (!initialized) return "";
        return waypointManager.getWorldSeed().isEmpty() ? VoxelConstants.getWorldByKey(Level.OVERWORLD).map(value -> Long.toString(((ServerLevel) value).getSeed())).orElse("") : waypointManager.getWorldSeed();
    }

    public void setWorldSeed(String newSeed) {
        if (!initialized) return;
        waypointManager.setWorldSeed(newSeed);
    }

    public void sendPlayerMessageOnMainThread(String s) {
        passMessage = s;
    }

    public WorldUpdateListener getWorldUpdateListener() {
        return worldUpdateListener;
    }

    public void clearServerSettings() {
        runAfterInitialized(() -> {
            radarOptions.radarAllowed = true;
            radarOptions.radarPlayersAllowed = true;
            radarOptions.radarMobsAllowed = true;
            mapOptions.cavesAllowed = true;
            mapOptions.serverTeleportCommand = null;

            mapOptions.worldmapAllowed = true;
            mapOptions.minimapAllowed = true;
            mapOptions.waypointsAllowed = true;
            mapOptions.deathWaypointAllowed = true;
        });
    }

    public void onPlayInit() {
        // registries are ready, but no world
    }

    public void onJoinServer() {
        if (getRadar() != null) {
            getRadar().onJoinServer();
        }
        ModrinthUpdateChecker.checkUpdates();
    }

    public void onDisconnect() {
        clearServerSettings();
    }

    public void onConfigurationInit() {
        clearServerSettings();
    }

    public void onClientStarted() {
    }

    public void onClientStopping() {
        VoxelConstants.onShutDown();
        ThreadManager.flushSaveQueue();
    }

    public Properties getImageProperties() {
        if (imageProperties == null) {
            loadImageProperties();
        }
        return imageProperties;
    }

    private void loadImageProperties() {
        imageProperties = new Properties();
        Identifier location = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "configs/images.properties");
        Optional<Resource> resource = VoxelConstants.getMinecraft().getResourceManager().getResource(location);
        if (resource.isEmpty()) {
            VoxelConstants.getLogger().warn("Image properties file at {} is missing!", location);
        } else {
            try (InputStream inputStream = resource.get().open()) {
                imageProperties.load(inputStream);
            } catch (Exception e) {
                VoxelConstants.getLogger().warn("Failed to read image properties from {}. {}", location, e);
            }
        }
    }
}
