package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.gui.GuiAddWaypoint;
import com.mamiyaotaru.voxelmap.gui.GuiWaypoints;
import com.mamiyaotaru.voxelmap.gui.GuiWelcomeScreen;
import com.mamiyaotaru.voxelmap.interfaces.AbstractMapData;
import com.mamiyaotaru.voxelmap.interfaces.IChangeObserver;
import com.mamiyaotaru.voxelmap.interfaces.IReloadListener;
import com.mamiyaotaru.voxelmap.options.ServerSettingsManager;
import com.mamiyaotaru.voxelmap.options.containers.MapOptions;
import com.mamiyaotaru.voxelmap.options.containers.WaypointOptions;
import com.mamiyaotaru.voxelmap.options.enums.OptionEnumMinimap;
import com.mamiyaotaru.voxelmap.persistent.gui.GuiPersistentMap;
import com.mamiyaotaru.voxelmap.render.CachedOrthoProjection;
import com.mamiyaotaru.voxelmap.render.DeferredRenderPass;
import com.mamiyaotaru.voxelmap.render.RenderUtils;
import com.mamiyaotaru.voxelmap.render.VoxelMapPipelines;
import com.mamiyaotaru.voxelmap.render.VoxelMapRenderTarget;
import com.mamiyaotaru.voxelmap.textures.DynamicMutableTexture;
import com.mamiyaotaru.voxelmap.textures.ScaledDynamicMutableTexture;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.BiomeRepository;
import com.mamiyaotaru.voxelmap.util.BlockRepository;
import com.mamiyaotaru.voxelmap.util.CPULightmap;
import com.mamiyaotaru.voxelmap.util.ColorUtils;
import com.mamiyaotaru.voxelmap.util.Contact;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.FullMapData;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.MapChunkCache;
import com.mamiyaotaru.voxelmap.util.MapUtils;
import com.mamiyaotaru.voxelmap.util.MinimapContext;
import com.mamiyaotaru.voxelmap.util.MutableBlockPos;
import com.mamiyaotaru.voxelmap.util.MutableBlockPosCache;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.math.Axis;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.OutOfMemoryScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Random;
import java.util.TreeSet;

public class Map implements Runnable, IChangeObserver, IReloadListener {
    private final Minecraft minecraft = Minecraft.getInstance();
    private final ServerSettingsManager serverSettings;
    private final MapOptions options;
    private final WaypointOptions waypointOptions;
    private final ColorManager colorManager;
    private final WaypointManager waypointManager;
    private final MinimapContext minimapContext;
    private final Random generator = new Random();

    // Map UI
    private static final float MAP_IMAGE_DEPTH = 0.0F;
    private static final float MAP_OVERLAY_DEPTH = 100.0F;
    private static final float MAP_TEXT_DEPTH = 200.0F;
    private AbstractTexture minimapArrowTexture;
    private AbstractTexture squareMapFrameTexture;
    private AbstractTexture squareMapStencilTexture;
    private AbstractTexture roundMapFrameTexture;
    private AbstractTexture roundMapStencilTexture;
    private int scWidth;
    private int scHeight;
    private String message = "";
    private long messageTime;
    private static double minTablistOffset;
    private static float statusIconOffset = 0.0F;

    // Map Data
    private final FullMapData[] mapData = new FullMapData[5];
    private final MapChunkCache[] chunkCache = new MapChunkCache[5];
    private DynamicMutableTexture[] mapImages;
    private final DynamicMutableTexture[] mapImagesFiltered = new DynamicMutableTexture[5];
    private final DynamicMutableTexture[] mapImagesUnfiltered = new DynamicMutableTexture[5];

    // Map Core Calculation
    private final int availableProcessors = Runtime.getRuntime().availableProcessors();
    private final boolean multicore = this.availableProcessors > 1;
    private final boolean threading = this.multicore;
    private final Object zCalcLock = new Object();
    private volatile boolean zCalcRunning = true;
    private Thread zCalc = createZCalcThread();
    private int zCalcTicker;
    private final Object coordinateLock = new Object();
    private ClientLevel world;
    private int updateTimer;
    private boolean doFullRender = true;
    private boolean imageChanged = true;

    // Map Terrain Calculation
    private final int heightMapResetHeight = this.multicore ? 2 : 5;
    private final int heightMapResetTime = this.multicore ? 300 : 3000;
    private int heightMapFudge;
    private boolean lastBeneathRendering;
    private BlockState transparentBlockState;
    private BlockState surfaceBlockState;

    // Map Player Calculation
    private boolean zoomChanged;
    private int zoom;
    private double zoomScale = 1.0;
    private double zoomScaleAdjusted = 1.0;
    private int lastX;
    private int lastZ;
    private int lastY;
    private int lastImageX;
    private int lastImageZ;
    private float direction;
    private int rotationFactor;
    private boolean fullscreenMap;
    private boolean lastFullscreen;
    private Screen lastGuiScreen;
    private boolean optionsChanged;

    // Map Light Calculation
    private boolean needLightmapRefresh = true;
    private int tickWithLightChange;
    private final int[] lightmapColors = new int[256];
    private final int[] lastLightmapValues = new int[16];
    private boolean needSkyColor;
    private int lastSkyColor;
    private double lastGamma;
    private float lastSunBrightness;
    private float lastLightning;
    private float lastNightVision;
    private boolean lastPaused = true;
    private boolean lastAboveHorizon = true;
    private int lastBiome;

    // Map Rendering
    private final CachedOrthoProjection mapProjection;
    private final CachedOrthoProjection hudProjection;
    private final VoxelMapRenderTarget baseMapRenderTarget; // Used for minimap rendering before masking
    private final VoxelMapRenderTarget finalMapRenderTarget; // Used for minimap rendering after masking

    public Map() {
        this.serverSettings = VoxelConstants.getVoxelMapInstance().getServerSettings();
        this.options = VoxelConstants.getVoxelMapInstance().getMapOptions();
        this.waypointOptions = VoxelConstants.getVoxelMapInstance().getWaypointOptions();
        this.colorManager = VoxelConstants.getVoxelMapInstance().getColorManager();
        this.waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        this.minimapContext = new MinimapContext();
        ArrayList<KeyMapping> tempBindings = new ArrayList<>();
        tempBindings.addAll(Arrays.asList(minecraft.options.keyMappings));
        tempBindings.addAll(Arrays.asList(this.options.keyBindings));
        minecraft.options.keyMappings = tempBindings.toArray(new KeyMapping[0]);

        this.zCalc.start();

        for (int i = 0; i < this.mapData.length; i++) {
            int resolution = 1 << (i + 5); // 32, 64, ...
            int chunks = (1 << (i + 1)) + 1; //3, 5, ...

            this.mapData[i] = new FullMapData(resolution, resolution);
            this.chunkCache[i] = new MapChunkCache(chunks, chunks, this);

            this.mapImagesFiltered[i] = new DynamicMutableTexture(String.format("voxelmap-map-%s", resolution), resolution, resolution, true);
            this.mapImagesFiltered[i].sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);

            this.mapImagesUnfiltered[i] = new ScaledDynamicMutableTexture(String.format("voxelmap-map-unfiltered-%s", resolution), resolution, resolution, true);
            this.mapImagesUnfiltered[i].sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);

        }

        if (this.options.filtering.get()) {
            this.mapImages = this.mapImagesFiltered;
        } else {
            this.mapImages = this.mapImagesUnfiltered;
        }

        this.zoom = this.options.zoom.get();
        this.setZoomScale();

        this.mapProjection = new CachedOrthoProjection("VoxelMap Minimap Projection", 1000.0F, 21000.0F, true);
        this.hudProjection = new CachedOrthoProjection("VoxelMap HUD Projection", 1000.0F, 21000.0F, true);

        final int fboTextureSize = 512;

        this.baseMapRenderTarget = new VoxelMapRenderTarget("VoxelMap Base Map Target", true, GpuFormat.RGBA8_UNORM);
        this.baseMapRenderTarget.createBuffers(fboTextureSize, fboTextureSize);

        this.finalMapRenderTarget = new VoxelMapRenderTarget("VoxelMap Final Map Target", true, GpuFormat.RGBA8_UNORM);
        this.finalMapRenderTarget.createBuffers(fboTextureSize, fboTextureSize);

        VoxelConstants.getVoxelMapInstance().addReloadListener(this);
    }

    private Thread createZCalcThread() {
        Thread thread = new Thread(this, "Voxelmap LiveMap Calculation Thread");
        thread.setDaemon(true);
        return thread;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        minimapArrowTexture = minecraft.getTextureManager().getTexture(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "images/minimap/minimap_arrow.png"));
        squareMapFrameTexture = minecraft.getTextureManager().getTexture(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "images/minimap/square_map_frame.png"));
        squareMapStencilTexture = minecraft.getTextureManager().getTexture(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "images/minimap/square_map_stencil.png"));
        roundMapFrameTexture = minecraft.getTextureManager().getTexture(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "images/minimap/round_map_frame.png"));
        roundMapStencilTexture = minecraft.getTextureManager().getTexture(Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "images/minimap/round_map_stencil.png"));

        squareMapStencilTexture.sampler = VoxelMapPipelines.LINEAR_CLAMP_SAMPLER;
        roundMapStencilTexture.sampler = VoxelMapPipelines.LINEAR_CLAMP_SAMPLER;

        boolean arrowFiltering = Boolean.parseBoolean(VoxelConstants.getVoxelMapInstance().getImageProperties().getProperty("minimapArrowFiltering", "true"));
        minimapArrowTexture.sampler = arrowFiltering ? VoxelMapPipelines.LINEAR_CLAMP_SAMPLER : VoxelMapPipelines.NEAREST_CLAMP_SAMPLER;

        boolean frameFiltering = Boolean.parseBoolean(VoxelConstants.getVoxelMapInstance().getImageProperties().getProperty("minimapFrameFiltering", "true"));
        squareMapFrameTexture.sampler = frameFiltering ? VoxelMapPipelines.LINEAR_CLAMP_SAMPLER : VoxelMapPipelines.NEAREST_CLAMP_SAMPLER;
        roundMapFrameTexture.sampler = frameFiltering ? VoxelMapPipelines.LINEAR_CLAMP_SAMPLER : VoxelMapPipelines.NEAREST_CLAMP_SAMPLER;
    }

    public void forceFullRender(boolean forceFullRender) {
        this.doFullRender = forceFullRender;
        VoxelConstants.getVoxelMapInstance().getSettingsAndLightingChangeNotifier().notifyOfChanges();
    }

    public void optionsChanged() {
        optionsChanged = true;
    }

    private boolean isMapEnabled() {
        return !options.hide.get() && serverSettings.minimapAllowed.get();
    }

    private boolean isRotationEnabled() {
        return options.rotates.get() && !fullscreenMap;
    }

    private boolean isSlopeEnabled() {
        return options.terrainDepth.get() == OptionEnumMinimap.TerrainDepth.SLOPE_MAP || options.terrainDepth.get() == OptionEnumMinimap.TerrainDepth.BOTH;
    }

    private boolean isHeightEnabled() {
        return options.terrainDepth.get() == OptionEnumMinimap.TerrainDepth.HEIGHT_MAP || options.terrainDepth.get() == OptionEnumMinimap.TerrainDepth.BOTH;
    }

    private int getMapScale() {
        if (fullscreenMap) {
            return 0;
        }
        return switch (options.sizeModifier.get()) {
            case SMALL -> -1;
            case MEDIUM -> 0;
            case LARGE -> 1;
            case XL -> 2;
            case XXL -> 3;
            case XXXL -> 4;
        };
    }

    private float getMapImageScale() {
        return options.rotates.get() && options.squareMap.get() ? 1.4142F : 1.0F;
    }

    @Override
    public void run() {
        while (this.zCalcRunning) {
            if (this.world != null) {
                if (isMapEnabled()) {
                    try {
                        this.mapCalc(this.doFullRender);
                        if (!this.doFullRender) {
                            MutableBlockPos blockPos = MutableBlockPosCache.get();
                            this.chunkCache[this.zoom].centerChunks(blockPos.withXYZ(this.lastX, 0, this.lastZ));
                            MutableBlockPosCache.release(blockPos);
                            this.chunkCache[this.zoom].checkIfChunksChanged();
                        }
                    } catch (Exception exception) {
                        VoxelConstants.getLogger().error("Voxelmap LiveMap Calculation Thread", exception);
                    }
                }

                this.doFullRender = this.zoomChanged;
                this.zoomChanged = false;
            }

            this.zCalcTicker = 0;
            synchronized (this.zCalcLock) {
                try {
                    this.zCalcLock.wait(0L);
                } catch (InterruptedException exception) {
                    if (this.zCalcRunning) {
                        VoxelConstants.getLogger().error("Voxelmap LiveMap Calculation Thread", exception);
                    }
                }
            }
        }
    }

    public void shutdown() {
        this.zCalcRunning = false;
        Thread thread = this.zCalc;
        synchronized (this.zCalcLock) {
            this.zCalcLock.notifyAll();
        }
        thread.interrupt();

        try {
            thread.join(5000L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }

        if (thread.isAlive()) {
            VoxelConstants.getLogger().warn("Voxelmap LiveMap Calculation Thread did not stop within shutdown timeout");
        }
    }

    public void newWorld(ClientLevel world) {
        this.world = world;
        this.mapData[this.zoom].blank();
        this.doFullRender = true;
        VoxelConstants.getVoxelMapInstance().getSettingsAndLightingChangeNotifier().notifyOfChanges();
    }

    public void newWorldName() {
        String subworldName = this.waypointManager.getCurrentSubworldDescriptor(true);
        StringBuilder subworldNameBuilder = (new StringBuilder("§r")).append(I18n.get("worldmap.multiworld.newWorld")).append(":").append(" ");
        if (subworldName.isEmpty() && this.waypointManager.isMultiworld()) {
            subworldNameBuilder.append("???");
        } else if (!subworldName.isEmpty()) {
            subworldNameBuilder.append(subworldName);
        }

        this.showMessage(subworldNameBuilder.toString());
    }

    public void onTickInGame(GuiGraphicsExtractor graphics) {
        this.rotationFactor = this.options.oldNorth.get() ? 90 : 0;

        if (minecraft.gui.screen() == null) {
            if (options.welcome.get()) {
                minecraft.gui.setScreen(new GuiWelcomeScreen(null));
            }
            if (options.keyBindMenu.consumeClick()) {
                minecraft.gui.setScreen(new GuiPersistentMap(null));
            }
            if (options.keyBindMobToggle.consumeClick()) {
                VoxelConstants.getVoxelMapInstance().getRadarOptions().showRadar.cycle();
            }
            if (options.keyBindWaypointToggle.consumeClick()) {
                options.toggleInGameWaypoints();
            }
            if (options.keyBindZoom.consumeClick()) {
                cycleZoomLevel();
            }
            if (options.keyBindFullscreen.consumeClick()) {
                fullscreenMap = !fullscreenMap;
                showMessage(I18n.get("minimap.ui.zoomLevel", 2.0 / zoomScale));
            }
            if (options.keyBindMinimapToggle.consumeClick()) {
                options.hide.cycle();
            }
            if (serverSettings.waypointsAllowed.get()) {
                if (options.keyBindWaypointMenu.consumeClick()) {
                    minecraft.gui.setScreen(new GuiWaypoints(null));
                }
                if (options.keyBindWaypoint.consumeClick()) {
                    boolean isFirst = waypointManager.getWaypoints().isEmpty();
                    double scale = VoxelConstants.getPlayer().level().dimensionType().coordinateScale();

                    int x = (int) (GameVariableAccessShim.xCoord() * scale);
                    int z = (int) (GameVariableAccessShim.zCoord() * scale);
                    int y = GameVariableAccessShim.yCoord();
                    float r = isFirst ? 0.0F : generator.nextFloat();
                    float g = isFirst ? 1.0F : generator.nextFloat();
                    float b = isFirst ? 0.0F : generator.nextFloat();

                    TreeSet<DimensionContainer> dimensions = new TreeSet<>();
                    dimensions.add(VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getPlayer().level()));
                    Waypoint waypoint = new Waypoint("", x, z, y, true, r, g, b, "", waypointManager.getCurrentSubworldDescriptor(false), dimensions);

                    minecraft.gui.setScreen(new GuiAddWaypoint(null, waypoint, false));
                }
            }
        } else {
            if (serverSettings.deathpointsAllowd.get()) {
                if (minecraft.gui.screen() instanceof DeathScreen && !(lastGuiScreen instanceof DeathScreen)) {
                    waypointManager.handleDeath();
                }
            }
        }
        this.lastGuiScreen = minecraft.gui.screen();

        this.checkForChanges();
        this.calculateCurrentLightAndSkyColor();

        if (this.threading) {
            if (this.zCalcRunning && !this.zCalc.isAlive()) {
                this.zCalc = createZCalcThread();
                this.zCalc.start();
                this.zCalcTicker = 0;
            }

            if (this.zCalcRunning && !(minecraft.gui.screen() instanceof DeathScreen) && !(minecraft.gui.screen() instanceof OutOfMemoryScreen)) {
                ++this.zCalcTicker;
                if (this.zCalcTicker > 2000) {
                    this.zCalcTicker = 0;
                    Exception ex = new Exception();
                    ex.setStackTrace(this.zCalc.getStackTrace());
                    DebugRenderState.print();
                    VoxelConstants.getLogger().error("Voxelmap LiveMap Calculation Thread is hanging?", ex);
                }
                synchronized (this.zCalcLock) {
                    this.zCalcLock.notifyAll();
                }
            }
        } else {
            if (isMapEnabled() && this.world != null) {
                this.mapCalc(this.doFullRender);
                if (!this.doFullRender) {
                    MutableBlockPos blockPos = MutableBlockPosCache.get();
                    this.chunkCache[this.zoom].centerChunks(blockPos.withXYZ(this.lastX, 0, this.lastZ));
                    MutableBlockPosCache.release(blockPos);
                    this.chunkCache[this.zoom].checkIfChunksChanged();
                }
            }

            this.doFullRender = false;
        }

        this.direction = GameVariableAccessShim.rotationYaw() + 180.0F;

        while (this.direction >= 360.0F) {
            this.direction -= 360.0F;
        }

        while (this.direction < 0.0F) {
            this.direction += 360.0F;
        }

        if (!this.message.isEmpty() && (System.currentTimeMillis() - this.messageTime > 3000L)) {
            this.message = "";
        }

        if (!minecraft.gui.hud.isHidden() && (minecraft.gui.screen() == null || options.showUnderMenus.get())) {
            this.drawMinimap(graphics);
        }

        this.updateTimer = this.updateTimer > 5000 ? 0 : this.updateTimer + 1;
    }

    private void cycleZoomLevel() {
        if (--this.zoom < 0) {
            this.zoom = 4;
        }
        this.options.zoom.set(this.zoom);
        this.zoomChanged = true;
        this.setZoomScale();
        this.doFullRender = true;
        this.showMessage(I18n.get("minimap.ui.zoomLevel", 2.0 / this.zoomScale));

    }

    private void setZoomScale() {
        this.zoomScale = Math.pow(2.0, this.zoom) / 2.0;
        this.zoomScaleAdjusted = this.zoomScale / getMapImageScale();
    }

    public void calculateCurrentLightAndSkyColor() {
        try {
            if (this.world != null) {
                if (this.needLightmapRefresh && VoxelConstants.getElapsedTicks() != this.tickWithLightChange && !minecraft.isPaused()) {
                    this.needLightmapRefresh = false;
                    CPULightmap lightmap = CPULightmap.getInstance();
                    lightmap.setup();
                    for (int blockLight = 0; blockLight < 16; blockLight++) {
                        for (int skyLight = 0; skyLight < 16; skyLight++) {
                            this.lightmapColors[blockLight + skyLight * 16] = lightmap.getLight(blockLight, skyLight);
                        }
                    }
                }

                boolean lightChanged = false;
                if (minecraft.options.gamma().get() != this.lastGamma) {
                    lightChanged = true;
                    this.lastGamma = minecraft.options.gamma().get();
                }

                float sunBrightness = 1 - (this.world.getSkyDarken() / 15f);
                if (Math.abs(this.lastSunBrightness - sunBrightness) > 0.01 || sunBrightness == 1.0 && sunBrightness != this.lastSunBrightness || sunBrightness == 0.0 && sunBrightness != this.lastSunBrightness) {
                    lightChanged = true;
                    this.needSkyColor = true;
                    this.lastSunBrightness = sunBrightness;
                }

                float nightVision = 0.0F;
                if (VoxelConstants.getPlayer().hasEffect(MobEffects.NIGHT_VISION)) {
                    int duration = VoxelConstants.getPlayer().getEffect(MobEffects.NIGHT_VISION).getDuration();
                    nightVision = duration > 200 ? 1.0F : 0.7F + Mth.sin((duration - 1.0F) * (float) Math.PI * 0.2F) * 0.3F;
                }

                if (this.lastNightVision != nightVision) {
                    this.lastNightVision = nightVision;
                    lightChanged = true;
                }

                int lastLightningBolt = this.world.getSkyFlashTime();
                if (this.lastLightning != lastLightningBolt) {
                    this.lastLightning = lastLightningBolt;
                    lightChanged = true;
                }

                if (this.lastPaused != minecraft.isPaused()) {
                    this.lastPaused = !this.lastPaused;
                    lightChanged = true;
                }

                boolean scheduledUpdate = (this.updateTimer - 50) % 50 == 0;
                if (lightChanged || scheduledUpdate) {
                    this.tickWithLightChange = VoxelConstants.getElapsedTicks();
                    this.needLightmapRefresh = true;
                }

                boolean aboveHorizon = VoxelConstants.getPlayer().getEyePosition(0.0F).y >= this.world.getLevelData().getHorizonHeight(this.world);
                if (this.world.dimension().identifier().toString().toLowerCase().contains("ether")) {
                    aboveHorizon = true;
                }

                if (aboveHorizon != this.lastAboveHorizon) {
                    this.needSkyColor = true;
                    this.lastAboveHorizon = aboveHorizon;
                }

                MutableBlockPos blockPos = MutableBlockPosCache.get();
                int biomeID = this.world.registryAccess().lookupOrThrow(Registries.BIOME).getId(this.world.getBiome(blockPos.withXYZ(GameVariableAccessShim.xCoord(), GameVariableAccessShim.yCoord(), GameVariableAccessShim.zCoord())).value());
                MutableBlockPosCache.release(blockPos);
                if (biomeID != this.lastBiome) {
                    this.needSkyColor = true;
                    this.lastBiome = biomeID;
                }

                if (this.needSkyColor || scheduledUpdate) {
                    this.colorManager.setSkyColor(this.getSkyColor());
                }
            }
        } catch (NullPointerException ignore) {

        }
    }

    private int getSkyColor() {
        this.needSkyColor = false;
        boolean aboveHorizon = this.lastAboveHorizon;
        Vector4f color = new Vector4f();
        minecraft.gameRenderer.fogRenderer.computeFogColor(minecraft.gameRenderer.mainCamera(), 0.0F, this.world, minecraft.options.renderDistance().get(), minecraft.gameRenderer.bossOverlayWorldDarkening(0.0F), color);
        int r = (int) (color.x * 255.0F);
        int g = (int) (color.y * 255.0F);
        int b = (int) (color.z * 255.0F);
        if (!aboveHorizon) {
            return 0x0A000000 | (r << 16) | (g << 8) | b;
        } else {
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    }

    public int[] getLightmapArray() {
        return this.lightmapColors;
    }

    public int getLightmapColor(int skyLight, int blockLight) {
        if (this.lightmapColors == null) {
            return 0;
        }
        return ARGB.toABGR(this.lightmapColors[blockLight + skyLight * 16]);
    }

    public void drawMinimap(GuiGraphicsExtractor graphics) {
        int scScaleOrig = 1;

        while (minecraft.getWindow().getWidth() / (scScaleOrig + 1) >= 320 && minecraft.getWindow().getHeight() / (scScaleOrig + 1) >= 240) {
            ++scScaleOrig;
        }

        int scScale = Math.max(1, scScaleOrig + getMapScale());
        double scaledWidthD = (double) minecraft.getWindow().getWidth() / scScale;
        double scaledHeightD = (double) minecraft.getWindow().getHeight() / scScale;
        this.scWidth = Mth.ceil(scaledWidthD);
        this.scHeight = Mth.ceil(scaledHeightD);
        float scaleProj = (float) (scScale) / minecraft.getWindow().getGuiScale();

        int mapX = 0;
        int mapY = 0;
        switch (options.mapCorner.get()) {
            case TOP_LEFT -> {
                mapX = 37;
                mapY = 37;
            }
            case TOP_RIGHT -> {
                mapX = scWidth - 37;
                mapY = 37;
            }
            case BOTTOM_RIGHT -> {
                mapX = scWidth - 37;
                mapY = scHeight - 37;
            }
            case BOTTOM_LEFT -> {
                mapX = 37;
                mapY = scHeight - 37;
            }
        }
        float statusIconOffset = 0.0F;
        if (options.moveMapBelowStatusEffectIcons.get() && options.mapCorner.get() == OptionEnumMinimap.Location.TOP_RIGHT) {
            for (MobEffectInstance effect : VoxelConstants.getPlayer().getActiveEffects()) {
                if (effect.showIcon()) {
                    if (!effect.getEffect().value().isBeneficial()) {
                        statusIconOffset = 50.0F;
                        break;
                    }
                    statusIconOffset = 24.0F;
                }
            }
            float resFactor = (float) scHeight / minecraft.getWindow().getGuiScaledHeight();
            mapY += (int) (statusIconOffset * resFactor);
        }
        Map.statusIconOffset = statusIconOffset;

        this.minimapContext.updateVars(
                GameVariableAccessShim.xCoordDouble(),
                GameVariableAccessShim.yCoordDouble(),
                GameVariableAccessShim.zCoordDouble(),
                isRotationEnabled() ? this.direction : -this.rotationFactor,
                this.zoomScale,
                this.zoomScaleAdjusted
        );

        RenderTarget fullscreenTarget = RenderUtils.getFullscreenTarget();

        RenderUtils.setProjectionMatrix(hudProjection.getBuffer(RenderUtils.getGuiWidth(), RenderUtils.getGuiHeight()), ProjectionType.ORTHOGRAPHIC, -2000.0F);
        try (DeferredRenderPass pass = RenderUtils.createDeferredRenderPass("VoxelMap Map Draw", fullscreenTarget.getColorTextureView(), Optional.of(new Vector4f(0.0F, 0.0F, 0.0F, 0.0F)), fullscreenTarget.getDepthTextureView(), OptionalDouble.of(0.0))) {
            Matrix4fStack matrixStack = RenderUtils.getMatrixStack();
            matrixStack.pushMatrix();
            matrixStack.identity();
            if (!options.hide.get()) {
                if (fullscreenMap) {
                    renderMapFull(matrixStack, pass, scWidth / 2, scHeight / 2, scaleProj);
                    drawArrow(matrixStack, pass, scWidth / 2, scHeight / 2, scaleProj);
                } else {
                    renderMap(matrixStack, pass, mapX, mapY, scScale, scaleProj);
                    drawArrow(matrixStack, pass, mapX, mapY, scaleProj);
                    drawDirections(matrixStack, pass, mapX, mapY, scaleProj);
                }
            }
            showCoords(matrixStack, pass, mapX, mapY, scaleProj);
            matrixStack.popMatrix();
        }
        RenderUtils.restoreProjectionMatrix();
        RenderUtils.blitRenderTarget(graphics, fullscreenTarget);
    }

    private void checkForChanges() {
        boolean changed = colorManager.checkForChanges();

        if (optionsChanged) {
            if (options.filtering.get()) {
                mapImages = mapImagesFiltered;
            } else {
                mapImages = mapImagesUnfiltered;
            }
            changed = true;
            setZoomScale();
        }
        optionsChanged = false;

        if (changed) {
            doFullRender = true;
            VoxelConstants.getVoxelMapInstance().getSettingsAndLightingChangeNotifier().notifyOfChanges();
        }
    }

    private void mapCalc(boolean full) {
        int currentX = GameVariableAccessShim.xCoord();
        int currentZ = GameVariableAccessShim.zCoord();
        int currentY = GameVariableAccessShim.yCoord();
        int offsetX = currentX - this.lastX;
        int offsetZ = currentZ - this.lastZ;
        int offsetY = currentY - this.lastY;
        int zoom = this.zoom;
        int multi = (int) Math.pow(2.0, zoom);
        ClientLevel world = this.world;
        boolean needHeightAndID;
        boolean needHeightMap = false;
        boolean needLight = false;
        boolean skyColorChanged = false;
        int skyColor = this.colorManager.getAirColor();
        if (this.lastSkyColor != skyColor) {
            skyColorChanged = true;
            this.lastSkyColor = skyColor;
        }

        if (this.options.dynamicLighting.get()) {
            for (int t = 0; t < 16; ++t) {
                int newValue = getLightmapColor(t, 0);
                if (this.lastLightmapValues[t] != newValue) {
                    needLight = true;
                    this.lastLightmapValues[t] = newValue;
                }
            }
        }

        if (offsetY != 0) {
            ++this.heightMapFudge;
        } else if (this.heightMapFudge != 0) {
            ++this.heightMapFudge;
        }

        if (full || Math.abs(offsetY) >= this.heightMapResetHeight || this.heightMapFudge > this.heightMapResetTime) {
            if (this.lastY != currentY) {
                needHeightMap = true;
            }

            this.lastY = currentY;
            this.heightMapFudge = 0;
        }

        if (Math.abs(offsetX) > 32 * multi || Math.abs(offsetZ) > 32 * multi) {
            full = true;
        }

        MutableBlockPos blockPos = MutableBlockPosCache.get();
        blockPos.setXYZ(this.lastX, Math.max(Math.min(GameVariableAccessShim.yCoord(), world.getMaxY() - 1), world.getMinY()), this.lastZ);
        boolean caves = serverSettings.cavesAllowed.get() && MapUtils.isUnderground(world, blockPos, currentY);
        MutableBlockPosCache.release(blockPos);

        boolean beneathRendering = caves;
        if (this.lastBeneathRendering != beneathRendering) {
            full = true;
        }

        this.lastBeneathRendering = beneathRendering;
        needHeightAndID = needHeightMap && caves;
        int color24;
        synchronized (this.coordinateLock) {
            if (!full) {
                this.mapImages[zoom].moveY(offsetZ);
                this.mapImages[zoom].moveX(offsetX);
            }

            this.lastX = currentX;
            this.lastZ = currentZ;
        }
        int startX = currentX - 16 * multi;
        int startZ = currentZ - 16 * multi;
        if (!full) {
            this.mapData[zoom].moveZ(offsetZ);
            this.mapData[zoom].moveX(offsetX);

            for (int imageY = offsetZ > 0 ? 32 * multi - 1 : -offsetZ - 1; imageY >= (offsetZ > 0 ? 32 * multi - offsetZ : 0); --imageY) {
                for (int imageX = 0; imageX < 32 * multi; ++imageX) {
                    color24 = this.getPixelColor(true, true, true, true, caves, world, zoom, multi, startX, startZ, imageX, imageY);
                    this.mapImages[zoom].setRGB(imageX, imageY, color24);
                }
            }

            for (int imageY = 32 * multi - 1; imageY >= 0; --imageY) {
                for (int imageX = offsetX > 0 ? 32 * multi - offsetX : 0; imageX < (offsetX > 0 ? 32 * multi : -offsetX); ++imageX) {
                    color24 = this.getPixelColor(true, true, true, true, caves, world, zoom, multi, startX, startZ, imageX, imageY);
                    this.mapImages[zoom].setRGB(imageX, imageY, color24);
                }
            }
        }

        if (full || isHeightEnabled() && needHeightMap || needHeightAndID || this.options.dynamicLighting.get() && needLight || skyColorChanged) {
            for (int imageY = 32 * multi - 1; imageY >= 0; --imageY) {
                for (int imageX = 0; imageX < 32 * multi; ++imageX) {
                    color24 = this.getPixelColor(full, full || needHeightAndID, full, full || needLight || needHeightAndID, caves, world, zoom, multi, startX, startZ, imageX, imageY);
                    this.mapImages[zoom].setRGB(imageX, imageY, color24);
                }
            }
        }

        if ((full || offsetX != 0 || offsetZ != 0 || !this.lastFullscreen) && this.fullscreenMap && this.options.biomeOverlay.get() != OptionEnumMinimap.BiomeOverlay.OFF) {
            this.mapData[zoom].segmentBiomes();
            this.mapData[zoom].findCenterOfSegments(!this.options.oldNorth.get());
        }

        this.lastFullscreen = this.fullscreenMap;
        if (full || offsetX != 0 || offsetZ != 0 || needHeightMap || needLight || skyColorChanged) {
            this.imageChanged = true;
        }

        if (needLight || skyColorChanged) {
            VoxelConstants.getVoxelMapInstance().getSettingsAndLightingChangeNotifier().notifyOfChanges();
        }

    }

    @Override
    public void handleChangeInWorld(int chunkX, int chunkZ) {
        try {
            this.chunkCache[this.zoom].registerChangeAt(chunkX, chunkZ);
        } catch (Exception e) {
            VoxelConstants.getLogger().warn(e);
        }
    }

    @Override
    public void processChunk(LevelChunk chunk) {
        this.rectangleCalc(chunk.getPos().x() * 16, chunk.getPos().z() * 16, chunk.getPos().x() * 16 + 15, chunk.getPos().z() * 16 + 15);
    }

    private void rectangleCalc(int left, int top, int right, int bottom) {
        MutableBlockPos blockPos = MutableBlockPosCache.get();
        blockPos.setXYZ(this.lastX, Math.max(Math.min(GameVariableAccessShim.yCoord(), world.getMaxY()), world.getMinY()), this.lastZ);
        int currentY = GameVariableAccessShim.yCoord();
        boolean caves = serverSettings.cavesAllowed.get() && MapUtils.isUnderground(world, blockPos, currentY);
        MutableBlockPosCache.release(blockPos);

        int zoom = this.zoom;
        int startX = this.lastX;
        int startZ = this.lastZ;
        ClientLevel world = this.world;
        int multi = (int) Math.pow(2.0, zoom);
        startX -= 16 * multi;
        startZ -= 16 * multi;
        left = left - startX - 1;
        right = right - startX + 1;
        top = top - startZ - 1;
        bottom = bottom - startZ + 1;
        left = Math.max(0, left);
        right = Math.min(32 * multi - 1, right);
        top = Math.max(0, top);
        bottom = Math.min(32 * multi - 1, bottom);
        int color24;

        for (int imageY = bottom; imageY >= top; --imageY) {
            for (int imageX = left; imageX <= right; ++imageX) {
                color24 = this.getPixelColor(true, true, true, true, caves, world, zoom, multi, startX, startZ, imageX, imageY);
                this.mapImages[zoom].setRGB(imageX, imageY, color24);
            }
        }

        this.imageChanged = true;
    }

    private int getPixelColor(boolean needBiome, boolean needHeightAndID, boolean needTint, boolean needLight, boolean caves, ClientLevel world, int zoom, int multi, int startX, int startZ, int imageX, int imageY) {
        int surfaceHeight = Short.MIN_VALUE;
        int seafloorHeight = Short.MIN_VALUE;
        int transparentHeight = Short.MIN_VALUE;
        int foliageHeight = Short.MIN_VALUE;
        int surfaceColor;
        int seafloorColor = 0;
        int transparentColor = 0;
        int foliageColor = 0;
        this.surfaceBlockState = null;
        this.transparentBlockState = BlockRepository.air.defaultBlockState();
        BlockState foliageBlockState = BlockRepository.air.defaultBlockState();
        BlockState seafloorBlockState = BlockRepository.air.defaultBlockState();
        boolean surfaceBlockChangeForcedTint = false;
        boolean transparentBlockChangeForcedTint = false;
        boolean foliageBlockChangeForcedTint = false;
        boolean seafloorBlockChangeForcedTint = false;
        int surfaceBlockStateID;
        int transparentBlockStateID;
        int foliageBlockStateID;
        int seafloorBlockStateID;
        MutableBlockPos blockPos = MutableBlockPosCache.get();
        MutableBlockPos tempBlockPos = MutableBlockPosCache.get();
        blockPos.withXYZ(startX + imageX, 64, startZ + imageY);
        int color24;
        Biome biome;
        if (needBiome) {
            // int chunkX = SectionPos.blockToSectionCoord(blockPos.getX());
            // int chunkZ = SectionPos.blockToSectionCoord(blockPos.getZ());
            // if (world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) != null) { // TODO 1.21.5 testen
            biome = world.getBiome(blockPos).value();
            // } else {
            // biome = null;
            // }

            this.mapData[zoom].setBiome(imageX, imageY, biome);
        } else {
            biome = this.mapData[zoom].getBiome(imageX, imageY);
        }

        if (this.options.biomeOverlay.get() == OptionEnumMinimap.BiomeOverlay.SOLID) {
            if (biome != null) {
                color24 = ARGB.toABGR(BiomeRepository.getBiomeColor(biome) | 0xFF000000);
            } else {
                color24 = 0;
            }

        } else {
            boolean solid = false;
            if (needHeightAndID) {
                if (!caves) {
                    LevelChunk chunk = world.getChunkAt(blockPos);
                    transparentHeight = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, blockPos.getX() & 15, blockPos.getZ() & 15) + 1;
                    this.transparentBlockState = world.getBlockState(blockPos.withXYZ(startX + imageX, transparentHeight - 1, startZ + imageY));
                    FluidState fluidState = this.transparentBlockState.getFluidState();
                    if (fluidState != Fluids.EMPTY.defaultFluidState()) {
                        this.transparentBlockState = fluidState.createLegacyBlock();
                    }

                    surfaceHeight = transparentHeight;
                    this.surfaceBlockState = this.transparentBlockState;
                    VoxelShape voxelShape;
                    boolean hasOpacity = this.surfaceBlockState.getLightDampening() > 0;
                    if (!hasOpacity && this.surfaceBlockState.canOcclude() && this.surfaceBlockState.useShapeForLightOcclusion()) {
                        voxelShape = this.surfaceBlockState.getFaceOcclusionShape(Direction.DOWN);
                        hasOpacity = Shapes.faceShapeOccludes(voxelShape, Shapes.empty());
                        voxelShape = this.surfaceBlockState.getFaceOcclusionShape(Direction.UP);
                        hasOpacity = hasOpacity || Shapes.faceShapeOccludes(Shapes.empty(), voxelShape);
                    }

                    while (!hasOpacity && surfaceHeight > world.getMinY()) {
                        foliageBlockState = this.surfaceBlockState;
                        --surfaceHeight;
                        this.surfaceBlockState = world.getBlockState(blockPos.withXYZ(startX + imageX, surfaceHeight - 1, startZ + imageY));
                        fluidState = this.surfaceBlockState.getFluidState();
                        if (fluidState != Fluids.EMPTY.defaultFluidState()) {
                            this.surfaceBlockState = fluidState.createLegacyBlock();
                        }

                        hasOpacity = this.surfaceBlockState.getLightDampening() > 0;
                        if (!hasOpacity && this.surfaceBlockState.canOcclude() && this.surfaceBlockState.useShapeForLightOcclusion()) {
                            voxelShape = this.surfaceBlockState.getFaceOcclusionShape(Direction.DOWN);
                            hasOpacity = Shapes.faceShapeOccludes(voxelShape, Shapes.empty());
                            voxelShape = this.surfaceBlockState.getFaceOcclusionShape(Direction.UP);
                            hasOpacity = hasOpacity || Shapes.faceShapeOccludes(Shapes.empty(), voxelShape);
                        }
                    }

                    if (surfaceHeight == transparentHeight) {
                        transparentHeight = Short.MIN_VALUE;
                        this.transparentBlockState = BlockRepository.air.defaultBlockState();
                        foliageBlockState = world.getBlockState(blockPos.withXYZ(startX + imageX, surfaceHeight, startZ + imageY));
                    }

                    if (foliageBlockState.getBlock() == Blocks.SNOW) {
                        this.surfaceBlockState = foliageBlockState;
                        foliageBlockState = BlockRepository.air.defaultBlockState();
                    }

                    if (foliageBlockState == this.transparentBlockState) {
                        foliageBlockState = BlockRepository.air.defaultBlockState();
                    }

                    if (foliageBlockState != null && !(foliageBlockState.getBlock() instanceof AirBlock)) {
                        foliageHeight = surfaceHeight + 1;
                    } else {
                        foliageHeight = Short.MIN_VALUE;
                    }

                    Block material = this.surfaceBlockState.getBlock();
                    if (material == Blocks.WATER || material == Blocks.ICE) {
                        seafloorHeight = surfaceHeight;

                        for (seafloorBlockState = world.getBlockState(blockPos.withXYZ(startX + imageX, surfaceHeight - 1, startZ + imageY)); seafloorBlockState.getLightDampening() < 5 && !(seafloorBlockState.getBlock() instanceof LeavesBlock)
                                && seafloorHeight > world.getMinY() + 1; seafloorBlockState = world.getBlockState(blockPos.withXYZ(startX + imageX, seafloorHeight - 1, startZ + imageY))) {
                            material = seafloorBlockState.getBlock();
                            if (transparentHeight == Short.MIN_VALUE && material != Blocks.ICE && material != Blocks.WATER && Heightmap.Types.MOTION_BLOCKING.isOpaque().test(seafloorBlockState)) {
                                transparentHeight = seafloorHeight;
                                this.transparentBlockState = seafloorBlockState;
                            }

                            if (foliageHeight == Short.MIN_VALUE && seafloorHeight != transparentHeight && this.transparentBlockState != seafloorBlockState && material != Blocks.ICE && material != Blocks.WATER && !(material instanceof AirBlock) && material != Blocks.BUBBLE_COLUMN) {
                                foliageHeight = seafloorHeight;
                                foliageBlockState = seafloorBlockState;
                            }

                            --seafloorHeight;
                        }

                        if (seafloorBlockState.getBlock() == Blocks.WATER) {
                            seafloorBlockState = BlockRepository.air.defaultBlockState();
                        }
                    }
                } else {
                    surfaceHeight = this.getNetherHeight(startX + imageX, startZ + imageY);
                    this.surfaceBlockState = world.getBlockState(blockPos.withXYZ(startX + imageX, surfaceHeight - 1, startZ + imageY));
                    surfaceBlockStateID = BlockRepository.getStateId(this.surfaceBlockState);
                    foliageHeight = surfaceHeight + 1;
                    blockPos.setXYZ(startX + imageX, foliageHeight - 1, startZ + imageY);
                    foliageBlockState = world.getBlockState(blockPos);
                    Block material = foliageBlockState.getBlock();
                    if (material != Blocks.SNOW && !(material instanceof AirBlock) && material != Blocks.LAVA && material != Blocks.WATER) {
                        foliageBlockStateID = BlockRepository.getStateId(foliageBlockState);
                    } else {
                        foliageHeight = Short.MIN_VALUE;
                    }
                }

                surfaceBlockStateID = BlockRepository.getStateId(this.surfaceBlockState);
                if (this.options.biomeShading.get() && this.surfaceBlockState != this.mapData[zoom].getBlockstate(imageX, imageY)) {
                    surfaceBlockChangeForcedTint = true;
                }

                this.mapData[zoom].setHeight(imageX, imageY, surfaceHeight);
                this.mapData[zoom].setBlockstateID(imageX, imageY, surfaceBlockStateID);
                if (this.options.biomeShading.get() && this.transparentBlockState != this.mapData[zoom].getTransparentBlockstate(imageX, imageY)) {
                    transparentBlockChangeForcedTint = true;
                }

                this.mapData[zoom].setTransparentHeight(imageX, imageY, transparentHeight);
                transparentBlockStateID = BlockRepository.getStateId(this.transparentBlockState);
                this.mapData[zoom].setTransparentBlockstateID(imageX, imageY, transparentBlockStateID);
                if (this.options.biomeShading.get() && foliageBlockState != this.mapData[zoom].getFoliageBlockstate(imageX, imageY)) {
                    foliageBlockChangeForcedTint = true;
                }

                this.mapData[zoom].setFoliageHeight(imageX, imageY, foliageHeight);
                foliageBlockStateID = BlockRepository.getStateId(foliageBlockState);
                this.mapData[zoom].setFoliageBlockstateID(imageX, imageY, foliageBlockStateID);
                if (this.options.biomeShading.get() && seafloorBlockState != this.mapData[zoom].getOceanFloorBlockstate(imageX, imageY)) {
                    seafloorBlockChangeForcedTint = true;
                }

                this.mapData[zoom].setOceanFloorHeight(imageX, imageY, seafloorHeight);
                seafloorBlockStateID = BlockRepository.getStateId(seafloorBlockState);
                this.mapData[zoom].setOceanFloorBlockstateID(imageX, imageY, seafloorBlockStateID);
            } else {
                surfaceHeight = this.mapData[zoom].getHeight(imageX, imageY);
                surfaceBlockStateID = this.mapData[zoom].getBlockstateID(imageX, imageY);
                this.surfaceBlockState = BlockRepository.getStateById(surfaceBlockStateID);
                transparentHeight = this.mapData[zoom].getTransparentHeight(imageX, imageY);
                transparentBlockStateID = this.mapData[zoom].getTransparentBlockstateID(imageX, imageY);
                this.transparentBlockState = BlockRepository.getStateById(transparentBlockStateID);
                foliageHeight = this.mapData[zoom].getFoliageHeight(imageX, imageY);
                foliageBlockStateID = this.mapData[zoom].getFoliageBlockstateID(imageX, imageY);
                foliageBlockState = BlockRepository.getStateById(foliageBlockStateID);
                seafloorHeight = this.mapData[zoom].getOceanFloorHeight(imageX, imageY);
                seafloorBlockStateID = this.mapData[zoom].getOceanFloorBlockstateID(imageX, imageY);
                seafloorBlockState = BlockRepository.getStateById(seafloorBlockStateID);
            }

            if (surfaceHeight == Short.MIN_VALUE) {
                surfaceHeight = this.lastY + 1;
                solid = true;
            }

            if (this.surfaceBlockState.getBlock() == Blocks.LAVA) {
                solid = false;
            }

            if (this.options.biomeShading.get()) {
                surfaceColor = this.colorManager.getBlockColor(blockPos, surfaceBlockStateID, biome);
                int tint;
                if (!needTint && !surfaceBlockChangeForcedTint) {
                    tint = this.mapData[zoom].getBiomeTint(imageX, imageY);
                } else {
                    tint = this.colorManager.getBiomeTint(this.mapData[zoom], world, this.surfaceBlockState, surfaceBlockStateID, blockPos.withXYZ(startX + imageX, surfaceHeight - 1, startZ + imageY), tempBlockPos, startX, startZ);
                    this.mapData[zoom].setBiomeTint(imageX, imageY, tint);
                }

                if (tint != -1) {
                    surfaceColor = ColorUtils.colorMultiplier(surfaceColor, tint);
                }
            } else {
                surfaceColor = this.colorManager.getBlockColorWithDefaultTint(blockPos, surfaceBlockStateID);
            }

            surfaceColor = this.applyHeight(surfaceColor, caves, world, zoom, multi, startX, startZ, imageX, imageY, surfaceHeight, solid, 1);
            int light;
            if (needLight) {
                light = this.getLight(surfaceColor, this.surfaceBlockState, world, startX + imageX, startZ + imageY, surfaceHeight, solid);
                this.mapData[zoom].setLight(imageX, imageY, light);
            } else {
                light = this.mapData[zoom].getLight(imageX, imageY);
            }

            if (light == 0) {
                surfaceColor = 0;
            } else if (light != 255) {
                surfaceColor = ColorUtils.colorMultiplier(surfaceColor, light);
            }

            if (this.options.waterTransparency.get() && seafloorHeight != Short.MIN_VALUE) {
                if (!this.options.biomeShading.get()) {
                    seafloorColor = this.colorManager.getBlockColorWithDefaultTint(blockPos, seafloorBlockStateID);
                } else {
                    seafloorColor = this.colorManager.getBlockColor(blockPos, seafloorBlockStateID, biome);
                    int tint;
                    if (!needTint && !seafloorBlockChangeForcedTint) {
                        tint = this.mapData[zoom].getOceanFloorBiomeTint(imageX, imageY);
                    } else {
                        tint = this.colorManager.getBiomeTint(this.mapData[zoom], world, seafloorBlockState, seafloorBlockStateID, blockPos.withXYZ(startX + imageX, seafloorHeight - 1, startZ + imageY), tempBlockPos, startX, startZ);
                        this.mapData[zoom].setOceanFloorBiomeTint(imageX, imageY, tint);
                    }

                    if (tint != -1) {
                        seafloorColor = ColorUtils.colorMultiplier(seafloorColor, tint);
                    }
                }

                seafloorColor = this.applyHeight(seafloorColor, caves, world, zoom, multi, startX, startZ, imageX, imageY, seafloorHeight, solid, 0);
                int seafloorLight;
                if (needLight) {
                    seafloorLight = this.getLight(seafloorColor, seafloorBlockState, world, startX + imageX, startZ + imageY, seafloorHeight, solid);
                    blockPos.setXYZ(startX + imageX, seafloorHeight, startZ + imageY);
                    BlockState blockStateAbove = world.getBlockState(blockPos);
                    Block materialAbove = blockStateAbove.getBlock();
                    if (this.options.dynamicLighting.get() && materialAbove == Blocks.ICE) {
                        int multiplier = minecraft.options.ambientOcclusion().get() ? 200 : 120;
                        seafloorLight = ColorUtils.colorMultiplier(seafloorLight, 0xFF000000 | multiplier << 16 | multiplier << 8 | multiplier);
                    }

                    this.mapData[zoom].setOceanFloorLight(imageX, imageY, seafloorLight);
                } else {
                    seafloorLight = this.mapData[zoom].getOceanFloorLight(imageX, imageY);
                }

                if (seafloorLight == 0) {
                    seafloorColor = 0;
                } else if (seafloorLight != 255) {
                    seafloorColor = ColorUtils.colorMultiplier(seafloorColor, seafloorLight);
                }
            }

            if (this.options.blockTransparency.get()) {
                if (transparentHeight != Short.MIN_VALUE && this.transparentBlockState != null && this.transparentBlockState != BlockRepository.air.defaultBlockState()) {
                    if (this.options.biomeShading.get()) {
                        transparentColor = this.colorManager.getBlockColor(blockPos, transparentBlockStateID, biome);
                        int tint;
                        if (!needTint && !transparentBlockChangeForcedTint) {
                            tint = this.mapData[zoom].getTransparentBiomeTint(imageX, imageY);
                        } else {
                            tint = this.colorManager.getBiomeTint(this.mapData[zoom], world, this.transparentBlockState, transparentBlockStateID, blockPos.withXYZ(startX + imageX, transparentHeight - 1, startZ + imageY), tempBlockPos, startX, startZ);
                            this.mapData[zoom].setTransparentBiomeTint(imageX, imageY, tint);
                        }

                        if (tint != -1) {
                            transparentColor = ColorUtils.colorMultiplier(transparentColor, tint);
                        }
                    } else {
                        transparentColor = this.colorManager.getBlockColorWithDefaultTint(blockPos, transparentBlockStateID);
                    }

                    transparentColor = this.applyHeight(transparentColor, caves, world, zoom, multi, startX, startZ, imageX, imageY, transparentHeight, solid, 3);
                    int transparentLight;
                    if (needLight) {
                        transparentLight = this.getLight(transparentColor, this.transparentBlockState, world, startX + imageX, startZ + imageY, transparentHeight, solid);
                        this.mapData[zoom].setTransparentLight(imageX, imageY, transparentLight);
                    } else {
                        transparentLight = this.mapData[zoom].getTransparentLight(imageX, imageY);
                    }

                    if (transparentLight == 0) {
                        transparentColor = 0;
                    } else if (transparentLight != 255) {
                        transparentColor = ColorUtils.colorMultiplier(transparentColor, transparentLight);
                    }
                }

                if (foliageHeight != Short.MIN_VALUE && foliageBlockState != null && foliageBlockState != BlockRepository.air.defaultBlockState()) {
                    if (!this.options.biomeShading.get()) {
                        foliageColor = this.colorManager.getBlockColorWithDefaultTint(blockPos, foliageBlockStateID);
                    } else {
                        foliageColor = this.colorManager.getBlockColor(blockPos, foliageBlockStateID, biome);
                        int tint;
                        if (!needTint && !foliageBlockChangeForcedTint) {
                            tint = this.mapData[zoom].getFoliageBiomeTint(imageX, imageY);
                        } else {
                            tint = this.colorManager.getBiomeTint(this.mapData[zoom], world, foliageBlockState, foliageBlockStateID, blockPos.withXYZ(startX + imageX, foliageHeight - 1, startZ + imageY), tempBlockPos, startX, startZ);
                            this.mapData[zoom].setFoliageBiomeTint(imageX, imageY, tint);
                        }

                        if (tint != -1) {
                            foliageColor = ColorUtils.colorMultiplier(foliageColor, tint);
                        }
                    }

                    foliageColor = this.applyHeight(foliageColor, caves, world, zoom, multi, startX, startZ, imageX, imageY, foliageHeight, solid, 2);
                    int foliageLight;
                    if (needLight) {
                        foliageLight = this.getLight(foliageColor, foliageBlockState, world, startX + imageX, startZ + imageY, foliageHeight, solid);
                        this.mapData[zoom].setFoliageLight(imageX, imageY, foliageLight);
                    } else {
                        foliageLight = this.mapData[zoom].getFoliageLight(imageX, imageY);
                    }

                    if (foliageLight == 0) {
                        foliageColor = 0;
                    } else if (foliageLight != 255) {
                        foliageColor = ColorUtils.colorMultiplier(foliageColor, foliageLight);
                    }
                }
            }

            if (seafloorColor != 0 && seafloorHeight > Short.MIN_VALUE) {
                color24 = seafloorColor;
                if (foliageColor != 0 && foliageHeight <= surfaceHeight) {
                    color24 = ColorUtils.colorAdder(foliageColor, seafloorColor);
                }

                if (transparentColor != 0 && transparentHeight <= surfaceHeight) {
                    color24 = ColorUtils.colorAdder(transparentColor, color24);
                }

                color24 = ColorUtils.colorAdder(surfaceColor, color24);
            } else {
                color24 = surfaceColor;
            }

            if (foliageColor != 0 && foliageHeight > surfaceHeight) {
                color24 = ColorUtils.colorAdder(foliageColor, color24);
            }

            if (transparentColor != 0 && transparentHeight > surfaceHeight) {
                color24 = ColorUtils.colorAdder(transparentColor, color24);
            }

            if (this.options.biomeOverlay.get() == OptionEnumMinimap.BiomeOverlay.TRANSPARENT) {
                int bc = 0;
                if (biome != null) {
                    bc = ARGB.toABGR(BiomeRepository.getBiomeColor(biome));
                }

                bc = 0x7F000000 | bc;
                color24 = ColorUtils.colorAdder(bc, color24);
            }

        }
        MutableBlockPosCache.release(blockPos);
        MutableBlockPosCache.release(tempBlockPos);
        return MapUtils.doSlimeAndGrid(color24, world, startX + imageX, startZ + imageY);
    }

    private int getBlockHeight(boolean caves, Level world, int x, int z) {
        MutableBlockPos blockPos = MutableBlockPosCache.get();
        int playerHeight = GameVariableAccessShim.yCoord();
        blockPos.setXYZ(x, playerHeight, z);
        LevelChunk chunk = (LevelChunk) world.getChunk(blockPos);
        int height = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, blockPos.getX() & 15, blockPos.getZ() & 15) + 1;
        BlockState blockState = world.getBlockState(blockPos.withXYZ(x, height - 1, z));
        FluidState fluidState = this.transparentBlockState.getFluidState();
        if (fluidState != Fluids.EMPTY.defaultFluidState()) {
            blockState = fluidState.createLegacyBlock();
        }

        while (blockState.getLightDampening() == 0 && height > world.getMinY()) {
            --height;
            blockState = world.getBlockState(blockPos.withXYZ(x, height - 1, z));
            fluidState = this.surfaceBlockState.getFluidState();
            if (fluidState != Fluids.EMPTY.defaultFluidState()) {
                blockState = fluidState.createLegacyBlock();
            }
        }
        MutableBlockPosCache.release(blockPos);
        return caves && height > playerHeight ? this.getNetherHeight(x, z) : height;
    }

    private int getNetherHeight(int x, int z) {
        MutableBlockPos blockPos = MutableBlockPosCache.get();
        int y = this.lastY;
        blockPos.setXYZ(x, y, z);
        BlockState blockState = this.world.getBlockState(blockPos);
        if (blockState.getLightDampening() == 0 && blockState.getBlock() != Blocks.LAVA) {
            while (y > world.getMinY()) {
                --y;
                blockPos.setXYZ(x, y, z);
                blockState = this.world.getBlockState(blockPos);
                if (blockState.getLightDampening() > 0 || blockState.getBlock() == Blocks.LAVA) {
                    MutableBlockPosCache.release(blockPos);
                    return y + 1;
                }
            }
            MutableBlockPosCache.release(blockPos);
            return y;
        } else {
            while (y <= this.lastY + 10 && y < world.getMaxY()) {
                ++y;
                blockPos.setXYZ(x, y, z);
                blockState = this.world.getBlockState(blockPos);
                if (blockState.getLightDampening() == 0 && blockState.getBlock() != Blocks.LAVA) {
                    MutableBlockPosCache.release(blockPos);
                    return y;
                }
            }
            MutableBlockPosCache.release(blockPos);
            return Short.MIN_VALUE;
        }
    }

    private int getSeafloorHeight(Level world, int x, int z, int height) {
        MutableBlockPos blockPos = MutableBlockPosCache.get();
        for (BlockState blockState = world.getBlockState(blockPos.withXYZ(x, height - 1, z)); blockState.getLightDampening() < 5 && !(blockState.getBlock() instanceof LeavesBlock) && height > world.getMinY() + 1; blockState = world.getBlockState(blockPos.withXYZ(x, height - 1, z))) {
            --height;
        }
        MutableBlockPosCache.release(blockPos);
        return height;
    }

    private int getTransparentHeight(boolean caves, Level world, int x, int z, int height) {
        MutableBlockPos blockPos = MutableBlockPosCache.get();
        int transHeight;
        if (!caves) {
            transHeight = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockPos.withXYZ(x, height, z)).getY();
            if (transHeight <= height) {
                transHeight = Short.MIN_VALUE;
            }
        } else {
            transHeight = Short.MIN_VALUE;
        }

        BlockState blockState = world.getBlockState(blockPos.withXYZ(x, transHeight - 1, z));
        Block material = blockState.getBlock();
        if (transHeight == height + 1 && material == Blocks.SNOW) {
            transHeight = Short.MIN_VALUE;
        }

        if (material == Blocks.BARRIER) {
            ++transHeight;
            blockState = world.getBlockState(blockPos.withXYZ(x, transHeight - 1, z));
            material = blockState.getBlock();
            if (material instanceof AirBlock) {
                transHeight = Short.MIN_VALUE;
            }
        }
        MutableBlockPosCache.release(blockPos);
        return transHeight;
    }

    private int applyHeight(int color24, boolean caves, Level world, int zoom, int multi, int startX, int startZ, int imageX, int imageY, int height, boolean solid, int layer) {
        if (color24 != this.colorManager.getAirColor() && color24 != 0 && (isHeightEnabled() || isSlopeEnabled()) && !solid) {
            int heightComp = -1;
            int diff;
            double sc = 0.0;
            if (!isSlopeEnabled()) {
                diff = height - this.lastY;
                sc = Math.log10(Math.abs(diff) / 8.0 + 1.0) / 1.8;
                if (diff < 0) {
                    sc = 0.0 - sc;
                }
            } else {
                if (imageX > 0 && imageY < 32 * multi - 1) {
                    if (layer == 0) {
                        heightComp = this.mapData[zoom].getOceanFloorHeight(imageX - 1, imageY + 1);
                    }

                    if (layer == 1) {
                        heightComp = this.mapData[zoom].getHeight(imageX - 1, imageY + 1);
                    }

                    if (layer == 2) {
                        heightComp = height;
                    }

                    if (layer == 3) {
                        heightComp = this.mapData[zoom].getTransparentHeight(imageX - 1, imageY + 1);
                        if (heightComp == Short.MIN_VALUE) {
                            Block block = BlockRepository.getStateById(this.mapData[zoom].getTransparentBlockstateID(imageX, imageY)).getBlock();
                            if (block == Blocks.GLASS || block instanceof StainedGlassBlock) {
                                heightComp = this.mapData[zoom].getHeight(imageX - 1, imageY + 1);
                            }
                        }
                    }
                } else {
                    if (layer == 0) {
                        int baseHeight = this.getBlockHeight(caves, world, startX + imageX - 1, startZ + imageY + 1);
                        heightComp = this.getSeafloorHeight(world, startX + imageX - 1, startZ + imageY + 1, baseHeight);
                    }

                    if (layer == 1) {
                        heightComp = this.getBlockHeight(caves, world, startX + imageX - 1, startZ + imageY + 1);
                    }

                    if (layer == 2) {
                        heightComp = height;
                    }

                    if (layer == 3) {
                        int baseHeight = this.getBlockHeight(caves, world, startX + imageX - 1, startZ + imageY + 1);
                        heightComp = this.getTransparentHeight(caves, world, startX + imageX - 1, startZ + imageY + 1, baseHeight);
                        if (heightComp == Short.MIN_VALUE) {
                            MutableBlockPos blockPos = MutableBlockPosCache.get();
                            BlockState blockState = world.getBlockState(blockPos.withXYZ(startX + imageX, height - 1, startZ + imageY));
                            MutableBlockPosCache.release(blockPos);
                            Block block = blockState.getBlock();
                            if (block == Blocks.GLASS || block instanceof StainedGlassBlock) {
                                heightComp = baseHeight;
                            }
                        }
                    }
                }

                if (heightComp == Short.MIN_VALUE) {
                    heightComp = height;
                }

                diff = heightComp - height;
                if (diff != 0) {
                    sc = diff > 0 ? 1.0 : -1.0;
                    sc /= 8.0;
                }

                if (isHeightEnabled()) {
                    diff = height - this.lastY;
                    double heightsc = Math.log10(Math.abs(diff) / 8.0 + 1.0) / 3.0;
                    sc = diff > 0 ? sc + heightsc : sc - heightsc;
                }
            }

            int alpha = color24 >> 24 & 0xFF;
            int r = color24 >> 16 & 0xFF;
            int g = color24 >> 8 & 0xFF;
            int b = color24 & 0xFF;
            if (sc > 0.0) {
                r += (int) (sc * (255 - r));
                g += (int) (sc * (255 - g));
                b += (int) (sc * (255 - b));
            } else if (sc < 0.0) {
                sc = Math.abs(sc);
                r -= (int) (sc * r);
                g -= (int) (sc * g);
                b -= (int) (sc * b);
            }

            color24 = alpha * 16777216 + r * 65536 + g * 256 + b;
        }

        return color24;
    }

    private int getLight(int color24, BlockState blockState, Level world, int x, int z, int height, boolean solid) {
        int combinedLight = 0xffffffff;
        if (solid) {
            combinedLight = 0;
        } else if (color24 != this.colorManager.getAirColor() && color24 != 0 && this.options.dynamicLighting.get()) {
            MutableBlockPos blockPos = MutableBlockPosCache.get();
            blockPos.setXYZ(x, Math.max(Math.min(height, world.getMaxY()), world.getMinY()), z);
            int blockLight = world.getBrightness(LightLayer.BLOCK, blockPos);
            int skyLight = world.getBrightness(LightLayer.SKY, blockPos);
            if (blockState.getBlock() == Blocks.LAVA || blockState.getBlock() == Blocks.MAGMA_BLOCK) {
                blockLight = 14;
            }
            MutableBlockPosCache.release(blockPos);
            combinedLight = getLightmapColor(skyLight, blockLight);
        }

        return ARGB.toABGR(combinedLight);
    }


    private void renderMap(Matrix4fStack matrixStack, DeferredRenderPass pass, int x, int y, int scScale, float scaleProj) {
        matrixStack.pushMatrix();
        matrixStack.scale(scaleProj, scaleProj, 1.0F);

        synchronized (this.coordinateLock) {
            if (this.imageChanged) {
                this.imageChanged = false;
                this.mapImages[this.zoom].upload();
                this.lastImageX = this.lastX;
                this.lastImageZ = this.lastZ;
            }
        }

        // Set minimap projection matrix
        RenderUtils.setProjectionMatrix(mapProjection.getBuffer(512.0F, 512.0F), ProjectionType.ORTHOGRAPHIC, -2000.0F);
        matrixStack.pushMatrix();
        matrixStack.identity();
        matrixStack.translate(256.0F, 256.0F, 0.0F);

        // Draw map, radar, etc.
        try (DeferredRenderPass mapPass = RenderUtils.createDeferredRenderPass("VoxelMap Base Map Draw", baseMapRenderTarget.getColorTextureView(), Optional.of(new Vector4f(0.0F, 0.0F, 0.0F, 0.0F)), baseMapRenderTarget.getDepthTextureView(), OptionalDouble.of(0.0))) {
            float scale = getMapImageScale();
            float multi = (float) (1.0 / this.zoomScale);
            float percentX = (float) (GameVariableAccessShim.xCoordDouble() - this.lastImageX) * multi;
            float percentY = (float) (GameVariableAccessShim.zCoordDouble() - this.lastImageZ) * multi;

            matrixStack.pushMatrix();
            if (!isRotationEnabled()) {
                matrixStack.rotate(Axis.ZP.rotationDegrees(rotationFactor));
            } else {
                matrixStack.rotate(Axis.ZP.rotationDegrees(-direction));
            }
            matrixStack.scale(scale, scale, 1.0F);
            matrixStack.translate(-percentX * 512.0F / 64.0F, -percentY * 512.0F / 64.0F, 0.0F);
            mapPass.setPipeline(VoxelMapPipelines.GUI_TEXTURED_DEPTH_TEST);
            mapPass.bindTexture("Sampler0", mapImages[zoom]);
            mapPass.beginBatch();
            mapPass.drawTexturedModalRect(matrixStack, -256.0F, -256.0F, -10.0F, 512.0F, 512.0F, 0xFFFFFFFF);
            mapPass.endBatch();
            if (options.worldBorder.get()) {
                WorldBorder worldBorder = minecraft.level.getWorldBorder();

                float x0 = ((float) (worldBorder.getMinX()) - lastImageX) * multi * 512.0F / 64.0F;
                float z0 = ((float) (worldBorder.getMinZ()) - lastImageZ) * multi * 512.0F / 64.0F;
                float x1 = ((float) (worldBorder.getMaxX()) - lastImageX) * multi * 512.0F / 64.0F;
                float z1 = ((float) (worldBorder.getMaxZ()) - lastImageZ) * multi * 512.0F / 64.0F;

                mapPass.setPipeline(VoxelMapPipelines.GUI_DEPTH_TEST);
                mapPass.beginBatch();
                mapPass.drawTexturedModalRect(matrixStack, x0 - 2.0F, z0 - 2.0F, -10.0F, x1 - x0 + 4.0F, 4.0F, 0xFFFF0000);
                mapPass.drawTexturedModalRect(matrixStack, x0 - 2.0F, z1 - 2.0F, -10.0F, x1 - x0 + 4.0F, 4.0F, 0xFFFF0000);
                mapPass.drawTexturedModalRect(matrixStack, x0 - 2.0F, z0 - 2.0F, -10.0F, 4.0F, z1 - z0 + 4.0F, 0xFFFF0000);
                mapPass.drawTexturedModalRect(matrixStack, x1 - 2.0F, z0 - 2.0F, -10.0F, 4.0F, z1 - z0 + 4.0F, 0xFFFF0000);
                mapPass.endBatch();
            }
            matrixStack.popMatrix();

            if (VoxelConstants.getVoxelMapInstance().getRadar() != null) {
                VoxelConstants.getVoxelMapInstance().getRadar().onTickInGame(minimapContext);
                VoxelConstants.getVoxelMapInstance().getRadar().renderMapMobs(matrixStack, mapPass, 0, 0, Contact.DisplayState.BELOW_FRAME, scScale, 512.0F / 64.0F);
            }
        }

        // Masking the drawn map
        try (DeferredRenderPass mapPass = RenderUtils.createDeferredRenderPass("VoxelMap Final Map Draw", finalMapRenderTarget.getColorTextureView(), Optional.of(new Vector4f(0.0F, 0.0F, 0.0F, 0.0F)), finalMapRenderTarget.getDepthTextureView(), OptionalDouble.of(0.0))) {
            mapPass.setPipeline(VoxelMapPipelines.GUI_TEXTURED_NO_DEPTH_TEST);
            mapPass.bindTexture("Sampler0", options.squareMap.get() ? squareMapStencilTexture : roundMapStencilTexture);
            mapPass.beginBatch();
            mapPass.drawTexturedModalRect(matrixStack, -256.0F, -256.0F, 0.0F, 512.0F, 512.0F, 0xFFFFFFFF);
            mapPass.endBatch();

            mapPass.setPipeline(VoxelMapPipelines.GUI_TEXTURED_NO_DEPTH_TEST_MASKED);
            mapPass.bindTexture("Sampler0", baseMapRenderTarget.getColorTextureView(), VoxelMapPipelines.LINEAR_CLAMP_SAMPLER);
            mapPass.beginBatch();
            mapPass.drawBlitRect(matrixStack, -256.0F, -256.0F, 0.0F, 512.0F, 512.0F, 0xFFFFFFFF);
            mapPass.endBatch();
        }

        // Restore projection matrix
        matrixStack.popMatrix();
        RenderUtils.restoreProjectionMatrix();

        double guiScale = (double) minecraft.getWindow().getWidth() / this.scWidth;
        minTablistOffset = guiScale * 63;

        pass.setPipeline(VoxelMapPipelines.GUI_TEXTURED_DEPTH_TEST);

        pass.bindTexture("Sampler0", finalMapRenderTarget.getColorTextureView(), VoxelMapPipelines.LINEAR_CLAMP_SAMPLER);
        pass.beginBatch();
        pass.drawBlitRect(matrixStack, x - 32.0F, y - 32.0F, MAP_IMAGE_DEPTH, 64.0F, 64.0F, 0xFFFFFFFF);
        pass.endBatch();

        pass.bindTexture("Sampler0", options.squareMap.get() ? squareMapFrameTexture : roundMapFrameTexture);
        pass.beginBatch();
        pass.drawTexturedModalRect(matrixStack, x - 32.0F, y - 32.0F, MAP_OVERLAY_DEPTH, 64.0F, 64.0F, 0xFFFFFFFF);
        pass.endBatch();

        double lastXDouble = GameVariableAccessShim.xCoordDouble();
        double lastZDouble = GameVariableAccessShim.zCoordDouble();

        if (serverSettings.waypointsAllowed.get()) {
            TextureAtlas textureAtlas = VoxelConstants.getVoxelMapInstance().getWaypointManager().getTextureAtlas();
            pass.bindTexture("Sampler0", textureAtlas);
            pass.beginBatch();
            for (Waypoint waypoint : waypointManager.getWaypoints()) {
                boolean isHighlighted = waypointManager.isHighlightedWaypoint(waypoint);
                if (waypoint.isActive() || isHighlighted) {
                    double distanceSq = waypoint.getDistanceSqToEntity(minecraft.getCameraEntity());
                    boolean isOutOfRange = waypointOptions.maxDistance.get() <= 10000 && distanceSq >= (waypointOptions.maxDistance.get() * waypointOptions.maxDistance.get());
                    if (!isOutOfRange || isHighlighted) {
                        drawWaypoint(matrixStack, pass, x, y, waypoint, textureAtlas, null, isHighlighted, -1, lastXDouble, lastZDouble);
                    }
                }
            }
            Waypoint highlightedPoint = waypointManager.getHighlightedWaypoint();
            if (highlightedPoint != null) {
                drawWaypoint(matrixStack, pass, x, y, highlightedPoint, textureAtlas, textureAtlas.getAtlasSprite("marker/target"), true, 0xFFFF0000, lastXDouble, lastZDouble);
            }
            pass.endBatch();
        }

        matrixStack.popMatrix();
    }

    private void drawWaypoint(Matrix4fStack matrixStack, DeferredRenderPass pass, int x, int y, Waypoint waypoint, TextureAtlas textureAtlas, Sprite icon, boolean isHighlighted, int color, double baseX, double baseZ) {
        boolean uprightIcon = icon != null;

        double wayX = baseX - waypoint.getX() - 0.5;
        double wayY = baseZ - waypoint.getZ() - 0.5;
        float locate = (float) Math.toDegrees(Math.atan2(wayX, wayY));
        float hypot = (float) (Math.sqrt(wayX * wayX + wayY * wayY) / zoomScaleAdjusted);
        boolean far;
        locate += isRotationEnabled() ? direction : -rotationFactor;

        if (this.options.squareMap.get()) {
            double radLocate = Math.toRadians(locate);
            double dispX = hypot * Math.cos(radLocate);
            double dispY = hypot * Math.sin(radLocate);
            far = Math.abs(dispX) > 28.5 || Math.abs(dispY) > 28.5;
            if (far) {
                hypot = (float) (hypot / Math.max(Math.abs(dispX), Math.abs(dispY)) * 30.0);
            }
        } else {
            far = hypot >= 31.0f;
            if (far) {
                hypot = 34.0f;
            }
        }

        int iconColor = color == -1 ? waypoint.getUnifiedColor(!waypoint.enabled && isHighlighted ? 0.3F : 1.0F) : color;
        if (far) {
            if (icon == null) {
                icon = textureAtlas.getAtlasSprite("marker/" + waypoint.imageSuffix);
                if (icon == textureAtlas.getMissingImage()) {
                    icon = textureAtlas.getAtlasSprite("marker/arrow");
                }
            }

            try {
                matrixStack.pushMatrix();
                matrixStack.translate(x, y, 0.0F);
                matrixStack.rotate(Axis.ZP.rotationDegrees(-locate));
                if (uprightIcon) {
                    matrixStack.translate(0.0F, -hypot, 0.0F);
                    matrixStack.rotate(Axis.ZP.rotationDegrees(locate));
                    matrixStack.translate(-x, -y, 0.0F);
                } else {
                    matrixStack.translate(-x, -y, 0.0F);
                    matrixStack.translate(0.0F, -hypot, 0.0F);
                }

                pass.drawSpriteRect(matrixStack, icon, x - 4.0F, y - 4.0F, MAP_OVERLAY_DEPTH, 8.0F, 8.0F, iconColor);
            } catch (Exception var40) {
                this.showMessage("Error: marker overlay not found!");
            } finally {
                matrixStack.popMatrix();
            }
        } else {
            if (icon == null) {
                icon = textureAtlas.getAtlasSprite("selectable/" + waypoint.imageSuffix);
                if (icon == textureAtlas.getMissingImage()) {
                    icon = textureAtlas.getAtlasSprite(WaypointManager.FALLBACK_ICON_NAME);
                }
            }

            try {
                matrixStack.pushMatrix();
                matrixStack.rotate(Axis.ZP.rotationDegrees(-locate));
                matrixStack.translate(0.0F, -hypot, 0.0F);
                matrixStack.rotate(Axis.ZP.rotationDegrees(locate));

                pass.drawSpriteRect(matrixStack, icon, x - 4.0F, y - 4.0F, MAP_OVERLAY_DEPTH, 8.0F, 8.0F, iconColor);
            } catch (Exception var42) {
                this.showMessage("Error: waypoint overlay not found!");
            } finally {
                matrixStack.popMatrix();
            }
        }
    }

    private void drawArrow(Matrix4fStack matrixStack, DeferredRenderPass pass, int x, int y, float scaleProj) {
        matrixStack.pushMatrix();
        matrixStack.scale(scaleProj, scaleProj, 1.0F);

        matrixStack.translate(x, y, 0.0F);
        matrixStack.rotate(Axis.ZP.rotationDegrees(isRotationEnabled() ? 0.0F : this.direction + this.rotationFactor));
        matrixStack.translate(-x, -y, 0.0F);

        pass.setPipeline(VoxelMapPipelines.GUI_TEXTURED_DEPTH_TEST);
        pass.bindTexture("Sampler0", minimapArrowTexture);
        pass.beginBatch();
        pass.drawTexturedModalRect(matrixStack, x - 4.0F, y - 4.0F, MAP_OVERLAY_DEPTH, 8.0F, 8.0F, 0xFFFFFFFF);
        pass.endBatch();

        matrixStack.popMatrix();
    }

    private void renderMapFull(Matrix4fStack matrixStack, DeferredRenderPass pass, int x, int y, float scaleProj) {
        synchronized (this.coordinateLock) {
            if (this.imageChanged) {
                this.imageChanged = false;
                this.mapImages[this.zoom].upload();
                this.lastImageX = this.lastX;
                this.lastImageZ = this.lastZ;
            }
        }
        matrixStack.pushMatrix();
        matrixStack.scale(scaleProj, scaleProj, 1.0F);
        matrixStack.translate(x, y, 0.0F);
        matrixStack.rotate(Axis.ZP.rotationDegrees(rotationFactor));
        matrixStack.translate(-x, -y, 0.0F);
        int left = x - 128;
        int top = y - 128;
        pass.setPipeline(VoxelMapPipelines.GUI_TEXTURED_DEPTH_TEST);
        pass.bindTexture("Sampler0", mapImages[zoom]);
        pass.beginBatch();
        pass.drawTexturedModalRect(matrixStack, left, top, MAP_IMAGE_DEPTH, 256.0f, 256.0F, 0xFFFFFFFF);
        pass.endBatch();
        matrixStack.popMatrix();

        if (this.options.biomeOverlay.get() != OptionEnumMinimap.BiomeOverlay.OFF) {
            double factor = Math.pow(2.0, 3 - this.zoom);
            int minimumSize = (int) Math.pow(2.0, this.zoom);
            minimumSize *= minimumSize;
            ArrayList<AbstractMapData.BiomeLabel> labels = this.mapData[this.zoom].getBiomeLabels();
            matrixStack.pushMatrix();

            for (AbstractMapData.BiomeLabel o : labels) {
                if (o.segmentSize > minimumSize) {
                    String name = o.name;
                    float labelX = (float) (o.x * factor);
                    float labelZ = (float) (o.z * factor);
                    if (this.options.oldNorth.get()) {
                        pass.drawCenteredString(matrixStack, name, (left + 256) - labelZ, top + labelX - 3.0F, MAP_TEXT_DEPTH, 0xFFFFFFFF, true);
                    } else {
                        pass.drawCenteredString(matrixStack, name, left + labelX, top + labelZ - 3.0F, MAP_TEXT_DEPTH, 0xFFFFFFFF, true);
                    }
                }
            }

            matrixStack.popMatrix();
        }
    }

    private void drawDirections(Matrix4fStack matrixStack, DeferredRenderPass pass, int x, int y, float scaleProj) {
        float scale = 0.5F;
        float rotate;
        if (isRotationEnabled()) {
            rotate = -this.direction - 90.0F - this.rotationFactor;
        } else {
            rotate = -90.0F;
        }

        float distance;
        if (this.options.squareMap.get()) {
            if (isRotationEnabled()) {
                float tempdir = this.direction % 90.0F;
                tempdir = 45.0F - Math.abs(45.0F - tempdir);
                distance = 33.5F / scale / Mth.cos(Math.toRadians(tempdir));
            } else {
                distance = 33.5F / scale;
            }
        } else {
            distance = 32.5F / scale;
        }

        matrixStack.pushMatrix();
        matrixStack.scale(scaleProj, scaleProj, 1.0F);
        matrixStack.scale(scale, scale, 1.0F);

        matrixStack.pushMatrix();
        matrixStack.translate(distance * Mth.sin(-(rotate - 90.0F) * Mth.DEG_TO_RAD), distance * Mth.cos(-(rotate - 90.0F) * Mth.DEG_TO_RAD), 0.0F);
        pass.drawCenteredString(matrixStack, "N", x / scale, y / scale - 4.0F, MAP_TEXT_DEPTH, 0xFFFFFFFF, true);
        matrixStack.popMatrix();
        matrixStack.pushMatrix();
        matrixStack.translate(distance * Mth.sin(-(rotate) * Mth.DEG_TO_RAD), distance * Mth.cos(-(rotate) * Mth.DEG_TO_RAD), 0.0F);
        pass.drawCenteredString(matrixStack, "E", x / scale, y / scale - 4.0F, MAP_TEXT_DEPTH, 0xFFFFFFFF, true);
        matrixStack.popMatrix();
        matrixStack.pushMatrix();
        matrixStack.translate(distance * Mth.sin(-(rotate + 90.0F) * Mth.DEG_TO_RAD), distance * Mth.cos(-(rotate + 90.0F) * Mth.DEG_TO_RAD), 0.0F);
        pass.drawCenteredString(matrixStack, "S", x / scale, y / scale - 4.0F, MAP_TEXT_DEPTH, 0xFFFFFFFF, true);
        matrixStack.popMatrix();
        matrixStack.pushMatrix();
        matrixStack.translate(distance * Mth.sin(-(rotate + 180.0F) * Mth.DEG_TO_RAD), distance * Mth.cos(-(rotate + 180.0F) * Mth.DEG_TO_RAD), 0.0F);
        pass.drawCenteredString(matrixStack, "W", x / scale, y / scale - 4.0F, MAP_TEXT_DEPTH, 0xFFFFFFFF, true);
        matrixStack.popMatrix();

        matrixStack.popMatrix();
    }

    private void showCoords(Matrix4fStack matrixStack, DeferredRenderPass pass, int x, int y, float scaleProj) {
        if (!this.options.hide.get() && !this.fullscreenMap) {
            int textStart;
            if (y > this.scHeight - 37 - 32 - 4 - 15) {
                textStart = y - 32 - 4 - 9;
            } else {
                textStart = y + 32 + 4;
            }
            int lineCount = 0;
            int lineHeight = 10;
            float scale = 0.5F;
            matrixStack.pushMatrix();
            matrixStack.scale(scale * scaleProj, scale * scaleProj, 1.0F);

            String coords;

            switch (options.showCoordInfo.get()) {
                case DEFAULT -> {
                    coords = GameVariableAccessShim.xCoord() + ", " + GameVariableAccessShim.yCoord() + ", " + GameVariableAccessShim.zCoord();
                    pass.drawCenteredString(matrixStack, coords, x / scale, textStart / scale + lineHeight * lineCount, MAP_TEXT_DEPTH, 0xFFFFFFFF, true); // X, Z
                    lineCount++;
                }
                case CLASSIC -> {
                    coords = this.dCoord(GameVariableAccessShim.xCoord()) + ", " + this.dCoord(GameVariableAccessShim.zCoord());
                    pass.drawCenteredString(matrixStack, coords, x / scale, textStart / scale + lineHeight * lineCount, MAP_TEXT_DEPTH, 0xFFFFFFFF, true); // X, Z
                    lineCount++;

                    coords = this.dCoord(GameVariableAccessShim.yCoord());
                    pass.drawCenteredString(matrixStack, coords, x / scale, textStart / scale + lineHeight * lineCount, MAP_TEXT_DEPTH, 0xFFFFFFFF, true); // Y
                    lineCount++;
                }
            }

            if (this.options.showBiomeInfo.get()) {
                coords = BiomeRepository.getName(this.lastBiome);
                pass.drawCenteredString(matrixStack, coords, x / scale, textStart / scale + lineHeight * lineCount, MAP_TEXT_DEPTH, 0xFFFFFFFF, true); // BIOME
                lineCount++;
            }

            if (!this.message.isEmpty()) {
                pass.drawCenteredString(matrixStack, this.message, x / scale, textStart / scale + lineHeight * lineCount, MAP_TEXT_DEPTH, 0xFFFFFFFF, true); // WORLD NAME
                lineCount++;
            }

            matrixStack.popMatrix();
        } else {
            int textStart = 5;
            int lineCount = 0;
            int lineHeight = 10;

            if (this.options.showCoordInfo.get() != OptionEnumMinimap.CoordInfo.OFF) {
                int heading = (int) (this.direction + this.rotationFactor);
                if (heading > 360) {
                    heading -= 360;
                }
                String ns = "";
                String ew = "";
                if (heading > 360 - 67.5 || heading <= 67.5) {
                    ns = "north";
                } else if (heading > 180 - 67.5 && heading <= 180 + 67.5) {
                    ns = "south";
                }
                if (heading > 90 - 67.5 && heading <= 90 + 67.5) {
                    ew = "east";
                } else if (heading > 270 - 67.5 && heading <= 270 + 67.5) {
                    ew = "west";
                }

                String direction = I18n.get("minimap.ui." + ns + ew);
                String stats = "(" + this.dCoord(GameVariableAccessShim.xCoord()) + ", " + this.dCoord(GameVariableAccessShim.yCoord()) + ", " + this.dCoord(GameVariableAccessShim.zCoord()) + ") " + heading + "' " + direction;
                pass.drawCenteredString(matrixStack, stats, (this.scWidth * scaleProj / 2.0F), textStart + lineHeight * lineCount, MAP_TEXT_DEPTH, 0xFFFFFFFF, true);
                lineCount++;
            }
            if (!this.message.isEmpty()) {
                pass.drawCenteredString(matrixStack, this.message, (this.scWidth * scaleProj / 2.0F), textStart + lineHeight * lineCount, MAP_TEXT_DEPTH, 0xFFFFFFFF, true);
                lineCount++;
            }
        }
    }

    private String dCoord(int paramInt1) {
        if (paramInt1 < 0) {
            return "-" + Math.abs(paramInt1);
        } else {
            return paramInt1 > 0 ? "+" + paramInt1 : String.valueOf(paramInt1);
        }
    }

    private void showMessage(String str) {
        this.message = str;
        this.messageTime = System.currentTimeMillis();
    }

    public static double getMinTablistOffset() {
        return minTablistOffset;
    }

    public static float getStatusIconOffset() {
        return statusIconOffset;
    }
}
