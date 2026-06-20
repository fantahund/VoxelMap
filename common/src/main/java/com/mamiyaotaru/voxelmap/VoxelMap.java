package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.entityrender.EntityMapImageManager;
import com.mamiyaotaru.voxelmap.interfaces.AbstractRadar;
import com.mamiyaotaru.voxelmap.interfaces.IReloadListener;
import com.mamiyaotaru.voxelmap.options.MapOptionsManager;
import com.mamiyaotaru.voxelmap.options.ServerSettingsManager;
import com.mamiyaotaru.voxelmap.options.containers.MapOptions;
import com.mamiyaotaru.voxelmap.options.containers.PersistentMapOptions;
import com.mamiyaotaru.voxelmap.options.containers.RadarOptions;
import com.mamiyaotaru.voxelmap.options.containers.WaypointOptions;
import com.mamiyaotaru.voxelmap.persistent.PersistentMap;
import com.mamiyaotaru.voxelmap.persistent.ThreadManager;
import com.mamiyaotaru.voxelmap.util.BiomeRepository;
import com.mamiyaotaru.voxelmap.util.DimensionManager;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.MapUtils;
import com.mamiyaotaru.voxelmap.util.ModrinthUpdateChecker;
import com.mamiyaotaru.voxelmap.util.WorldUpdateListener;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Unit;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class VoxelMap implements PreparableReloadListener, Executor {
    private boolean initialized = false;

    private ServerSettingsManager serverSettings;
    private MapOptionsManager optionsManager;
    private MapOptions mapOptions;
    private PersistentMapOptions persistentMapOptions;
    private RadarOptions radarOptions;
    private WaypointOptions waypointOptions;

    private Map map;
    private PersistentMap persistentMap;
    private Radar radar;
    private RadarSimple radarSimple;
    private SettingsAndLightingChangeNotifier settingsAndLightingChangeNotifier;
    private WorldUpdateListener worldUpdateListener;

    private ColorManager colorManager;
    private WaypointManager waypointManager;
    private DimensionManager dimensionManager;
    private EntityMapImageManager entityMapImageManager;

    private ClientLevel world;
    private String worldName = "";
    private String passMessage;
    private Properties imageProperties;

    private final ArrayDeque<Runnable> executeQueue = new ArrayDeque<>();
    private final ArrayDeque<Runnable> runOnWorldSet = new ArrayDeque<>();
    private final ArrayList<IReloadListener> reloadListeners = new ArrayList<>();

    VoxelMap() {}

    public void lateInit(boolean showUnderMenus, boolean isFair) {
        serverSettings = new ServerSettingsManager();
        serverSettings.radarAllowed.set(!isFair);
        serverSettings.radarMobsAllowed.set(!isFair);
        serverSettings.radarPlayersAllowed.set(!isFair);

        optionsManager = new MapOptionsManager();
        mapOptions = new MapOptions();
        persistentMapOptions = new PersistentMapOptions();
        radarOptions = new RadarOptions();
        waypointOptions = new WaypointOptions();
        optionsManager.addOptionsContainer(mapOptions);
        optionsManager.addOptionsContainer(persistentMapOptions);
        optionsManager.addOptionsContainer(radarOptions);
        optionsManager.addOptionsContainer(waypointOptions);
        optionsManager.loadAll();

        colorManager = new ColorManager();
        waypointManager = new WaypointManager();
        dimensionManager = new DimensionManager();
        entityMapImageManager = new EntityMapImageManager();

        try {
            if (serverSettings.radarAllowed.get() && (serverSettings.radarMobsAllowed.get() || serverSettings.radarPlayersAllowed.get())) {
                radar = new Radar();
                radarSimple = new RadarSimple();
            }
        } catch (RuntimeException e) {
            VoxelConstants.getLogger().error("Failed creating radar {}", e.getLocalizedMessage(), e);
            serverSettings.radarAllowed.set(false);
            serverSettings.radarMobsAllowed.set(false);
            serverSettings.radarPlayersAllowed.set(false);
            radar = null;
            radarSimple = null;
        }

        mapOptions.showUnderMenus.set(showUnderMenus);
        optionsManager.updateOptionsAllowed(serverSettings, "Initialization");

        map = new Map();
        persistentMap = new PersistentMap();
        settingsAndLightingChangeNotifier = new SettingsAndLightingChangeNotifier();
        worldUpdateListener = new WorldUpdateListener();
        worldUpdateListener.addListener(map);
        worldUpdateListener.addListener(persistentMap);

        initialized = true;
    }

    @Override
    public synchronized void execute(Runnable task) {
        if (!initialized) {
            executeQueue.addLast(task);
        } else {
            task.run();
        }
    }

    public synchronized void runAfterWorldSet(Runnable task) {
        if (world == null) {
            runOnWorldSet.addLast(task);
        } else {
            task.run();
        }
    }

    public void addReloadListener(IReloadListener listener) {
        reloadListeners.add(listener);
    }

    @Override
    public CompletableFuture<Void> reload(SharedState sharedState, Executor executor, PreparationBarrier preparationBarrier, Executor executor2) {
        return preparationBarrier.wait((Object) Unit.INSTANCE).thenRunAsync(() -> apply(sharedState.resourceManager()), executor2);
    }

    public boolean isRunning() {
        return initialized;
    }

    protected void apply(ResourceManager resourceManager) {
        execute(() -> {
            Identifier filePath = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "configs/images.properties");
            imageProperties = new Properties();
            resourceManager.getResource(filePath).ifPresent((resource) -> {
                try (InputStream is = resource.open()) {
                    imageProperties.load(is);
                } catch (IOException ignored) {
                }
            });

            BiomeRepository.loadBiomeColors();

            reloadListeners.forEach((listener) -> listener.onResourceManagerReload(resourceManager));
        });
    }

    public void onEventsSet(Events events) {
        events.initEvents(this);
    }

    public void onTickInGame(GuiGraphicsExtractor graphics) {
        if (!initialized) return;

        map.onTickInGame(graphics);
        if (passMessage != null) {
            VoxelConstants.getMinecraft().gui.hud.getChat().addClientSystemMessage(Component.literal(passMessage));
            passMessage = null;
        }

        entityMapImageManager.onRenderTick();

    }

    public void onTick() {
        if (!initialized) {
            return;
        }

        while (!executeQueue.isEmpty()) {
            executeQueue.removeFirst().run();
        }

        ClientLevel newWorld = GameVariableAccessShim.getWorld();
        if (world != newWorld) {
            world = newWorld;
            waypointManager.newWorld(world);
            persistentMap.newWorld(world);
            if (world != null) {
                MapUtils.reset();

                // send "new" world_id packet
                VoxelConstants.getPacketBridge().sendWorldIDPacket("");

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

        execute(() -> {
            boolean applied = false;

            if (msg.contains("§3 §6 §3 §6 §3 §6 §d")) {
                serverSettings.cavesAllowed.set(false);
                VoxelConstants.getLogger().info("Server disabled cavemapping.");
                applied = true;
            }

            if (msg.contains("§3 §6 §3 §6 §3 §6 §e")) {
                serverSettings.radarAllowed.set(false);
                serverSettings.radarMobsAllowed.set(false);
                serverSettings.radarPlayersAllowed.set(false);
                VoxelConstants.getLogger().info("Server disabled radar.");
                applied = true;
            }

            if (msg.contains("§3 §6 §3 §6 §3 §6 §f")) {
                serverSettings.cavesAllowed.set(true);
                VoxelConstants.getLogger().info("Server enabled cavemapping.");
                applied = true;
            }

            if (msg.contains("§3 §6 §3 §6 §3 §6 §0")) {
                serverSettings.radarAllowed.set(true);
                serverSettings.radarMobsAllowed.set(true);
                serverSettings.radarPlayersAllowed.set(true);
                VoxelConstants.getLogger().info("Server enabled radar.");
                applied = true;
            }

            if (applied) {
                optionsManager.updateOptionsAllowed(serverSettings, "Permission Message");
            }
        });
    }

    public ServerSettingsManager getServerSettings() {
        return serverSettings;
    }

    public MapOptionsManager getOptionsManager() {
        return optionsManager;
    }

    public MapOptions getMapOptions() {
        return mapOptions;
    }

    public PersistentMapOptions getPersistentMapOptions() {
        return persistentMapOptions;
    }

    public RadarOptions getRadarOptions() {
        return radarOptions;
    }

    public WaypointOptions getWaypointOptions() {
        return waypointOptions;
    }

    public Map getMap() {
        return map;
    }

    public PersistentMap getPersistentMap() {
        return persistentMap;
    }

    public SettingsAndLightingChangeNotifier getSettingsAndLightingChangeNotifier() {
        return settingsAndLightingChangeNotifier;
    }

    public AbstractRadar getRadar() {
        if (radarOptions.showRadar.get()) {
            return switch (radarOptions.radarMode.get()) {
                case SIMPLE -> radarSimple;
                case FULL -> radar;
            };
        }

        return null;
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

    public EntityMapImageManager getEntityMapImageManager() {
        return entityMapImageManager;
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

    public Properties getImageProperties() {
        return imageProperties;
    }

    public void sendPlayerMessageOnMainThread(String s) {
        passMessage = s;
    }

    public WorldUpdateListener getWorldUpdateListener() {
        return worldUpdateListener;
    }

    public void clearServerSettings() {
        execute(() -> {
            serverSettings.reset();
            optionsManager.updateOptionsAllowed(serverSettings, "Settings Reset");
        });
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
        if (map != null) {
            map.shutdown();
        }
        VoxelConstants.onShutDown();
        ThreadManager.flushSaveQueue();
        ThreadManager.shutdownCalculationQueue();
    }
}
