package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.interfaces.IRadar;
import com.mamiyaotaru.voxelmap.persistent.PersistentMap;
import com.mamiyaotaru.voxelmap.persistent.PersistentMapSettingsManager;
import com.mamiyaotaru.voxelmap.util.BiomeRepository;
import com.mamiyaotaru.voxelmap.util.DimensionManager;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.MapUtils;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import com.mamiyaotaru.voxelmap.util.WorldUpdateListener;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Unit;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class VoxelMap implements ResourceReloader {
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
    private ClientWorld world;
    private String worldName = "";
    private static String passMessage;

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
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            radarOptions.radarAllowed = true;
            radarOptions.radarPlayersAllowed = true;
            radarOptions.radarMobsAllowed = true;
            mapOptions.cavesAllowed = true;
            mapOptions.serverTeleportCommand = null;
        });
        this.map = new Map();
        this.settingsAndLightingChangeNotifier = new SettingsAndLightingChangeNotifier();
        this.worldUpdateListener = new WorldUpdateListener();
        this.worldUpdateListener.addListener(this.map);
        this.worldUpdateListener.addListener(this.persistentMap);
        ReloadableResourceManagerImpl resourceManager = (ReloadableResourceManagerImpl) VoxelConstants.getMinecraft().getResourceManager();
        resourceManager.registerReloader(this);
        this.apply(resourceManager);
    }

    @Override
    public CompletableFuture<Void> reload(Synchronizer synchronizer, ResourceManager manager, Profiler prepareProfiler, Profiler applyProfiler, Executor prepareExecutor, Executor applyExecutor) {
        return synchronizer.whenPrepared((Object) Unit.INSTANCE).thenRunAsync(() -> this.apply(manager), applyExecutor);
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

    public void onTickInGame(DrawContext drawContext) {
        this.map.onTickInGame(drawContext);
        if (passMessage != null) {
            VoxelConstants.getMinecraft().inGameHud.getChatHud().addMessage(Text.literal(passMessage));
            passMessage = null;
        }

    }

    public void onTick() {
        if (GameVariableAccessShim.getWorld() != null && !GameVariableAccessShim.getWorld().equals(this.world) || this.world != null && !this.world.equals(GameVariableAccessShim.getWorld())) {
            this.world = GameVariableAccessShim.getWorld();
            this.waypointManager.newWorld(this.world);
            this.persistentMap.newWorld(this.world);
            if (this.world != null) {
                MapUtils.reset();
                /*PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
                buffer.writeBytes("worldinfo:world_id".getBytes(StandardCharsets.UTF_8));
                buffer.writeByte(0);
                buffer.writeBytes("voxelmap:settings".getBytes(StandardCharsets.UTF_8));
                //TODO 1.20.2 VoxelConstants.getMinecraft().getNetworkHandler().sendPacket(new CustomPayloadC2SPacket(new Identifier("minecraft:register"), buffer));
                VoxelConstants.getMinecraft().getNetworkHandler().getConnection().send(new CustomPayloadC2SPacket(buffer));
                ByteBuf wIdRequestBuf = Unpooled.buffer(3);
                // send "new" world_id packet
                wIdRequestBuf.writeByte(0);
                wIdRequestBuf.writeByte(42);
                wIdRequestBuf.writeByte(0);*/
                //TODO 1.20.2 VoxelConstants.getMinecraft().getNetworkHandler().getConnection().send(new CustomPayloadC2SPacket(new PacketByteBuf(wIdRequestBuf)));
                //VoxelConstants.getPlayer().networkHandler.getConnection.send(new CustomPayloadC2SPacket(new Identifier("worldinfo:world_id"), new PacketByteBuf(wIdRequestBuf)));

                //FIXME 1.20.2 VoxelConstants.getPlayer().getSkinTexture();
                /*java.util.Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> skinMap = VoxelConstants.getMinecraft().getSkinProvider().getTextures(VoxelConstants.getPlayer().getGameProfile());
                if (skinMap.containsKey(MinecraftProfileTexture.Type.SKIN)) {
                    VoxelConstants.getMinecraft().getSkinProvider().loadSkin(skinMap.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
                }*/

                if (!this.worldName.equals(this.waypointManager.getCurrentWorldName())) {
                    this.worldName = this.waypointManager.getCurrentWorldName();
                }

                this.map.newWorld(this.world);
            }
        }

        VoxelConstants.tick();
        this.persistentMap.onTick();
    }

    public static void checkPermissionMessages(Text message) {
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
        this.waypointManager.setSubworldName(name, fromServer);
        this.map.newWorldName();
    }

    public String getWorldSeed() { return waypointManager.getWorldSeed().isEmpty() ? VoxelConstants.getWorldByKey(World.OVERWORLD).map(value -> Long.toString(((ServerWorld) value).getSeed())).orElse("") : waypointManager.getWorldSeed(); }

    public void setWorldSeed(String newSeed) { waypointManager.setWorldSeed(newSeed); }

    public void sendPlayerMessageOnMainThread(String s) {
        passMessage = s;
    }

    public WorldUpdateListener getWorldUpdateListener() {
        return this.worldUpdateListener;
    }
}
