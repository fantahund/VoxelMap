package com.mamiyaotaru.voxelmap;

import com.google.common.base.Charsets;
import com.mamiyaotaru.voxelmap.interfaces.AbstractVoxelMap;
import com.mamiyaotaru.voxelmap.interfaces.IColorManager;
import com.mamiyaotaru.voxelmap.interfaces.IDimensionManager;
import com.mamiyaotaru.voxelmap.interfaces.IMap;
import com.mamiyaotaru.voxelmap.interfaces.IPersistentMap;
import com.mamiyaotaru.voxelmap.interfaces.IRadar;
import com.mamiyaotaru.voxelmap.interfaces.ISettingsAndLightingChangeNotifier;
import com.mamiyaotaru.voxelmap.interfaces.IWaypointManager;
import com.mamiyaotaru.voxelmap.persistent.PersistentMap;
import com.mamiyaotaru.voxelmap.persistent.PersistentMapSettingsManager;
import com.mamiyaotaru.voxelmap.util.BiomeRepository;
import com.mamiyaotaru.voxelmap.util.DimensionManager;
import com.mamiyaotaru.voxelmap.util.GLUtils;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.MapUtils;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import com.mamiyaotaru.voxelmap.util.TickCounter;
import com.mamiyaotaru.voxelmap.util.WorldUpdateListener;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VoxelMap extends AbstractVoxelMap implements ResourceReloader {
    public static MapSettingsManager mapOptions = null;
    public static RadarSettingsManager radarOptions = null;
    private PersistentMapSettingsManager persistentMapOptions = null;
    private IMap map = null;
    private IRadar radar = null;
    private IRadar radarSimple = null;
    private PersistentMap persistentMap = null;
    private ISettingsAndLightingChangeNotifier settingsAndLightingChangeNotifier = null;
    private WorldUpdateListener worldUpdateListener = null;
    private IColorManager colorManager = null;
    private IWaypointManager waypointManager = null;
    private IDimensionManager dimensionManager = null;
    private ClientWorld world;
    private String worldName = "";
    private static String passMessage = null;

    private static final Logger logger = LogManager.getLogger("VoxelMap");

    public VoxelMap() {
        instance = this;
    }

    public void lateInit(boolean showUnderMenus, boolean isFair) {
        GLUtils.textureManager = MinecraftClient.getInstance().getTextureManager();
        mapOptions = new MapSettingsManager();
        mapOptions.showUnderMenus = showUnderMenus;
        radarOptions = new RadarSettingsManager();
        mapOptions.addSecondaryOptionsManager(radarOptions);
        this.persistentMapOptions = new PersistentMapSettingsManager();
        mapOptions.addSecondaryOptionsManager(this.persistentMapOptions);
        BiomeRepository.loadBiomeColors();
        this.colorManager = new ColorManager(this);
        this.waypointManager = new WaypointManager(this);
        this.dimensionManager = new DimensionManager(this);
        this.persistentMap = new PersistentMap(this);
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
                this.radar = new Radar(this);
                this.radarSimple = new RadarSimple(this);
            }
        } catch (Exception var4) {
            System.err.println("Failed creating radar " + var4.getLocalizedMessage());
            var4.printStackTrace();
            radarOptions.radarAllowed = false;
            radarOptions.radarMobsAllowed = false;
            radarOptions.radarPlayersAllowed = false;
            this.radar = null;
            this.radarSimple = null;
        }
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            radarOptions.radarAllowed = true;
            radarOptions.radarPlayersAllowed = radarOptions.radarAllowed;
            radarOptions.radarMobsAllowed = radarOptions.radarAllowed;
            mapOptions.cavesAllowed = true;
        });
        this.map = new Map(this);
        this.settingsAndLightingChangeNotifier = new SettingsAndLightingChangeNotifier();
        this.worldUpdateListener = new WorldUpdateListener();
        this.worldUpdateListener.addListener(this.map);
        this.worldUpdateListener.addListener(this.persistentMap);
        ReloadableResourceManagerImpl resourceManager = (ReloadableResourceManagerImpl) MinecraftClient.getInstance().getResourceManager();
        resourceManager.registerReloader(this);
        this.apply(resourceManager);
    }

    @Override
    public CompletableFuture<Void> reload(ResourceReloader.Synchronizer synchronizer, ResourceManager resourceManager, Profiler loadProfiler, Profiler applyProfiler, Executor loadExecutor, Executor applyExecutor) {
        return synchronizer.whenPrepared((Object) Unit.INSTANCE).thenRunAsync(() -> this.apply(resourceManager), applyExecutor);
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

    public void onTickInGame(MatrixStack matrixStack, MinecraftClient mc) {
        this.map.onTickInGame(matrixStack, mc);
        if (VoxelMap.passMessage != null) {
            mc.inGameHud.getChatHud().addMessage(Text.literal(VoxelMap.passMessage));
            VoxelMap.passMessage = null;
        }

    }

    public void onTick(MinecraftClient mc) {
        if (GameVariableAccessShim.getWorld() != null && !GameVariableAccessShim.getWorld().equals(this.world) || this.world != null && !this.world.equals(GameVariableAccessShim.getWorld())) {
            this.world = GameVariableAccessShim.getWorld();
            this.waypointManager.newWorld(this.world);
            this.persistentMap.newWorld(this.world);
            if (this.world != null) {
                MapUtils.reset();
                StringBuilder channelList = new StringBuilder();
                channelList.append("worldinfo:world_id");
                PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
                buffer.writeBytes(channelList.toString().getBytes(Charsets.UTF_8));
                mc.getNetworkHandler().sendPacket(new CustomPayloadC2SPacket(new Identifier("minecraft:register"), buffer));
                ByteBuf wIdRequestBuf = Unpooled.buffer(3);
                wIdRequestBuf.writeByte(0);
                wIdRequestBuf.writeByte(42);
                wIdRequestBuf.writeByte(0);
                mc.player.networkHandler.sendPacket(new CustomPayloadC2SPacket(new Identifier("worldinfo:world_id"), new PacketByteBuf(wIdRequestBuf)));
                mc.player.getSkinTexture();
                java.util.Map skinMap = mc.getSkinProvider().getTextures(mc.player.getGameProfile());
                if (skinMap.containsKey(Type.SKIN)) {
                    mc.getSkinProvider().loadSkin((MinecraftProfileTexture) skinMap.get(Type.SKIN), Type.SKIN);
                }

                if (!this.worldName.equals(this.waypointManager.getCurrentWorldName())) {
                    this.worldName = this.waypointManager.getCurrentWorldName();
                }

                this.map.newWorld(this.world);
            }
        }

        TickCounter.onTick();
        this.persistentMap.onTick(mc);
    }

    public static void checkPermissionMessages(Text message) {
        String msg = TextUtils.asFormattedString(message);
        msg = msg.replaceAll("§r", "");

        if (msg.contains("§3 §6 §3 §6 §3 §6 §d")) {
            mapOptions.cavesAllowed = false;
            getLogger().info("Server disabled cavemapping.");
        }

        if (msg.contains("§3 §6 §3 §6 §3 §6 §e")) {
            radarOptions.radarAllowed = false;
            radarOptions.radarPlayersAllowed = false;
            radarOptions.radarMobsAllowed = false;
            getLogger().info("Server disabled radar.");
        }

        if (msg.contains("§3 §6 §3 §6 §3 §6 §f")) {
            mapOptions.cavesAllowed = true;
            getLogger().info("Server enabled cavemapping.");
        }

        if (msg.contains("§3 §6 §3 §6 §3 §6 §0")) {
            radarOptions.radarAllowed = true;
            radarOptions.radarPlayersAllowed = true;
            radarOptions.radarMobsAllowed = true;
            getLogger().info("Server enabled radar.");
        }

    }

    @Override
    public MapSettingsManager getMapOptions() {
        return mapOptions;
    }

    @Override
    public RadarSettingsManager getRadarOptions() {
        return radarOptions;
    }

    @Override
    public PersistentMapSettingsManager getPersistentMapOptions() {
        return this.persistentMapOptions;
    }

    @Override
    public IMap getMap() {
        return this.map;
    }

    @Override
    public ISettingsAndLightingChangeNotifier getSettingsAndLightingChangeNotifier() {
        return this.settingsAndLightingChangeNotifier;
    }

    @Override
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

    @Override
    public IColorManager getColorManager() {
        return this.colorManager;
    }

    @Override
    public IWaypointManager getWaypointManager() {
        return this.waypointManager;
    }

    @Override
    public IDimensionManager getDimensionManager() {
        return this.dimensionManager;
    }

    @Override
    public IPersistentMap getPersistentMap() {
        return this.persistentMap;
    }

    @Override
    public void setPermissions(boolean hasFullRadarPermission, boolean hasPlayersOnRadarPermission, boolean hasMobsOnRadarPermission, boolean hasCavemodePermission) {
        radarOptions.radarAllowed = hasFullRadarPermission;
        radarOptions.radarPlayersAllowed = hasPlayersOnRadarPermission;
        radarOptions.radarMobsAllowed = hasMobsOnRadarPermission;
        mapOptions.cavesAllowed = hasCavemodePermission;
    }

    @Override
    public synchronized void newSubWorldName(String name, boolean fromServer) {
        this.waypointManager.setSubworldName(name, fromServer);
        this.map.newWorldName();
    }

    @Override
    public synchronized void newSubWorldHash(String hash) {
        this.waypointManager.setSubworldHash(hash);
    }

    @Override
    public String getWorldSeed() {
        if (MinecraftClient.getInstance().isIntegratedServerRunning()) {
            String seed = "";

            try {
                seed = Long.toString(MinecraftClient.getInstance().getServer().getWorld(World.OVERWORLD).getSeed());
            } catch (Exception var3) {
            }

            return seed;
        } else {
            return this.waypointManager.getWorldSeed();
        }
    }

    @Override
    public void setWorldSeed(String newSeed) {
        if (!MinecraftClient.getInstance().isIntegratedServerRunning()) {
            this.waypointManager.setWorldSeed(newSeed);
        }

    }

    @Override
    public void sendPlayerMessageOnMainThread(String s) {
        VoxelMap.passMessage = s;
    }

    @Override
    public WorldUpdateListener getWorldUpdateListener() {
        return this.worldUpdateListener;
    }

    public static Logger getLogger() {
        return logger;
    }
}
