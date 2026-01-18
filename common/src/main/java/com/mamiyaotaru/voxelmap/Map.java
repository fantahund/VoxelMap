package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.gui.GuiAddWaypoint;
import com.mamiyaotaru.voxelmap.gui.GuiWaypoints;
import com.mamiyaotaru.voxelmap.gui.GuiWelcomeScreen;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.interfaces.AbstractMapData;
import com.mamiyaotaru.voxelmap.interfaces.IChangeObserver;
import com.mamiyaotaru.voxelmap.persistent.GuiPersistentMap;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.BiomeRepository;
import com.mamiyaotaru.voxelmap.util.BlockRepository;
import com.mamiyaotaru.voxelmap.util.CPULightmap;
import com.mamiyaotaru.voxelmap.util.ColorUtils;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.DynamicMoveableTexture;
import com.mamiyaotaru.voxelmap.util.FullMapData;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.LayoutVariables;
import com.mamiyaotaru.voxelmap.util.MapChunkCache;
import com.mamiyaotaru.voxelmap.util.MapUtils;
import com.mamiyaotaru.voxelmap.util.MutableBlockPos;
import com.mamiyaotaru.voxelmap.util.MutableBlockPosCache;
import com.mamiyaotaru.voxelmap.util.ScaledDynamicMutableTexture;
import com.mamiyaotaru.voxelmap.util.VoxelMapCachedOrthoProjectionMatrixBuffer;
import com.mamiyaotaru.voxelmap.util.VoxelMapGuiGraphics;
import com.mamiyaotaru.voxelmap.util.VoxelMapPipelines;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.OutOfMemoryScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
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
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.OptionalInt;
import java.util.Random;
import java.util.TreeSet;

public class Map implements Runnable, IChangeObserver {
    private final Minecraft minecraft = Minecraft.getInstance();
    // private final float[] lastLightBrightnessTable = new float[16];
    private final Object coordinateLock = new Object();
    private final Identifier resourceArrow = Identifier.fromNamespaceAndPath("voxelmap", "images/minimap_arrow.png");
    private final Identifier resourceSquareMapFrame = Identifier.fromNamespaceAndPath("voxelmap", "images/square_map_frame.png");
    private final Identifier resourceSquareMapStencil = Identifier.fromNamespaceAndPath("voxelmap", "images/square_map_stencil.png");
    private final Identifier resourceRoundMapFrame = Identifier.fromNamespaceAndPath("voxelmap", "images/round_map_frame.png");
    private final Identifier resourceRoundMapStencil = Identifier.fromNamespaceAndPath("voxelmap", "images/round_map_stencil.png");
    private ClientLevel world;
    private final MapSettingsManager options;
    private final LayoutVariables layoutVariables;
    private final ColorManager colorManager;
    private final WaypointManager waypointManager;
    private final int availableProcessors = Runtime.getRuntime().availableProcessors();
    private final boolean multicore = this.availableProcessors > 1;
    private final int heightMapResetHeight = this.multicore ? 2 : 5;
    private final int heightMapResetTime = this.multicore ? 300 : 3000;
    private final boolean threading = this.multicore;
    private BlockState transparentBlockState;
    private BlockState surfaceBlockState;
    private boolean imageChanged = true;
    private LightTexture lightmapTexture;
    private boolean needLightmapRefresh = true;
    private int tickWithLightChange;
    private boolean lastPaused = true;
    private double lastGamma;
    private float lastSunBrightness;
    private float lastLightning;
    private float lastPotion;
    private final int[] lastLightmapValues = { -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216 };
    private boolean lastBeneathRendering;
    private boolean needSkyColor;
    private boolean lastAboveHorizon = true;
    private int lastBiome;
    private int lastSkyColor;
    private final Random generator = new Random();
    private Screen lastGuiScreen;
    private boolean fullscreenMap;
    private int zoom;
    private int scWidth;
    private int scHeight;
    private int heightMapFudge;
    private int timer;
    private boolean doFullRender = true;
    private boolean zoomChanged;
    private int lastX;
    private int lastZ;
    private int lastY;
    private int lastImageX;
    private int lastImageZ;
    private boolean lastFullscreen;
    private float direction;
    private int northRotate;
    private Thread zCalc = new Thread(this, "Voxelmap LiveMap Calculation Thread");
    private int zCalcTicker;
    private int[] lightmapColors = new int[256];
    private double zoomScale = 1.0;
    private double zoomScaleAdjusted = 1.0;
    private static double minTablistOffset;
    private static float statusIconOffset = 0.0F;
    private String message = "";
    private long messageTime;

    private final int mapDataCount = 5;
    private final FullMapData[] mapData = new FullMapData[this.mapDataCount];
    private final MapChunkCache[] chunkCache = new MapChunkCache[this.mapDataCount];
    private DynamicMoveableTexture[] mapImages;
    private Identifier[] mapResources;
    private final DynamicMoveableTexture[] mapImagesFiltered = new DynamicMoveableTexture[this.mapDataCount];
    private final DynamicMoveableTexture[] mapImagesUnfiltered = new DynamicMoveableTexture[this.mapDataCount];
    private final Identifier[] resourceMapImageFiltered = new Identifier[this.mapDataCount];
    private final Identifier[] resourceMapImageUnfiltered = new Identifier[this.mapDataCount];

    private GpuTexture fboTexture;
    private GpuTextureView fboTextureView;
    private Tesselator fboTessellator = new Tesselator(4096);
    private VoxelMapCachedOrthoProjectionMatrixBuffer projection;

    public Map() {
        this.options = VoxelConstants.getVoxelMapInstance().getMapOptions();
        this.colorManager = VoxelConstants.getVoxelMapInstance().getColorManager();
        this.waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        this.layoutVariables = new LayoutVariables();
        ArrayList<KeyMapping> tempBindings = new ArrayList<>();
        tempBindings.addAll(Arrays.asList(minecraft.options.keyMappings));
        tempBindings.addAll(Arrays.asList(this.options.keyBindings));
        minecraft.options.keyMappings = tempBindings.toArray(new KeyMapping[0]);

        this.zCalc.start();

        for (int i = 0; i < this.mapDataCount; i++) {
            resourceMapImageFiltered[i] = Identifier.fromNamespaceAndPath("voxelmap", String.format("map/filtered/%s", i));
            resourceMapImageUnfiltered[i] = Identifier.fromNamespaceAndPath("voxelmap", String.format("map/unfiltered/%s", i));

            int resolution = 1 << (i + 5); // 32, 64, ...
            int chunks = (1 << (i + 1)) + 1; //3, 5, ...

            this.mapData[i] = new FullMapData(resolution, resolution);
            this.chunkCache[i] = new MapChunkCache(chunks, chunks, this);

            this.mapImagesFiltered[i] = new DynamicMoveableTexture(String.format("voxelmap-map-%s", resolution), resolution, resolution, true);
            this.mapImagesFiltered[i].sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
            minecraft.getTextureManager().register(resourceMapImageFiltered[i], this.mapImagesFiltered[i]);

            this.mapImagesUnfiltered[i] = new ScaledDynamicMutableTexture(String.format("voxelmap-map-unfiltered-%s", resolution), resolution, resolution, true);
            this.mapImagesUnfiltered[i].sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
            minecraft.getTextureManager().register(resourceMapImageUnfiltered[i], this.mapImagesUnfiltered[i]);

        }

        if (this.options.filtering) {
            this.mapImages = this.mapImagesFiltered;
            this.mapResources = resourceMapImageFiltered;
        } else {
            this.mapImages = this.mapImagesUnfiltered;
            this.mapResources = resourceMapImageUnfiltered;
        }

        this.zoom = this.options.zoom;
        this.setZoomScale();

        final int fboTextureSize = 512;
        this.fboTexture = RenderSystem.getDevice().createTexture("voxelmap-fbotexture", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.RGBA8, fboTextureSize, fboTextureSize, 1, 1);
        this.fboTextureView = RenderSystem.getDevice().createTextureView(this.fboTexture);
        // DynamicTexture fboTexture = new DynamicTexture("voxelmap-fbotexture", fboTextureSize, fboTextureSize, true);
        // minecraft.getTextureManager().register(resourceFboTexture, fboTexture);
        // this.fboTexture = fboTexture.getTexture();
        this.projection = new VoxelMapCachedOrthoProjectionMatrixBuffer("VoxelMap Map To Screen Proj", -256.0F, 256.0F, 256.0F, -256.0F, 1000.0F, 21000.0F);

        this.loadMapTextures();
    }

    public void onResourceManagerReload(ResourceManager resourceManager) {
        this.loadMapTextures();
    }

    private void loadMapTextures() {
        boolean arrowFiltering = Boolean.parseBoolean(VoxelConstants.getVoxelMapInstance().getImageProperties().getProperty("minimap_arrow_filtering", "true"));
        FilterMode arrowFilterMode = arrowFiltering ? FilterMode.LINEAR : FilterMode.NEAREST;

        boolean frameFiltering = Boolean.parseBoolean(VoxelConstants.getVoxelMapInstance().getImageProperties().getProperty("minimap_frame_filtering", "true"));
        FilterMode frameFilterMode = frameFiltering ? FilterMode.LINEAR : FilterMode.NEAREST;

        try {
            DynamicTexture arrowTexture = new DynamicTexture(() -> "Minimap Arrow", TextureContents.load(Minecraft.getInstance().getResourceManager(), resourceArrow).image());
            arrowTexture.sampler = RenderSystem.getSamplerCache().getClampToEdge(arrowFilterMode);
            minecraft.getTextureManager().register(resourceArrow, arrowTexture);

            DynamicTexture squareMapTexture = new DynamicTexture(() -> "Minimap Square Map Frame", TextureContents.load(Minecraft.getInstance().getResourceManager(), resourceSquareMapFrame).image());
            squareMapTexture.sampler = RenderSystem.getSamplerCache().getClampToEdge(frameFilterMode);
            minecraft.getTextureManager().register(resourceSquareMapFrame, squareMapTexture);

            DynamicTexture roundMapTexture = new DynamicTexture(() -> "Minimap Round Map Frame", TextureContents.load(Minecraft.getInstance().getResourceManager(), resourceRoundMapFrame).image());
            roundMapTexture.sampler = RenderSystem.getSamplerCache().getClampToEdge(frameFilterMode);
            minecraft.getTextureManager().register(resourceRoundMapFrame, roundMapTexture);
        } catch (Exception exception) {
            VoxelConstants.getLogger().error("Failed getting map images " + exception.getLocalizedMessage(), exception);
        }
    }

    public void forceFullRender(boolean forceFullRender) {
        this.doFullRender = forceFullRender;
        VoxelConstants.getVoxelMapInstance().getSettingsAndLightingChangeNotifier().notifyOfChanges();
    }

    @Override
    public void run() {
        if (minecraft != null) {
            while (true) {
                if (this.world != null) {
                    if (!this.options.hide && this.options.minimapAllowed) {
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
                synchronized (this.zCalc) {
                    try {
                        this.zCalc.wait(0L);
                    } catch (InterruptedException exception) {
                        VoxelConstants.getLogger().error("Voxelmap LiveMap Calculation Thread", exception);
                    }
                }
            }
        }

    }

    public void newWorld(ClientLevel world) {
        this.world = world;
        this.lightmapTexture = this.getLightmapTexture();
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

    public void onTickInGame(GuiGraphics drawContext) {
        this.northRotate = this.options.oldNorth ? 90 : 0;

        if (this.lightmapTexture == null) {
            this.lightmapTexture = this.getLightmapTexture();
        }

        if (minecraft.screen == null && this.options.welcome) {
            minecraft.setScreen(new GuiWelcomeScreen(null));
        }

        if (minecraft.screen == null && this.options.keyBindMenu.consumeClick()) {
            minecraft.setScreen(new GuiPersistentMap(null));
        }

        if (minecraft.screen == null && this.options.keyBindWaypointMenu.consumeClick()) {
            if (VoxelMap.mapOptions.waypointsAllowed) {
                minecraft.setScreen(new GuiWaypoints(null));
            }
        }

        if (minecraft.screen == null && this.options.keyBindWaypoint.consumeClick()) {
            if (VoxelMap.mapOptions.waypointsAllowed) {
                float r;
                float g;
                float b;
                if (this.waypointManager.getWaypoints().isEmpty()) {
                    r = 0.0F;
                    g = 1.0F;
                    b = 0.0F;
                } else {
                    r = this.generator.nextFloat();
                    g = this.generator.nextFloat();
                    b = this.generator.nextFloat();
                }

                TreeSet<DimensionContainer> dimensions = new TreeSet<>();
                dimensions.add(VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getPlayer().level()));
                double dimensionScale = VoxelConstants.getPlayer().level().dimensionType().coordinateScale();
                Waypoint newWaypoint = new Waypoint("", (int) (GameVariableAccessShim.xCoord() * dimensionScale), (int) (GameVariableAccessShim.zCoord() * dimensionScale), GameVariableAccessShim.yCoord(), true, r, g, b, "",
                        VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentSubworldDescriptor(false), dimensions);
                minecraft.setScreen(new GuiAddWaypoint(null, newWaypoint, false));
            }
        }

        if (minecraft.screen == null && this.options.keyBindMobToggle.consumeClick()) {
            VoxelConstants.getVoxelMapInstance().getRadarOptions().setOptionValue(EnumOptionsMinimap.SHOW_RADAR);
            this.options.saveAll();
        }

        if (minecraft.screen == null && this.options.keyBindWaypointToggle.consumeClick()) {
            this.options.toggleIngameWaypoints();
        }

        if (minecraft.screen == null && this.options.keyBindZoom.consumeClick()) {
            this.cycleZoomLevel();
        }

        if (minecraft.screen == null && this.options.keyBindFullscreen.consumeClick()) {
            this.fullscreenMap = !this.fullscreenMap;
            this.showMessage(I18n.get("minimap.ui.zoomLevel") + String.format(" (%sx)", 2.0 / this.zoomScale));
        }

        if (minecraft.screen == null && this.options.keyBindMinimapToggle.consumeClick()) {
            this.options.setOptionValue(EnumOptionsMinimap.HIDE_MINIMAP);
        }

        this.checkForChanges();
        if (VoxelMap.mapOptions.deathWaypointAllowed && minecraft.screen instanceof DeathScreen && !(this.lastGuiScreen instanceof DeathScreen)) {
            this.waypointManager.handleDeath();
        }

        this.lastGuiScreen = minecraft.screen;
        this.calculateCurrentLightAndSkyColor();
        if (this.threading) {
            if (!this.zCalc.isAlive()) {
                this.zCalc = new Thread(this, "Voxelmap LiveMap Calculation Thread");
                this.zCalc.start();
                this.zCalcTicker = 0;
            }

            if (!(minecraft.screen instanceof DeathScreen) && !(minecraft.screen instanceof OutOfMemoryScreen)) {
                ++this.zCalcTicker;
                if (this.zCalcTicker > 2000) {
                    this.zCalcTicker = 0;
                    Exception ex = new Exception();
                    ex.setStackTrace(this.zCalc.getStackTrace());
                    DebugRenderState.print();
                    VoxelConstants.getLogger().error("Voxelmap LiveMap Calculation Thread is hanging?", ex);
                }
                synchronized (this.zCalc) {
                    this.zCalc.notify();
                }
            }
        } else {
            if (!this.options.hide && this.options.minimapAllowed && this.world != null) {
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

        boolean enabled = !minecraft.options.hideGui && (this.options.showUnderMenus || minecraft.screen == null) && !minecraft.debugEntries.isOverlayVisible();

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

        if (enabled && VoxelMap.mapOptions.minimapAllowed) {
            this.drawMinimap(drawContext);
        }

        this.timer = this.timer > 5000 ? 0 : this.timer + 1;
    }

    private void cycleZoomLevel() {
        if (--this.zoom < 0) {
            this.zoom = this.mapDataCount - 1;
        }
        this.options.zoom = this.zoom;
        this.options.saveAll();
        this.zoomChanged = true;
        this.setZoomScale();
        this.doFullRender = true;
        this.showMessage(I18n.get("minimap.ui.zoomLevel") + String.format(" (%sx)", 2.0 / this.zoomScale));

    }

    private void setZoomScale() {
        this.zoomScale = Math.pow(2.0, this.zoom) / 2.0;
        if (this.options.squareMap && this.options.rotates) {
            this.zoomScaleAdjusted = this.zoomScale / 1.4142F;
        } else {
            this.zoomScaleAdjusted = this.zoomScale;
        }

    }

    private LightTexture getLightmapTexture() {
        return minecraft.gameRenderer.lightTexture();
    }

    public void calculateCurrentLightAndSkyColor() {
        try {
            if (this.world != null) {
                if (this.needLightmapRefresh && VoxelConstants.getElapsedTicks() != this.tickWithLightChange && !minecraft.isPaused() || this.options.realTimeTorches) {
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

                float potionEffect = 0.0F;
                if (VoxelConstants.getPlayer().hasEffect(MobEffects.NIGHT_VISION)) {
                    int duration = VoxelConstants.getPlayer().getEffect(MobEffects.NIGHT_VISION).getDuration();
                    potionEffect = duration > 200 ? 1.0F : 0.7F + Mth.sin((duration - 1.0F) * (float) Math.PI * 0.2F) * 0.3F;
                }

                if (this.lastPotion != potionEffect) {
                    this.lastPotion = potionEffect;
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

                boolean scheduledUpdate = (this.timer - 50) % 50 == 0;
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
        Vector4f color = Minecraft.getInstance().gameRenderer.fogRenderer.computeFogColor(minecraft.gameRenderer.getMainCamera(), 0.0F, this.world, minecraft.options.renderDistance().get(), minecraft.gameRenderer.getDarkenWorldAmount(0.0F));
        float r = color.x;
        float g = color.y;
        float b = color.z;
        if (!aboveHorizon) {
            return 0x0A000000 + (int) (r * 255.0F) * 65536 + (int) (g * 255.0F) * 256 + (int) (b * 255.0F);
        } else {
            int backgroundColor = 0xFF000000 + (int) (r * 255.0F) * 65536 + (int) (g * 255.0F) * 256 + (int) (b * 255.0F);
            int sunsetColor = minecraft.gameRenderer.getMainCamera().attributeProbe().getValue(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, 0.0f);
            return ColorUtils.colorAdder(sunsetColor, backgroundColor);
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

    public void drawMinimap(GuiGraphics drawContext) {
        int scScaleOrig = 1;

        while (minecraft.getWindow().getWidth() / (scScaleOrig + 1) >= 320 && minecraft.getWindow().getHeight() / (scScaleOrig + 1) >= 240) {
            ++scScaleOrig;
        }

        int scScale = Math.max(1, scScaleOrig + (this.fullscreenMap ? 0 : this.options.sizeModifier));
        double scaledWidthD = (double) minecraft.getWindow().getWidth() / scScale;
        double scaledHeightD = (double) minecraft.getWindow().getHeight() / scScale;
        this.scWidth = Mth.ceil(scaledWidthD);
        this.scHeight = Mth.ceil(scaledHeightD);
        float scaleProj = (float) (scScale) / minecraft.getWindow().getGuiScale();

        int mapX;
        if (this.options.mapCorner != 0 && this.options.mapCorner != 3) {
            mapX = this.scWidth - 37;
        } else {
            mapX = 37;
        }

        int mapY;
        if (this.options.mapCorner != 0 && this.options.mapCorner != 1) {
            mapY = this.scHeight - 37;
        } else {
            mapY = 37;
        }

        float statusIconOffset = 0.0F;
        if (VoxelMap.mapOptions.moveMapDownWhileStatusEffect) {
            if (this.options.mapCorner == 1 && !VoxelConstants.getPlayer().getActiveEffects().isEmpty()) {

                for (MobEffectInstance statusEffectInstance : VoxelConstants.getPlayer().getActiveEffects()) {
                    if (statusEffectInstance.showIcon()) {
                        if (statusEffectInstance.getEffect().value().isBeneficial()) {
                            statusIconOffset = Math.max(statusIconOffset, 24.0F);
                        } else {
                            statusIconOffset = 50.0F;
                        }
                    }
                }
                int scHeight = minecraft.getWindow().getGuiScaledHeight();
                float resFactor = (float) this.scHeight / scHeight;
                mapY += (int) (statusIconOffset * resFactor);
            }
        }
        Map.statusIconOffset = statusIconOffset;

        if (!this.options.hide) {
            if (this.fullscreenMap) {
                this.renderMapFull(drawContext, this.scWidth, this.scHeight, scaleProj);
                this.drawArrow(drawContext, this.scWidth / 2, this.scHeight / 2, scaleProj);
            } else {
                this.renderMap(drawContext, mapX, mapY, scScale, scaleProj);
                this.drawDirections(drawContext, mapX, mapY, scaleProj);
                this.drawArrow(drawContext, mapX, mapY, scaleProj);
            }
        }

        this.showCoords(drawContext, mapX, mapY, scaleProj);
    }

    private void checkForChanges() {
        boolean changed = false;
        if (this.colorManager.checkForChanges()) {
            changed = true;
        }

        if (this.options.isChanged()) {
            if (this.options.filtering) {
                this.mapImages = this.mapImagesFiltered;
                this.mapResources = resourceMapImageFiltered;
            } else {
                this.mapImages = this.mapImagesUnfiltered;
                this.mapResources = resourceMapImageUnfiltered;
            }

            changed = true;
            this.setZoomScale();
        }

        if (changed) {
            this.doFullRender = true;
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

        if (this.options.lightmap) {
            int torchOffset = this.options.realTimeTorches ? 8 : 0;
            for (int t = 0; t < 16; ++t) {
                int newValue = getLightmapColor(t, torchOffset);
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

        boolean nether = false;
        boolean caves = false;
        boolean netherPlayerInOpen;
        MutableBlockPos blockPos = MutableBlockPosCache.get();
        blockPos.setXYZ(this.lastX, Math.max(Math.min(GameVariableAccessShim.yCoord(), world.getMaxY() - 1), world.getMinY()), this.lastZ);
        if (VoxelConstants.getPlayer().level().dimensionType().hasCeiling()) {

            netherPlayerInOpen = world.getChunk(blockPos).getHeight(Heightmap.Types.MOTION_BLOCKING, blockPos.getX() & 15, blockPos.getZ() & 15) <= currentY;
            nether = currentY < 126;
            if (this.options.cavesAllowed && this.options.showCaves && currentY >= 126 && !netherPlayerInOpen) {
                caves = true;
            }
        } else if (world.dimensionType().cardinalLightType() == DimensionType.CardinalLightType.NETHER && !VoxelConstants.getClientWorld().dimensionType().hasSkyLight()) {
            boolean endPlayerInOpen = world.getChunk(blockPos).getHeight(Heightmap.Types.MOTION_BLOCKING, blockPos.getX() & 15, blockPos.getZ() & 15) <= currentY;
            if (this.options.cavesAllowed && this.options.showCaves && !endPlayerInOpen) {
                caves = true;
            }
        } else if (this.options.cavesAllowed && this.options.showCaves && world.getBrightness(LightLayer.SKY, blockPos) <= 0) {
            caves = true;
        }
        MutableBlockPosCache.release(blockPos);

        boolean beneathRendering = caves || nether;
        if (this.lastBeneathRendering != beneathRendering) {
            full = true;
        }

        this.lastBeneathRendering = beneathRendering;
        needHeightAndID = needHeightMap && (nether || caves);
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
                    color24 = this.getPixelColor(true, true, true, true, nether, caves, world, zoom, multi, startX, startZ, imageX, imageY);
                    this.mapImages[zoom].setRGB(imageX, imageY, color24);
                }
            }

            for (int imageY = 32 * multi - 1; imageY >= 0; --imageY) {
                for (int imageX = offsetX > 0 ? 32 * multi - offsetX : 0; imageX < (offsetX > 0 ? 32 * multi : -offsetX); ++imageX) {
                    color24 = this.getPixelColor(true, true, true, true, nether, caves, world, zoom, multi, startX, startZ, imageX, imageY);
                    this.mapImages[zoom].setRGB(imageX, imageY, color24);
                }
            }
        }

        if (full || this.options.heightmap && needHeightMap || needHeightAndID || this.options.lightmap && needLight || skyColorChanged) {
            for (int imageY = 32 * multi - 1; imageY >= 0; --imageY) {
                for (int imageX = 0; imageX < 32 * multi; ++imageX) {
                    color24 = this.getPixelColor(full, full || needHeightAndID, full, full || needLight || needHeightAndID, nether, caves, world, zoom, multi, startX, startZ, imageX, imageY);
                    this.mapImages[zoom].setRGB(imageX, imageY, color24);
                }
            }
        }

        if ((full || offsetX != 0 || offsetZ != 0 || !this.lastFullscreen) && this.fullscreenMap && this.options.biomeOverlay != 0) {
            this.mapData[zoom].segmentBiomes();
            this.mapData[zoom].findCenterOfSegments(!this.options.oldNorth);
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
        this.rectangleCalc(chunk.getPos().x * 16, chunk.getPos().z * 16, chunk.getPos().x * 16 + 15, chunk.getPos().z * 16 + 15);
    }

    private void rectangleCalc(int left, int top, int right, int bottom) {
        boolean nether = false;
        boolean caves = false;
        boolean netherPlayerInOpen;
        MutableBlockPos blockPos = MutableBlockPosCache.get();
        blockPos.setXYZ(this.lastX, Math.max(Math.min(GameVariableAccessShim.yCoord(), world.getMaxY()), world.getMinY()), this.lastZ);
        int currentY = GameVariableAccessShim.yCoord();
        if (VoxelConstants.getPlayer().level().dimensionType().hasCeiling()) {
            netherPlayerInOpen = this.world.getChunk(blockPos).getHeight(Heightmap.Types.MOTION_BLOCKING, blockPos.getX() & 15, blockPos.getZ() & 15) <= currentY;
            nether = currentY < 126;
            if (this.options.cavesAllowed && this.options.showCaves && currentY >= 126 && !netherPlayerInOpen) {
                caves = true;
            }
        } else if (world.dimensionType().cardinalLightType() == DimensionType.CardinalLightType.NETHER && !world.dimensionType().hasSkyLight()) {
            boolean endPlayerInOpen = this.world.getChunk(blockPos).getHeight(Heightmap.Types.MOTION_BLOCKING, blockPos.getX() & 15, blockPos.getZ() & 15) <= currentY;
            if (this.options.cavesAllowed && this.options.showCaves && !endPlayerInOpen) {
                caves = true;
            }
        } else if (this.options.cavesAllowed && this.options.showCaves && this.world.getBrightness(LightLayer.SKY, blockPos) <= 0) {
            caves = true;
        }
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
                color24 = this.getPixelColor(true, true, true, true, nether, caves, world, zoom, multi, startX, startZ, imageX, imageY);
                this.mapImages[zoom].setRGB(imageX, imageY, color24);
            }
        }

        this.imageChanged = true;
    }

    private int getPixelColor(boolean needBiome, boolean needHeightAndID, boolean needTint, boolean needLight, boolean nether, boolean caves, ClientLevel world, int zoom, int multi, int startX, int startZ, int imageX, int imageY) {
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

        if (this.options.biomeOverlay == 1) {
            if (biome != null) {
                color24 = ARGB.toABGR(BiomeRepository.getBiomeColor(biome) | 0xFF000000);
            } else {
                color24 = 0;
            }

        } else {
            boolean solid = false;
            if (needHeightAndID) {
                if (!nether && !caves) {
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
                    boolean hasOpacity = this.surfaceBlockState.getLightBlock() > 0;
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

                        hasOpacity = this.surfaceBlockState.getLightBlock() > 0;
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

                        for (seafloorBlockState = world.getBlockState(blockPos.withXYZ(startX + imageX, surfaceHeight - 1, startZ + imageY)); seafloorBlockState.getLightBlock() < 5 && !(seafloorBlockState.getBlock() instanceof LeavesBlock)
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
                if (this.options.biomes && this.surfaceBlockState != this.mapData[zoom].getBlockstate(imageX, imageY)) {
                    surfaceBlockChangeForcedTint = true;
                }

                this.mapData[zoom].setHeight(imageX, imageY, surfaceHeight);
                this.mapData[zoom].setBlockstateID(imageX, imageY, surfaceBlockStateID);
                if (this.options.biomes && this.transparentBlockState != this.mapData[zoom].getTransparentBlockstate(imageX, imageY)) {
                    transparentBlockChangeForcedTint = true;
                }

                this.mapData[zoom].setTransparentHeight(imageX, imageY, transparentHeight);
                transparentBlockStateID = BlockRepository.getStateId(this.transparentBlockState);
                this.mapData[zoom].setTransparentBlockstateID(imageX, imageY, transparentBlockStateID);
                if (this.options.biomes && foliageBlockState != this.mapData[zoom].getFoliageBlockstate(imageX, imageY)) {
                    foliageBlockChangeForcedTint = true;
                }

                this.mapData[zoom].setFoliageHeight(imageX, imageY, foliageHeight);
                foliageBlockStateID = BlockRepository.getStateId(foliageBlockState);
                this.mapData[zoom].setFoliageBlockstateID(imageX, imageY, foliageBlockStateID);
                if (this.options.biomes && seafloorBlockState != this.mapData[zoom].getOceanFloorBlockstate(imageX, imageY)) {
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

            if (this.options.biomes) {
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

            surfaceColor = this.applyHeight(surfaceColor, nether, caves, world, zoom, multi, startX, startZ, imageX, imageY, surfaceHeight, solid, 1);
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

            if (this.options.waterTransparency && seafloorHeight != Short.MIN_VALUE) {
                if (!this.options.biomes) {
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

                seafloorColor = this.applyHeight(seafloorColor, nether, caves, world, zoom, multi, startX, startZ, imageX, imageY, seafloorHeight, solid, 0);
                int seafloorLight;
                if (needLight) {
                    seafloorLight = this.getLight(seafloorColor, seafloorBlockState, world, startX + imageX, startZ + imageY, seafloorHeight, solid);
                    blockPos.setXYZ(startX + imageX, seafloorHeight, startZ + imageY);
                    BlockState blockStateAbove = world.getBlockState(blockPos);
                    Block materialAbove = blockStateAbove.getBlock();
                    if (this.options.lightmap && materialAbove == Blocks.ICE) {
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

            if (this.options.blockTransparency) {
                if (transparentHeight != Short.MIN_VALUE && this.transparentBlockState != null && this.transparentBlockState != BlockRepository.air.defaultBlockState()) {
                    if (this.options.biomes) {
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

                    transparentColor = this.applyHeight(transparentColor, nether, caves, world, zoom, multi, startX, startZ, imageX, imageY, transparentHeight, solid, 3);
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
                    if (!this.options.biomes) {
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

                    foliageColor = this.applyHeight(foliageColor, nether, caves, world, zoom, multi, startX, startZ, imageX, imageY, foliageHeight, solid, 2);
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

            if (this.options.biomeOverlay == 2) {
                int bc = 0;
                if (biome != null) {
                    bc = ARGB.toABGR(BiomeRepository.getBiomeColor(biome));
                }

                bc = 2130706432 | bc;
                color24 = ColorUtils.colorAdder(bc, color24);
            }

        }
        MutableBlockPosCache.release(blockPos);
        MutableBlockPosCache.release(tempBlockPos);
        return MapUtils.doSlimeAndGrid(color24, world, startX + imageX, startZ + imageY);
    }

    private int getBlockHeight(boolean nether, boolean caves, Level world, int x, int z) {
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

        while (blockState.getLightBlock() == 0 && height > world.getMinY()) {
            --height;
            blockState = world.getBlockState(blockPos.withXYZ(x, height - 1, z));
            fluidState = this.surfaceBlockState.getFluidState();
            if (fluidState != Fluids.EMPTY.defaultFluidState()) {
                blockState = fluidState.createLegacyBlock();
            }
        }
        MutableBlockPosCache.release(blockPos);
        return (nether || caves) && height > playerHeight ? this.getNetherHeight(x, z) : height;
    }

    private int getNetherHeight(int x, int z) {
        MutableBlockPos blockPos = MutableBlockPosCache.get();
        int y = this.lastY;
        blockPos.setXYZ(x, y, z);
        BlockState blockState = this.world.getBlockState(blockPos);
        if (blockState.getLightBlock() == 0 && blockState.getBlock() != Blocks.LAVA) {
            while (y > world.getMinY()) {
                --y;
                blockPos.setXYZ(x, y, z);
                blockState = this.world.getBlockState(blockPos);
                if (blockState.getLightBlock() > 0 || blockState.getBlock() == Blocks.LAVA) {
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
                if (blockState.getLightBlock() == 0 && blockState.getBlock() != Blocks.LAVA) {
                    MutableBlockPosCache.release(blockPos);
                    return y;
                }
            }
            MutableBlockPosCache.release(blockPos);
            return this.world.getMinY() - 1;
        }
    }

    private int getSeafloorHeight(Level world, int x, int z, int height) {
        MutableBlockPos blockPos = MutableBlockPosCache.get();
        for (BlockState blockState = world.getBlockState(blockPos.withXYZ(x, height - 1, z)); blockState.getLightBlock() < 5 && !(blockState.getBlock() instanceof LeavesBlock) && height > world.getMinY() + 1; blockState = world.getBlockState(blockPos.withXYZ(x, height - 1, z))) {
            --height;
        }
        MutableBlockPosCache.release(blockPos);
        return height;
    }

    private int getTransparentHeight(boolean nether, boolean caves, Level world, int x, int z, int height) {
        MutableBlockPos blockPos = MutableBlockPosCache.get();
        int transHeight;
        if (!caves && !nether) {
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

    private int applyHeight(int color24, boolean nether, boolean caves, Level world, int zoom, int multi, int startX, int startZ, int imageX, int imageY, int height, boolean solid, int layer) {
        if (color24 != this.colorManager.getAirColor() && color24 != 0 && (this.options.heightmap || this.options.slopemap) && !solid) {
            int heightComp = -1;
            int diff;
            double sc = 0.0;
            if (!this.options.slopemap) {
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
                        int baseHeight = this.getBlockHeight(nether, caves, world, startX + imageX - 1, startZ + imageY + 1);
                        heightComp = this.getSeafloorHeight(world, startX + imageX - 1, startZ + imageY + 1, baseHeight);
                    }

                    if (layer == 1) {
                        heightComp = this.getBlockHeight(nether, caves, world, startX + imageX - 1, startZ + imageY + 1);
                    }

                    if (layer == 2) {
                        heightComp = height;
                    }

                    if (layer == 3) {
                        int baseHeight = this.getBlockHeight(nether, caves, world, startX + imageX - 1, startZ + imageY + 1);
                        heightComp = this.getTransparentHeight(nether, caves, world, startX + imageX - 1, startZ + imageY + 1, baseHeight);
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

                if (this.options.heightmap) {
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
        } else if (color24 != this.colorManager.getAirColor() && color24 != 0 && this.options.lightmap) {
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

    private void renderMap(GuiGraphics guiGraphics, int x, int y, int scScale, float scaleProj) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().scale(scaleProj, scaleProj);

        float scale = 1.0F;
        if (this.options.squareMap && this.options.rotates) {
            scale = 1.4142F;
        }

        synchronized (this.coordinateLock) {
            if (this.imageChanged) {
                this.imageChanged = false;
                this.mapImages[this.zoom].upload();
                this.lastImageX = this.lastX;
                this.lastImageZ = this.lastZ;
            }
        }
        //
        float multi = (float) (1.0 / this.zoomScale);
        float percentX = (float) (GameVariableAccessShim.xCoordDouble() - this.lastImageX) * multi;
        float percentY = (float) (GameVariableAccessShim.zCoordDouble() - this.lastImageZ) * multi;

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().identity();

        BufferBuilder bufferBuilder = fboTessellator.begin(Mode.QUADS, RenderPipelines.GUI_TEXTURED.getVertexFormat());

        bufferBuilder.addVertex(-256, 256, -2500).setUv(0, 0).setColor(255, 255, 255, 255);
        bufferBuilder.addVertex(256, 256, -2500).setUv(1, 0).setColor(255, 255, 255, 255);
        bufferBuilder.addVertex(256, -256, -2500).setUv(1, 1).setColor(255, 255, 255, 255);
        bufferBuilder.addVertex(-256, -256, -2500).setUv(0, 1).setColor(255, 255, 255, 255);

        // guiGraphics.pose().translate(256, 256);
        if (!this.options.rotates) {
            guiGraphics.pose().rotate(-this.northRotate * Mth.DEG_TO_RAD);
        } else {
            guiGraphics.pose().rotate(this.direction * Mth.DEG_TO_RAD);
        }
        guiGraphics.pose().scale(scale, scale);
        // guiGraphics.pose().translate(-256, -256);
        guiGraphics.pose().translate(-percentX * 512.0F / 64.0F, percentY * 512.0F / 64.0F);

        Vector3f vector3f = new Vector3f();
        guiGraphics.pose().transform(-256, 256, 1, vector3f);
        bufferBuilder.addVertex(vector3f.x, vector3f.y, -2500).setUv(0, 0).setColor(255, 255, 255, 255);

        guiGraphics.pose().transform(256, 256, 1, vector3f);
        bufferBuilder.addVertex(vector3f.x, vector3f.y, -2500).setUv(1, 0).setColor(255, 255, 255, 255);

        guiGraphics.pose().transform(256, -256, 1, vector3f);
        bufferBuilder.addVertex(vector3f.x, vector3f.y, -2500).setUv(1, 1).setColor(255, 255, 255, 255);

        guiGraphics.pose().transform(-256, -256, 1, vector3f);
        bufferBuilder.addVertex(vector3f.x, vector3f.y, -2500).setUv(0, 1).setColor(255, 255, 255, 255);

        ProjectionType originalProjectionType = RenderSystem.getProjectionType();
        GpuBufferSlice originalProjectionMatrix = RenderSystem.getProjectionMatrixBuffer();
        RenderSystem.setProjectionMatrix(projection.getBuffer(), ProjectionType.ORTHOGRAPHIC);
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();

        GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
                .writeTransform(
                        RenderSystem.getModelViewMatrix(),
                        new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                        new Vector3f(),
                        new Matrix4f());

        RenderPipeline renderPipeline = VoxelMapPipelines.GUI_TEXTURED_ANY_DEPTH_PIPELINE;
        try (MeshData meshData = bufferBuilder.build()) {
            GpuBuffer vertexBuffer = renderPipeline.getVertexFormat().uploadImmediateVertexBuffer(meshData.vertexBuffer());
            GpuBuffer indexBuffer;
            VertexFormat.IndexType indexType;
            if (meshData.indexBuffer() == null) {
                RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(meshData.drawState().mode());
                indexBuffer = autoStorageIndexBuffer.getBuffer(meshData.drawState().indexCount());
                indexType = autoStorageIndexBuffer.type();
            } else {
                indexBuffer = renderPipeline.getVertexFormat().uploadImmediateIndexBuffer(meshData.indexBuffer());
                indexType = meshData.drawState().indexType();
            }

            AbstractTexture stencilTexture = null;
            if (this.options.squareMap) {
                stencilTexture = Minecraft.getInstance().getTextureManager().getTexture(resourceSquareMapStencil);
            } else {
                stencilTexture = Minecraft.getInstance().getTextureManager().getTexture(resourceRoundMapStencil);
            }

            try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Voxelmap: Map to screen", fboTextureView, OptionalInt.of(0x00000000))) {
                renderPass.setPipeline(renderPipeline);
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                renderPass.setVertexBuffer(0, vertexBuffer);
                renderPass.setIndexBuffer(indexBuffer, indexType);

                renderPass.bindTexture("Sampler0", stencilTexture.getTextureView(), stencilTexture.getSampler());
                renderPass.drawIndexed(0, 0, meshData.drawState().indexCount() / 2, 1);
                renderPass.setPipeline(VoxelMapPipelines.GUI_TEXTURED_ANY_DEPTH_DST_ALPHA_PIPELINE);

                renderPass.bindTexture("Sampler0", mapImages[this.zoom].getTextureView(), mapImages[this.zoom].getSampler());
                renderPass.drawIndexed(0, meshData.drawState().indexCount() / 2, meshData.drawState().indexCount() / 2, 1);
            }
        }
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.setProjectionMatrix(originalProjectionMatrix, originalProjectionType);
        fboTessellator.clear();
        // if (((saved++) % 1000) == 0)
        // ImageUtils.saveImage("minimap_" + saved, fboTexture);

        guiGraphics.pose().popMatrix();

        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, fboTextureView, x - 32, y - 32, 64, 64, 0, 1, 0, 1, 0xffffffff);

        if (VoxelConstants.getVoxelMapInstance().getRadar() != null) {
            this.layoutVariables.updateVars(scScale, x, y, this.zoomScale, this.zoomScaleAdjusted);
            VoxelConstants.getVoxelMapInstance().getRadar().onTickInGame(guiGraphics, this.layoutVariables, 1.0F);
        }

        double guiScale = (double) minecraft.getWindow().getWidth() / this.scWidth;
        minTablistOffset = guiScale * 63;
        this.drawMapFrame(guiGraphics, x, y, this.options.squareMap);

        double lastXDouble = GameVariableAccessShim.xCoordDouble();
        double lastZDouble = GameVariableAccessShim.zCoordDouble();
        TextureAtlas textureAtlas = VoxelConstants.getVoxelMapInstance().getWaypointManager().getTextureAtlas();
        if (VoxelMap.mapOptions.waypointsAllowed) {
            Waypoint highlightedPoint = this.waypointManager.getHighlightedWaypoint();

            for (Waypoint pt : this.waypointManager.getWaypoints()) {
                if (pt.isActive() || pt == highlightedPoint) {
                    double distanceSq = pt.getDistanceSqToEntity(minecraft.getCameraEntity());
                    if (distanceSq < (this.options.maxWaypointDisplayDistance * this.options.maxWaypointDisplayDistance) || this.options.maxWaypointDisplayDistance < 0 || pt == highlightedPoint) {
                        this.drawWaypoint(guiGraphics, pt, textureAtlas, x, y, lastXDouble, lastZDouble, null, false);
                    }
                }
            }

            if (highlightedPoint != null) {
                this.drawWaypoint(guiGraphics, highlightedPoint, textureAtlas, x, y, lastXDouble, lastZDouble, textureAtlas.getAtlasSprite("marker/target"), true);
            }
        }
        guiGraphics.pose().popMatrix();
    }

    private void drawWaypoint(GuiGraphics guiGraphics, Waypoint pt, TextureAtlas textureAtlas, int x, int y, double lastXDouble, double lastZDouble, Sprite icon, boolean highlight) {
        boolean uprightIcon = icon != null;

        double wayX = lastXDouble - pt.getX() - 0.5;
        double wayY = lastZDouble - pt.getZ() - 0.5;
        float locate = (float) Math.toDegrees(Math.atan2(wayX, wayY));
        float hypot = (float) Math.sqrt(wayX * wayX + wayY * wayY);
        boolean far;
        if (this.options.rotates) {
            locate += this.direction;
        } else {
            locate -= this.northRotate;
        }

        hypot /= this.zoomScaleAdjusted;
        if (this.options.squareMap) {
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

        boolean target = false;
        if (far) {
            if (icon == null) {
                icon = textureAtlas.getAtlasSprite("marker/" + pt.imageSuffix);

                if (icon == textureAtlas.getMissingImage()) {
                    icon = textureAtlas.getAtlasSprite("marker/arrow");
                }
            } else {
                target = true;
            }
            int color = highlight ? 0xFFFF0000 : pt.getUnifiedColor(!pt.enabled && !target ? 0.3F : 1.0F);

            try {
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(x, y);
                guiGraphics.pose().rotate(-locate * Mth.DEG_TO_RAD);
                if (uprightIcon) {
                    guiGraphics.pose().translate(0.0f, -hypot);
                    guiGraphics.pose().rotate(locate * Mth.DEG_TO_RAD);
                    guiGraphics.pose().translate(-x, -y);
                } else {
                    guiGraphics.pose().translate(-x, -y);
                    guiGraphics.pose().translate(0.0f, -hypot);
                }

                icon.blit(guiGraphics, VoxelMapPipelines.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH_PIPELINE, x - 4, y - 4, 8, 8, color);
            } catch (Exception var40) {
                this.showMessage("Error: marker overlay not found!");
            } finally {
                guiGraphics.pose().popMatrix();
            }
        } else {
            if (icon == null) {
                icon = textureAtlas.getAtlasSprite("selectable/" + pt.imageSuffix);

                if (icon == textureAtlas.getMissingImage()) {
                    icon = textureAtlas.getAtlasSprite(WaypointManager.fallbackIconLocation);
                }
            } else {
                target = true;
            }
            int color = highlight ? 0xFFFF0000 : pt.getUnifiedColor(!pt.enabled && !target ? 0.3F : 1.0F);

            try {
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().rotate(-locate * Mth.DEG_TO_RAD);
                guiGraphics.pose().translate(0.0f, -hypot);
                guiGraphics.pose().rotate(locate * Mth.DEG_TO_RAD);

                icon.blit(guiGraphics, VoxelMapPipelines.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH_PIPELINE, x - 4, y - 4, 8, 8, color);
            } catch (Exception var42) {
                this.showMessage("Error: waypoint overlay not found!");
            } finally {
                guiGraphics.pose().popMatrix();
            }
        }
    }

    private void drawArrow(GuiGraphics guiGraphics, int x, int y, float scaleProj) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().scale(scaleProj, scaleProj);

        guiGraphics.pose().translate(x, y);
        guiGraphics.pose().rotate((this.options.rotates && !this.fullscreenMap ? 0.0F : this.direction + this.northRotate) * Mth.DEG_TO_RAD);
        guiGraphics.pose().translate(-x, -y);

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, resourceArrow, x - 4, y - 4, 0, 0, 8, 8, 8, 8);

        guiGraphics.pose().popMatrix();
    }

    private void renderMapFull(GuiGraphics guiGraphics, int scWidth, int scHeight, float scaleProj) {
        synchronized (this.coordinateLock) {
            if (this.imageChanged) {
                this.imageChanged = false;
                this.mapImages[this.zoom].upload();
                this.lastImageX = this.lastX;
                this.lastImageZ = this.lastZ;
            }
        }
        Matrix3x2fStack matrixStack = guiGraphics.pose();
        matrixStack.pushMatrix();
        matrixStack.scale(scaleProj, scaleProj);
        matrixStack.translate(scWidth / 2.0F, scHeight / 2.0F);
        matrixStack.rotate(this.northRotate * Mth.DEG_TO_RAD);
        matrixStack.translate(-(scWidth / 2.0F), -(scHeight / 2.0F));
        int left = scWidth / 2 - 128;
        int top = scHeight / 2 - 128;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, mapResources[this.zoom], left, top, 0, 0, 256, 256, 256, 256);
        matrixStack.popMatrix();

        if (this.options.biomeOverlay != 0) {
            double factor = Math.pow(2.0, 3 - this.zoom);
            int minimumSize = (int) Math.pow(2.0, this.zoom);
            minimumSize *= minimumSize;
            ArrayList<AbstractMapData.BiomeLabel> labels = this.mapData[this.zoom].getBiomeLabels();
            matrixStack.pushMatrix();

            for (AbstractMapData.BiomeLabel o : labels) {
                if (o.segmentSize > minimumSize) {
                    String name = o.name;
                    float x = (float) (o.x * factor);
                    float z = (float) (o.z * factor);
                    if (this.options.oldNorth) {
                        this.writeCentered(guiGraphics, name, (left + 256) - z, top + x - 3.0F, 0xFFFFFFFF, true);
                    } else {
                        this.writeCentered(guiGraphics, name, left + x, top + z - 3.0F, 0xFFFFFFFF, true);
                    }
                }
            }

            matrixStack.popMatrix();
        }
    }

    private void drawMapFrame(GuiGraphics guiGraphics, int x, int y, boolean squaremap) {
        Identifier frameResource = squaremap ? resourceSquareMapFrame : resourceRoundMapFrame;
        guiGraphics.blit(VoxelMapPipelines.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH_PIPELINE, frameResource, x - 32, y - 32, 0, 0, 64, 64, 64, 64);
    }

    private void drawDirections(GuiGraphics drawContext, int x, int y, float scaleProj) {
        Matrix3x2fStack poseStack = drawContext.pose();
        float scale = 0.5F;
        float rotate;
        if (this.options.rotates) {
            rotate = -this.direction - 90.0F - this.northRotate;
        } else {
            rotate = -90.0F;
        }

        float distance;
        if (this.options.squareMap) {
            if (this.options.rotates) {
                float tempdir = this.direction % 90.0F;
                tempdir = 45.0F - Math.abs(45.0F - tempdir);
                distance = 33.5F / scale / Mth.cos(Math.toRadians(tempdir));
            } else {
                distance = 33.5F / scale;
            }
        } else {
            distance = 32.5F / scale;
        }

        poseStack.pushMatrix();
        poseStack.scale(scaleProj, scaleProj);
        poseStack.scale(scale, scale);

        poseStack.pushMatrix();
        poseStack.translate(distance * Mth.sin(-(rotate - 90.0F) * Mth.DEG_TO_RAD), distance * Mth.cos(-(rotate - 90.0F) * Mth.DEG_TO_RAD));
        this.writeCentered(drawContext, "N", x / scale, y / scale - 4.0F, 0xFFFFFFFF, true);
        poseStack.popMatrix();
        poseStack.pushMatrix();
        poseStack.translate(distance * Mth.sin(-(rotate) * Mth.DEG_TO_RAD), distance * Mth.cos(-(rotate) * Mth.DEG_TO_RAD));
        this.writeCentered(drawContext, "E", x / scale, y / scale - 4.0F, 0xFFFFFFFF, true);
        poseStack.popMatrix();
        poseStack.pushMatrix();
        poseStack.translate(distance * Mth.sin(-(rotate + 90.0F) * Mth.DEG_TO_RAD), distance * Mth.cos(-(rotate + 90.0F) * Mth.DEG_TO_RAD));
        this.writeCentered(drawContext, "S", x / scale, y / scale - 4.0F, 0xFFFFFFFF, true);
        poseStack.popMatrix();
        poseStack.pushMatrix();
        poseStack.translate(distance * Mth.sin(-(rotate + 180.0F) * Mth.DEG_TO_RAD), distance * Mth.cos(-(rotate + 180.0F) * Mth.DEG_TO_RAD));
        this.writeCentered(drawContext, "W", x / scale, y / scale - 4.0F, 0xFFFFFFFF, true);
        poseStack.popMatrix();

        poseStack.popMatrix();
    }

    private void showCoords(GuiGraphics drawContext, int x, int y, float scaleProj) {
        Matrix3x2fStack matrixStack = drawContext.pose();

        if (!this.options.hide && !this.fullscreenMap) {
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
            matrixStack.scale(scale * scaleProj, scale * scaleProj);

            String coords;

            if (this.options.coords) {
                coords = this.dCoord(GameVariableAccessShim.xCoord()) + ", " + this.dCoord(GameVariableAccessShim.zCoord());
                this.writeCentered(drawContext, coords, x / scale, textStart / scale + lineHeight * lineCount, 0xFFFFFFFF, true); // X, Z
                lineCount++;

                coords = this.dCoord(GameVariableAccessShim.yCoord());
                this.writeCentered(drawContext, coords, x / scale, textStart / scale + lineHeight * lineCount, 0xFFFFFFFF, true); // Y
                lineCount++;
            }

            if (this.options.showBiome) {
                coords = BiomeRepository.getName(this.lastBiome);
                this.writeCentered(drawContext, coords, x / scale, textStart / scale + lineHeight * lineCount, 0xFFFFFFFF, true); // BIOME
                lineCount++;
            }

            if (!this.message.isEmpty()) {
                this.writeCentered(drawContext, this.message, x / scale, textStart / scale + lineHeight * lineCount, 0xFFFFFFFF, true); // WORLD NAME
                lineCount++;
            }

            matrixStack.popMatrix();
        } else {
            int heading = (int) (this.direction + this.northRotate);
            if (heading > 360) {
                heading -= 360;
            }
            String ns = "";
            String ew = "";
            if (heading > 360 - 67.5 || heading <= 67.5) {
                ns = "N";
            } else if (heading > 180 - 67.5 && heading <= 180 + 67.5) {
                ns = "S";
            }
            if (heading > 90 - 67.5 && heading <= 90 + 67.5) {
                ew = "E";
            } else if (heading > 270 - 67.5 && heading <= 270 + 67.5) {
                ew = "W";
            }

            String stats = "(" + this.dCoord(GameVariableAccessShim.xCoord()) + ", " + GameVariableAccessShim.yCoord() + ", " + this.dCoord(GameVariableAccessShim.zCoord()) + ") " + heading + "' " + ns + ew;
            this.writeCentered(drawContext, stats, (this.scWidth * scaleProj / 2.0F), 5.0F, 0xFFFFFFFF, true);
            if (!this.message.isEmpty()) {
                this.writeCentered(drawContext, this.message, (this.scWidth * scaleProj / 2.0F), 15.0F, 0xFFFFFFFF, true);
            }
        }
    }

    private String dCoord(int paramInt1) {
        if (paramInt1 < 0) {
            return "-" + Math.abs(paramInt1);
        } else {
            return paramInt1 > 0 ? "+" + paramInt1 : " " + paramInt1;
        }
    }

    private void showMessage(String str) {
        this.message = str;
        this.messageTime = System.currentTimeMillis();
    }

    private int textWidth(String string) {
        return minecraft.font.width(string);
    }

    private int textWidth(Component text) {
        return minecraft.font.width(text);
    }

    private void write(GuiGraphics drawContext, String text, float x, float y, int color, boolean shadow) {
        write(drawContext, Component.nullToEmpty(text), x, y, color, shadow);
    }

    private void write(GuiGraphics drawContext, Component text, float x, float y, int color, boolean shadow) {
        drawContext.drawString(minecraft.font, text, (int) x, (int) y, color, shadow);
    }

    private void writeCentered(GuiGraphics drawContext, String text, float x, float y, int color, boolean shadow) {
        writeCentered(drawContext, Component.nullToEmpty(text), x, y, color, shadow);
    }

    private void writeCentered(GuiGraphics drawContext, Component text, float x, float y, int color, boolean shadow) {
        drawContext.drawString(minecraft.font, text, (int) x - (textWidth(text) / 2), (int) y, color, shadow);
    }

    public static double getMinTablistOffset() {
        return minTablistOffset;
    }

    public static float getStatusIconOffset() {
        return statusIconOffset;
    }
}
