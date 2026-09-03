package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.ColorManager;
import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.SettingsAndLightingChangeNotifier;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.interfaces.AbstractMapData;
import com.mamiyaotaru.voxelmap.interfaces.IChangeObserver;
import com.mamiyaotaru.voxelmap.util.BiomeRepository;
import com.mamiyaotaru.voxelmap.util.BlockRepository;
import com.mamiyaotaru.voxelmap.util.ColorUtils;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.MapChunkCache;
import com.mamiyaotaru.voxelmap.util.MapUtils;
import com.mamiyaotaru.voxelmap.util.MutableBlockPos;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.ARGB;
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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PersistentMap implements IChangeObserver {
    static final float OVERVIEW_ZOOM_THRESHOLD = 0.25F;
    private static final long OVERVIEW_RENDER_VERSION = 7L;
    private static final int OVERVIEW_LIGHTING_UPDATES_PER_FRAME = 8;
    private static final int HEIGHT_SHADE_MIN = -1024;
    private static final int HEIGHT_SHADE_MAX = 1023;
    private static final double[] HEIGHTMAP_SHADE = createHeightShadeLookup(1.8, true);
    private static final double[] HEIGHTMAP_WITH_SLOPE_SHADE = createHeightShadeLookup(3.0, false);

    final MutableBlockPos blockPos = new MutableBlockPos(0, 0, 0);
    final ColorManager colorManager;
    final MapSettingsManager mapOptions;
    PersistentMapSettingsManager options;
    WorldMatcher worldMatcher;
    final int[] lightmapColors;
    ClientLevel world;
    String subworldName = "";
    protected final List<CachedRegion> cachedRegionsPool = Collections.synchronizedList(new ArrayList<>());
    protected final ConcurrentHashMap<String, CachedRegion> cachedRegions = new ConcurrentHashMap<>(150, 0.9F, 2);
    int lastLeft;
    int lastRight;
    int lastTop;
    int lastBottom;
    boolean lastFullDetailRequested;
    CachedRegion[] lastRegionsArray = new CachedRegion[0];
    private final Object lastRegionsLock = new Object();
    final Comparator<CachedRegion> ageThenDistanceSorter = (region1, region2) -> {
        long mostRecentAccess1 = region1.getMostRecentView();
        long mostRecentAccess2 = region2.getMostRecentView();
        if (mostRecentAccess1 < mostRecentAccess2) {
            return 1;
        } else if (mostRecentAccess1 > mostRecentAccess2) {
            return -1;
        } else {
            double distance1sq = (region1.getX() * 256 + region1.getWidth() / 2f - PersistentMap.this.options.mapX) * (region1.getX() * 256 + region1.getWidth() / 2f - PersistentMap.this.options.mapX) + (region1.getZ() * 256 + region1.getWidth() / 2f - PersistentMap.this.options.mapZ) * (region1.getZ() * 256 + region1.getWidth() / 2f - PersistentMap.this.options.mapZ);
            double distance2sq = (region2.getX() * 256 + region2.getWidth() / 2f - PersistentMap.this.options.mapX) * (region2.getX() * 256 + region2.getWidth() / 2f - PersistentMap.this.options.mapX) + (region2.getZ() * 256 + region2.getWidth() / 2f - PersistentMap.this.options.mapZ) * (region2.getZ() * 256 + region2.getWidth() / 2f - PersistentMap.this.options.mapZ);
            return Double.compare(distance1sq, distance2sq);
        }
    };
    final Comparator<RegionCoordinates> distanceSorter = (coordinates1, coordinates2) -> {
        double distance1sq = (coordinates1.x * 256 + 128 - PersistentMap.this.options.mapX) * (coordinates1.x * 256 + 128 - PersistentMap.this.options.mapX) + (coordinates1.z * 256 + 128 - PersistentMap.this.options.mapZ) * (coordinates1.z * 256 + 128 - PersistentMap.this.options.mapZ);
        double distance2sq = (coordinates2.x * 256 + 128 - PersistentMap.this.options.mapX) * (coordinates2.x * 256 + 128 - PersistentMap.this.options.mapX) + (coordinates2.z * 256 + 128 - PersistentMap.this.options.mapZ) * (coordinates2.z * 256 + 128 - PersistentMap.this.options.mapZ);
        return Double.compare(distance1sq, distance2sq);
    };
    private boolean queuedChangedChunks;
    private MapChunkCache chunkCache;
    private int lastRenderDistance;
    private final ConcurrentLinkedQueue<ChunkWithAge> chunkUpdateQueue = new ConcurrentLinkedQueue<>();
    private int overviewLightingUpdatesRemaining;
    private volatile long lightmapRevision;
    private final AtomicBoolean visibleRegionRefreshRequested = new AtomicBoolean();

    public PersistentMap() {
        this.colorManager = VoxelConstants.getVoxelMapInstance().getColorManager();
        mapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();
        this.options = VoxelConstants.getVoxelMapInstance().getPersistentMapOptions();
        this.lightmapColors = new int[256];
        Arrays.fill(this.lightmapColors, -16777216);
    }

    public void newWorld(ClientLevel world) {
        this.subworldName = "";
        this.purgeCachedRegions();
        this.queuedChangedChunks = false;
        this.chunkUpdateQueue.clear();
        this.world = world;
        if (this.worldMatcher != null) {
            this.worldMatcher.cancel();
        }

        if (world != null) {
            this.newWorldStuff();
        } else {
            Thread pauseForSubworldNamesThread = new Thread(null, null, "VoxelMap Pause for Subworld Name Thread") {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000L);
                    } catch (InterruptedException var2) {
                        VoxelConstants.getLogger().error(var2);
                    }

                    if (PersistentMap.this.world != null) {
                        PersistentMap.this.newWorldStuff();
                    }

                }
            };
            pauseForSubworldNamesThread.start();
        }

    }

    private void newWorldStuff() {
        String worldName = TextUtils.scrubNameFile(VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentWorldName());
        File oldCacheDir = new File(VoxelConstants.getMinecraft().gameDirectory, "/mods/mamiyaotaru/voxelmap/cache/" + worldName + "/");
        if (oldCacheDir.exists() && oldCacheDir.isDirectory()) {
            File newCacheDir = VoxelConstants.getVoxelMapInstance().getDataStore().getWorldCacheDir();
            newCacheDir.getParentFile().mkdirs();
            boolean success = oldCacheDir.renameTo(newCacheDir);
            if (!success) {
                VoxelConstants.getLogger().warn("Failed moving Voxelmap cache files.  Please move " + oldCacheDir.getPath() + " to " + newCacheDir.getPath());
            } else {
                VoxelConstants.getLogger().warn("Moved Voxelmap cache files from " + oldCacheDir.getPath() + " to " + newCacheDir.getPath());
            }
        }

        if (VoxelConstants.getVoxelMapInstance().getWaypointManager().isMultiworld() && !VoxelConstants.isSinglePlayer() && !VoxelConstants.getVoxelMapInstance().getWaypointManager().receivedAutoSubworldName()) {
            this.worldMatcher = new WorldMatcher(this, this.world);
            this.worldMatcher.findMatch();
        }

        this.createChunkCache(VoxelConstants.getMinecraft().options.renderDistance().get());
    }

    public void createChunkCache(int renderDistance) {
        int totalChunks = renderDistance * 2 + 1;
        this.chunkCache = new MapChunkCache(totalChunks, totalChunks, this);
    }

    public void onTick() {
        if (VoxelConstants.getMinecraft().getCameraEntity() == null) {
            return;
        }
        if (VoxelConstants.getMinecraft().gui.screen() == null) {
            this.options.mapX = GameVariableAccessShim.xCoord();
            this.options.mapZ = GameVariableAccessShim.zCoord();
        }

        if (!VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentSubworldDescriptor(false).equals(this.subworldName)) {
            this.subworldName = VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentSubworldDescriptor(false);
            if (this.worldMatcher != null && !this.subworldName.isEmpty()) {
                this.worldMatcher.cancel();
            }

            this.purgeCachedRegions();
        }

        if (this.queuedChangedChunks) {
            this.queuedChangedChunks = false;
            this.prunePool();
        }

        if (this.world != null) {
            int renderDistance = VoxelConstants.getMinecraft().options.renderDistance().get();
            if (renderDistance != this.lastRenderDistance) {
                this.lastRenderDistance = renderDistance;
                this.createChunkCache(renderDistance);
            }

            this.chunkCache.centerChunks(this.blockPos.withXYZ(GameVariableAccessShim.xCoord(), 0, GameVariableAccessShim.zCoord()));
            this.chunkCache.checkIfChunksBecameSurroundedByLoaded();

            while (!this.chunkUpdateQueue.isEmpty() && Math.abs(VoxelConstants.getElapsedTicks() - this.chunkUpdateQueue.peek().tick) >= 20) {
                this.doProcessChunk(this.chunkUpdateQueue.remove().chunk);
            }
        }

    }

    public PersistentMapSettingsManager getOptions() {
        return this.options;
    }

    public void purgeCachedRegions() {
        synchronized (this.cachedRegionsPool) {
            for (CachedRegion cachedRegion : this.cachedRegionsPool) {
                cachedRegion.cleanup();
            }

            this.cachedRegions.clear();
            this.cachedRegionsPool.clear();
            this.getRegions(0, -1, 0, -1);
        }
    }

    public void renameSubworld(String oldName, String newName) {
        synchronized (this.cachedRegionsPool) {
            for (CachedRegion cachedRegion : this.cachedRegionsPool) {
                cachedRegion.renameSubworld(oldName, newName);
            }

        }
    }

    public SettingsAndLightingChangeNotifier getSettingsAndLightingChangeNotifier() {
        return VoxelConstants.getVoxelMapInstance().getSettingsAndLightingChangeNotifier();
    }

    public void setLightMapArray(int[] lights) {
        boolean changed;
        synchronized (this) {
            changed = !Arrays.equals(lights, this.lightmapColors);
            System.arraycopy(lights, 0, this.lightmapColors, 0, 256);
            if (changed) {
                ++this.lightmapRevision;
            }
        }
        if (changed) {
            this.visibleRegionRefreshRequested.set(true);
            this.getSettingsAndLightingChangeNotifier().notifyOfLightingChanges();
        }

    }

    synchronized int[] getLightmapSnapshot() {
        return Arrays.copyOf(this.lightmapColors, this.lightmapColors.length);
    }

    synchronized LightmapSnapshot getLightmapSnapshotWithRevision() {
        return new LightmapSnapshot(Arrays.copyOf(this.lightmapColors, this.lightmapColors.length), this.lightmapRevision);
    }

    long getLightmapRevision() {
        return this.lightmapRevision;
    }

    public void requestVisibleRegionRefresh() {
        this.visibleRegionRefreshRequested.set(true);
    }

    void beginOverviewLightingFrame() {
        this.overviewLightingUpdatesRemaining = OVERVIEW_LIGHTING_UPDATES_PER_FRAME;
    }

    boolean tryAcquireOverviewLightingUpdate() {
        if (this.overviewLightingUpdatesRemaining <= 0) {
            return false;
        }
        --this.overviewLightingUpdatesRemaining;
        return true;
    }

    public void getAndStoreData(AbstractMapData mapData, Level world, LevelChunk chunk, MutableBlockPos pos, boolean underground, int startX, int startZ, int imageX, int imageY) {
        int bottomY = world.getMinY();
        int surfaceHeight;
        int seafloorHeight = bottomY;
        int transparentHeight = bottomY;
        int foliageHeight = bottomY;
        BlockState surfaceBlockState;
        BlockState transparentBlockState = BlockRepository.air.defaultBlockState();
        BlockState foliageBlockState = BlockRepository.air.defaultBlockState();
        BlockState seafloorBlockState = BlockRepository.air.defaultBlockState();
        pos = pos.withXYZ(startX + imageX, 64, startZ + imageY);
        Biome biome;
        if (!chunk.isEmpty()) {
            biome = world.getBiome(pos).value();
        } else {
            biome = null;
        }

        mapData.setBiome(imageX, imageY, biome);
        if (biome != null) {
            boolean solid = false;
            if (underground) {
                surfaceHeight = this.getNetherHeight(chunk, startX + imageX, startZ + imageY);
                surfaceBlockState = chunk.getBlockState(pos.withXYZ(startX + imageX, surfaceHeight - 1, startZ + imageY));
                if (surfaceHeight != Short.MIN_VALUE) {
                    foliageHeight = surfaceHeight + 1;
                    pos.setXYZ(startX + imageX, foliageHeight - 1, startZ + imageY);
                    foliageBlockState = chunk.getBlockState(pos);
                    Block material = foliageBlockState.getBlock();
                    if (material == Blocks.SNOW || material instanceof AirBlock || material == Blocks.LAVA || material == Blocks.WATER) {
                        foliageHeight = 0;
                    }
                }
            } else {
                transparentHeight = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX() & 15, pos.getZ() & 15) + 1;
                transparentBlockState = chunk.getBlockState(pos.withXYZ(startX + imageX, transparentHeight - 1, startZ + imageY));
                FluidState fluidState = transparentBlockState.getFluidState();
                if (fluidState != Fluids.EMPTY.defaultFluidState()) {
                    transparentBlockState = fluidState.createLegacyBlock();
                }

                surfaceHeight = transparentHeight;
                surfaceBlockState = transparentBlockState;
                VoxelShape voxelShape;
                boolean hasOpacity = transparentBlockState.getLightDampening() > 0;
                if (!hasOpacity && transparentBlockState.canOcclude() && transparentBlockState.useShapeForLightOcclusion()) {
                    voxelShape = transparentBlockState.getFaceOcclusionShape(Direction.DOWN);
                    hasOpacity = Shapes.faceShapeOccludes(voxelShape, Shapes.empty());
                    voxelShape = transparentBlockState.getFaceOcclusionShape(Direction.UP);
                    hasOpacity = hasOpacity || Shapes.faceShapeOccludes(Shapes.empty(), voxelShape);
                }

                while (!hasOpacity && surfaceHeight > bottomY) {
                    foliageBlockState = surfaceBlockState;
                    --surfaceHeight;
                    surfaceBlockState = chunk.getBlockState(pos.withXYZ(startX + imageX, surfaceHeight - 1, startZ + imageY));
                    fluidState = surfaceBlockState.getFluidState();
                    if (fluidState != Fluids.EMPTY.defaultFluidState()) {
                        surfaceBlockState = fluidState.createLegacyBlock();
                    }

                    hasOpacity = surfaceBlockState.getLightDampening() > 0;
                    if (!hasOpacity && surfaceBlockState.canOcclude() && surfaceBlockState.useShapeForLightOcclusion()) {
                        voxelShape = surfaceBlockState.getFaceOcclusionShape(Direction.DOWN);
                        hasOpacity = Shapes.faceShapeOccludes(voxelShape, Shapes.empty());
                        voxelShape = surfaceBlockState.getFaceOcclusionShape(Direction.UP);
                        hasOpacity = hasOpacity || Shapes.faceShapeOccludes(Shapes.empty(), voxelShape);
                    }
                }

                if (surfaceHeight == transparentHeight) {
                    transparentHeight = bottomY;
                    transparentBlockState = BlockRepository.air.defaultBlockState();
                    foliageBlockState = chunk.getBlockState(pos.withXYZ(startX + imageX, surfaceHeight, startZ + imageY));
                }

                if (foliageBlockState.getBlock() == Blocks.SNOW) {
                    surfaceBlockState = foliageBlockState;
                    foliageBlockState = BlockRepository.air.defaultBlockState();
                }

                if (foliageBlockState == transparentBlockState) {
                    foliageBlockState = BlockRepository.air.defaultBlockState();
                }

                if (foliageBlockState != null && !(foliageBlockState.getBlock() instanceof AirBlock)) {
                    foliageHeight = surfaceHeight + 1;
                } else {
                    foliageBlockState = BlockRepository.air.defaultBlockState();
                }

                Block material = surfaceBlockState.getBlock();
                if (material == Blocks.WATER || material == Blocks.ICE) {
                    seafloorHeight = surfaceHeight;

                    for (seafloorBlockState = chunk.getBlockState(pos.withXYZ(startX + imageX, surfaceHeight - 1, startZ + imageY)); seafloorBlockState.getLightDampening() < 5 && !(seafloorBlockState.getBlock() instanceof LeavesBlock) && seafloorHeight > bottomY + 1; seafloorBlockState = chunk.getBlockState(pos.withXYZ(startX + imageX, seafloorHeight - 1, startZ + imageY))) {
                        material = seafloorBlockState.getBlock();
                        if (transparentHeight == bottomY && material != Blocks.ICE && material != Blocks.WATER && seafloorBlockState.blocksMotion()) {
                            transparentHeight = seafloorHeight;
                            transparentBlockState = seafloorBlockState;
                        }

                        if (foliageHeight == bottomY && seafloorHeight != transparentHeight && transparentBlockState != seafloorBlockState && material != Blocks.ICE && material != Blocks.WATER && !(material instanceof AirBlock) && material != Blocks.BUBBLE_COLUMN) {
                            foliageHeight = seafloorHeight;
                            foliageBlockState = seafloorBlockState;
                        }

                        --seafloorHeight;
                    }

                    if (seafloorBlockState.getBlock() == Blocks.WATER) {
                        seafloorBlockState = BlockRepository.air.defaultBlockState();
                    }
                }
            }

            mapData.setHeight(imageX, imageY, surfaceHeight);
            mapData.setBlockstate(imageX, imageY, surfaceBlockState);
            mapData.setTransparentHeight(imageX, imageY, transparentHeight);
            mapData.setTransparentBlockstate(imageX, imageY, transparentBlockState);
            mapData.setFoliageHeight(imageX, imageY, foliageHeight);
            mapData.setFoliageBlockstate(imageX, imageY, foliageBlockState);
            mapData.setOceanFloorHeight(imageX, imageY, seafloorHeight);
            mapData.setOceanFloorBlockstate(imageX, imageY, seafloorBlockState);
            if (surfaceHeight < bottomY) {
                surfaceHeight = 80;
                solid = true;
            }

            if (surfaceBlockState.getBlock() == Blocks.LAVA) {
                solid = false;
            }

            int light = solid ? 0 : 255;
            if (!solid) {
                light = this.getLight(surfaceBlockState, world, pos, startX + imageX, startZ + imageY, surfaceHeight, solid);
            }

            mapData.setLight(imageX, imageY, light);
            int seafloorLight = 0;
            if (seafloorBlockState != null && seafloorBlockState != BlockRepository.air.defaultBlockState()) {
                seafloorLight = this.getLight(seafloorBlockState, world, pos, startX + imageX, startZ + imageY, seafloorHeight, solid);
            }

            mapData.setOceanFloorLight(imageX, imageY, seafloorLight);
            int transparentLight = 0;
            if (transparentBlockState != null && transparentBlockState != BlockRepository.air.defaultBlockState()) {
                transparentLight = this.getLight(transparentBlockState, world, pos, startX + imageX, startZ + imageY, transparentHeight, solid);
            }

            mapData.setTransparentLight(imageX, imageY, transparentLight);
            int foliageLight = 0;
            if (foliageBlockState != null && foliageBlockState != BlockRepository.air.defaultBlockState()) {
                foliageLight = this.getLight(foliageBlockState, world, pos, startX + imageX, startZ + imageY, foliageHeight, solid);
            }

            mapData.setFoliageLight(imageX, imageY, foliageLight);
        }
    }

    private int getNetherHeight(LevelChunk chunk, int x, int z) {
        int bottomY = chunk.getMinY();
        int y = 80;
        this.blockPos.setXYZ(x, y, z);
        BlockState blockState = chunk.getBlockState(this.blockPos);
        if (blockState.getLightDampening() == 0 && blockState.getBlock() != Blocks.LAVA) {
            while (y > bottomY) {
                --y;
                this.blockPos.setXYZ(x, y, z);
                blockState = chunk.getBlockState(this.blockPos);
                if (blockState.getLightDampening() > 0 || blockState.getBlock() == Blocks.LAVA) {
                    return y + 1;
                }
            }

            return y;
        } else {
            while (y <= 90) {
                ++y;
                this.blockPos.setXYZ(x, y, z);
                blockState = chunk.getBlockState(this.blockPos);
                if (blockState.getLightDampening() == 0 && blockState.getBlock() != Blocks.LAVA) {
                    return y;
                }
            }

            return Short.MIN_VALUE;
        }
    }

    private int getLight(BlockState blockState, Level world, MutableBlockPos blockPos, int x, int z, int height, boolean solid) {
        int lightCombined = 255;
        if (solid) {
            lightCombined = 0;
        } else if (blockState != null && !(blockState.getBlock() instanceof AirBlock)) {
            blockPos.setXYZ(x, Math.max(Math.min(height, world.getMaxY()), world.getMinY()), z);
            int blockLight = world.getBrightness(LightLayer.BLOCK, blockPos) & 15;
            int skyLight = world.getBrightness(LightLayer.SKY, blockPos);
            if (blockState.getBlock() == Blocks.LAVA || blockState.getBlock() == Blocks.MAGMA_BLOCK) {
                blockLight = 14;
            }

            lightCombined = blockLight + skyLight * 16;
        }

        return lightCombined;
    }

    public int getPixelColor(AbstractMapData mapData, ClientLevel world, MutableBlockPos blockPos, MutableBlockPos loopBlockPos, boolean underground, int multi, int startX, int startZ, int imageX, int imageY) {
        ColorManager.PersistentMapRenderContext renderContext = this.colorManager.createPersistentMapRenderContext(mapData, world, startX, startZ);
        CompressibleMapData.RenderView renderView = mapData instanceof CompressibleMapData.RenderView view ? view : null;
        return this.getPixelColor(mapData, renderView, renderContext, world, blockPos, loopBlockPos, underground, multi, startX, startZ, imageX, imageY, null);
    }

    int getPixelColor(CompressibleMapData.RenderView mapData, ColorManager.PersistentMapRenderContext renderContext, ClientLevel world, MutableBlockPos blockPos, MutableBlockPos loopBlockPos, boolean underground, int multi, int startX, int startZ, int imageX, int imageY) {
        return this.getPixelColor(mapData, mapData, renderContext, world, blockPos, loopBlockPos, underground, multi, startX, startZ, imageX, imageY, null);
    }

    int getPixelColor(
            CompressibleMapData.RenderView mapData,
            ColorManager.PersistentMapRenderContext renderContext,
            ClientLevel world,
            MutableBlockPos blockPos,
            MutableBlockPos loopBlockPos,
            boolean underground,
            int multi,
            int startX,
            int startZ,
            int imageX,
            int imageY,
            PixelRenderOutput output) {
        return this.getPixelColor(mapData, mapData, renderContext, world, blockPos, loopBlockPos, underground, multi, startX, startZ, imageX, imageY, output);
    }

    private int getPixelColor(AbstractMapData mapData, CompressibleMapData.RenderView renderView, ColorManager.PersistentMapRenderContext renderContext, ClientLevel world, MutableBlockPos blockPos, MutableBlockPos loopBlockPos, boolean underground, int multi, int startX, int startZ, int imageX, int imageY, PixelRenderOutput output) {
        if (output != null) {
            output.reset();
        }
        int bottomY = world.getMinY();
        int mcX = startX + imageX;
        int mcZ = startZ + imageY;
        BlockState surfaceBlockState;
        BlockState transparentBlockState;
        BlockState foliageBlockState;
        BlockState seafloorBlockState;
        int surfaceHeight;
        int seafloorHeight = bottomY;
        int transparentHeight = bottomY;
        int foliageHeight = bottomY;
        int surfaceColor;
        int seafloorColor = 0;
        int transparentColor = 0;
        int foliageColor = 0;
        int unlitSurfaceColor = 0;
        int unlitSeafloorColor = 0;
        int unlitTransparentColor = 0;
        int unlitFoliageColor = 0;
        int surfaceLight = 255;
        int seafloorLight = 0;
        int transparentLight = 0;
        int foliageLight = 0;
        blockPos = blockPos.withXYZ(mcX, 0, mcZ);
        int color24;
        Biome biome = mapData.getBiome(imageX, imageY);
        surfaceBlockState = mapData.getBlockstate(imageX, imageY);
        if (surfaceBlockState != null && (surfaceBlockState.getBlock() != BlockRepository.air || mapData.getLight(imageX, imageY) != 0 || mapData.getHeight(imageX, imageY) != Short.MIN_VALUE) && biome != null) {
            if (mapOptions.biomeOverlay == 1) {
                color24 = ARGB.toABGR(BiomeRepository.getBiomeColor(biome) | 0xFF000000);
            } else {
                boolean solid = false;
                int blockStateID;
                surfaceHeight = mapData.getHeight(imageX, imageY);
                blockStateID = renderView == null ? BlockRepository.getStateId(surfaceBlockState) : renderView.getBlockstateId(imageX, imageY);
                if (surfaceHeight < bottomY || surfaceHeight == world.getMaxY()) {
                    surfaceHeight = 80;
                    solid = true;
                }

                blockPos.setXYZ(mcX, surfaceHeight - 1, mcZ);
                if (surfaceBlockState.getBlock() == Blocks.LAVA) {
                    solid = false;
                }

                if (mapOptions.biomes) {
                    surfaceColor = this.colorManager.getBlockColor(blockPos, blockStateID, biome);
                    int tint;
                    tint = this.colorManager.getBiomeTint(mapData, world, surfaceBlockState, blockStateID, blockPos, loopBlockPos, startX, startZ, renderContext);
                    if (tint != -1) {
                        surfaceColor = ColorUtils.colorMultiplier(surfaceColor, tint);
                    }
                } else {
                    surfaceColor = this.colorManager.getBlockColorWithDefaultTint(blockPos, blockStateID);
                }

                surfaceColor = this.applyHeight(mapData, surfaceColor, underground, multi, imageX, imageY, surfaceHeight, solid, 1);
                surfaceLight = mapData.getLight(imageX, imageY);
                unlitSurfaceColor = surfaceColor;
                if (solid) {
                    surfaceColor = 0;
                    unlitSurfaceColor = 0;
                } else if (mapOptions.dynamicLighting) {
                    int lightValue = this.getLight(surfaceLight);
                    surfaceColor = ColorUtils.colorMultiplier(surfaceColor, lightValue);
                }

                if (mapOptions.waterTransparency && !solid) {
                    seafloorHeight = mapData.getOceanFloorHeight(imageX, imageY);
                    if (seafloorHeight > bottomY) {
                        blockPos.setXYZ(mcX, seafloorHeight - 1, mcZ);
                        seafloorBlockState = mapData.getOceanFloorBlockstate(imageX, imageY);
                        if (seafloorBlockState != null && seafloorBlockState != BlockRepository.air.defaultBlockState()) {
                            blockStateID = renderView == null ? BlockRepository.getStateId(seafloorBlockState) : renderView.getOceanFloorBlockstateId(imageX, imageY);
                            if (mapOptions.biomes) {
                                seafloorColor = this.colorManager.getBlockColor(blockPos, blockStateID, biome);
                                int tint;
                                tint = this.colorManager.getBiomeTint(mapData, world, seafloorBlockState, blockStateID, blockPos, loopBlockPos, startX, startZ, renderContext);
                                if (tint != -1) {
                                    seafloorColor = ColorUtils.colorMultiplier(seafloorColor, tint);
                                }
                            } else {
                                seafloorColor = this.colorManager.getBlockColorWithDefaultTint(blockPos, blockStateID);
                            }

                            seafloorColor = this.applyHeight(mapData, seafloorColor, underground, multi, imageX, imageY, seafloorHeight, solid, 0);
                            unlitSeafloorColor = seafloorColor;
                            seafloorLight = mapData.getOceanFloorLight(imageX, imageY);
                            if (mapOptions.dynamicLighting) {
                                int lightValue = this.getLight(seafloorLight);
                                seafloorColor = ColorUtils.colorMultiplier(seafloorColor, lightValue);
                            }
                        }
                    }
                }

                if (mapOptions.blockTransparency && !solid) {
                    transparentHeight = mapData.getTransparentHeight(imageX, imageY);
                    if (transparentHeight > bottomY) {
                        blockPos.setXYZ(mcX, transparentHeight - 1, mcZ);
                        transparentBlockState = mapData.getTransparentBlockstate(imageX, imageY);
                        if (transparentBlockState != null && transparentBlockState != BlockRepository.air.defaultBlockState()) {
                            blockStateID = renderView == null ? BlockRepository.getStateId(transparentBlockState) : renderView.getTransparentBlockstateId(imageX, imageY);
                            if (mapOptions.biomes) {
                                transparentColor = this.colorManager.getBlockColor(blockPos, blockStateID, biome);
                                int tint;
                                tint = this.colorManager.getBiomeTint(mapData, world, transparentBlockState, blockStateID, blockPos, loopBlockPos, startX, startZ, renderContext);
                                if (tint != -1) {
                                    transparentColor = ColorUtils.colorMultiplier(transparentColor, tint);
                                }
                            } else {
                                transparentColor = this.colorManager.getBlockColorWithDefaultTint(blockPos, blockStateID);
                            }

                            transparentColor = this.applyHeight(mapData, transparentColor, underground, multi, imageX, imageY, transparentHeight, solid, 3);
                            unlitTransparentColor = transparentColor;
                            transparentLight = mapData.getTransparentLight(imageX, imageY);
                            if (mapOptions.dynamicLighting) {
                                int lightValue = this.getLight(transparentLight);
                                transparentColor = ColorUtils.colorMultiplier(transparentColor, lightValue);
                            }
                        }
                    }

                    foliageHeight = mapData.getFoliageHeight(imageX, imageY);
                    if (foliageHeight > bottomY) {
                        blockPos.setXYZ(mcX, foliageHeight - 1, mcZ);
                        foliageBlockState = mapData.getFoliageBlockstate(imageX, imageY);
                        if (foliageBlockState != null && foliageBlockState != BlockRepository.air.defaultBlockState()) {
                            blockStateID = renderView == null ? BlockRepository.getStateId(foliageBlockState) : renderView.getFoliageBlockstateId(imageX, imageY);
                            if (mapOptions.biomes) {
                                foliageColor = this.colorManager.getBlockColor(blockPos, blockStateID, biome);
                                int tint;
                                tint = this.colorManager.getBiomeTint(mapData, world, foliageBlockState, blockStateID, blockPos, loopBlockPos, startX, startZ, renderContext);
                                if (tint != -1) {
                                    foliageColor = ColorUtils.colorMultiplier(foliageColor, tint);
                                }
                            } else {
                                foliageColor = this.colorManager.getBlockColorWithDefaultTint(blockPos, blockStateID);
                            }

                            foliageColor = this.applyHeight(mapData, foliageColor, underground, multi, imageX, imageY, foliageHeight, solid, 2);
                            unlitFoliageColor = foliageColor;
                            foliageLight = mapData.getFoliageLight(imageX, imageY);
                            if (mapOptions.dynamicLighting) {
                                int lightValue = this.getLight(foliageLight);
                                foliageColor = ColorUtils.colorMultiplier(foliageColor, lightValue);
                            }
                        }
                    }
                }

                color24 = composeLayers(surfaceColor, seafloorColor, transparentColor, foliageColor, surfaceHeight, seafloorHeight, transparentHeight, foliageHeight, bottomY, mapOptions.waterTransparency);
                int unlitColor = composeLayers(unlitSurfaceColor, unlitSeafloorColor, unlitTransparentColor, unlitFoliageColor, surfaceHeight, seafloorHeight, transparentHeight, foliageHeight, bottomY, mapOptions.waterTransparency);

                if (mapOptions.biomeOverlay == 2) {
                    int bc = 0;
                    if (biome != null) {
                        bc = ARGB.toABGR(BiomeRepository.getBiomeColor(biome));
                    }

                    bc = 0x7F000000 | bc;
                    color24 = ColorUtils.colorAdder(bc, color24);
                    unlitColor = ColorUtils.colorAdder(bc, unlitColor);
                }

                if (output != null) {
                    output.unlitColor = MapUtils.doSlimeAndGrid(ARGB.toABGR(unlitColor), world, mcX, mcZ);
                    output.light = composeApproximateLight(
                            unlitSurfaceColor,
                            surfaceLight,
                            unlitSeafloorColor,
                            seafloorLight,
                            unlitTransparentColor,
                            transparentLight,
                            unlitFoliageColor,
                            foliageLight,
                            surfaceHeight,
                            seafloorHeight,
                            transparentHeight,
                            foliageHeight,
                            bottomY,
                            mapOptions.waterTransparency,
                            output.lightAccumulator);
                    boolean waterSurface = mapOptions.waterTransparency
                            && seafloorHeight > bottomY
                            && surfaceBlockState.getFluidState().createLegacyBlock().getBlock() == Blocks.WATER;
                    output.overviewWater = waterSurface;
                    if (waterSurface
                            && mapOptions.biomeOverlay == 0) {
                        composeSplitLighting(
                                unlitSurfaceColor,
                                surfaceColor,
                                surfaceLight,
                                unlitSeafloorColor,
                                seafloorColor,
                                seafloorLight,
                                unlitTransparentColor,
                                transparentColor,
                                transparentLight,
                                unlitFoliageColor,
                                foliageColor,
                                foliageLight,
                                surfaceHeight,
                                seafloorHeight,
                                transparentHeight,
                                foliageHeight,
                                bottomY,
                                mapOptions.waterTransparency,
                                waterSurface,
                                output.splitLightingAccumulator);
                        int overlayColor = MapUtils.doSlimeAndGrid(0, world, mcX, mcZ);
                        if (overlayColor != 0) {
                            int internalOverlayColor = ARGB.toABGR(overlayColor);
                            output.splitLightingAccumulator.addTop(
                                    internalOverlayColor, internalOverlayColor, 0xFF, 0);
                        }
                        output.splitLightingPrepared = true;
                    }
                }

            }
            int displayedColor = MapUtils.doSlimeAndGrid(ARGB.toABGR(color24), world, mcX, mcZ);
            if (output != null && mapOptions.biomeOverlay == 1) {
                output.unlitColor = displayedColor;
                output.light = 255;
            }
            if (output != null) {
                output.ensureSingleComponent(displayedColor);
            }
            return displayedColor;
        } else {
            if (output != null) {
                output.unlitColor = 0;
                output.light = 255;
                output.ensureSingleComponent(0);
            }
            return 0;
        }
    }

    private static int composeLayers(
            int surfaceColor,
            int seafloorColor,
            int transparentColor,
            int foliageColor,
            int surfaceHeight,
            int seafloorHeight,
            int transparentHeight,
            int foliageHeight,
            int bottomY,
            boolean waterTransparency) {
        int color;
        if (waterTransparency && seafloorHeight > bottomY) {
            color = seafloorColor;
            if (foliageColor != 0 && foliageHeight <= surfaceHeight) {
                color = ColorUtils.colorAdder(foliageColor, seafloorColor);
            }
            if (transparentColor != 0 && transparentHeight <= surfaceHeight) {
                color = ColorUtils.colorAdder(transparentColor, color);
            }
            color = ColorUtils.colorAdder(surfaceColor, color);
        } else {
            color = surfaceColor;
        }
        if (foliageColor != 0 && foliageHeight > surfaceHeight) {
            color = ColorUtils.colorAdder(foliageColor, color);
        }
        if (transparentColor != 0 && transparentHeight > surfaceHeight) {
            color = ColorUtils.colorAdder(transparentColor, color);
        }
        return color;
    }

    static int composeApproximateLight(
            int surfaceColor,
            int surfaceLight,
            int seafloorColor,
            int seafloorLight,
            int transparentColor,
            int transparentLight,
            int foliageColor,
            int foliageLight,
            int surfaceHeight,
            int seafloorHeight,
            int transparentHeight,
            int foliageHeight,
            int bottomY,
            boolean waterTransparency,
            LightAccumulator light) {
        light.clear();
        if (waterTransparency && seafloorHeight > bottomY) {
            light.addTop(seafloorColor, seafloorLight);
            if (foliageColor != 0 && foliageHeight <= surfaceHeight) {
                light.addTop(foliageColor, foliageLight);
            }
            if (transparentColor != 0 && transparentHeight <= surfaceHeight) {
                light.addTop(transparentColor, transparentLight);
            }
            light.addTop(surfaceColor, surfaceLight);
        } else {
            light.addTop(surfaceColor, surfaceLight);
        }
        if (foliageColor != 0 && foliageHeight > surfaceHeight) {
            light.addTop(foliageColor, foliageLight);
        }
        if (transparentColor != 0 && transparentHeight > surfaceHeight) {
            light.addTop(transparentColor, transparentLight);
        }
        return light.combinedLight();
    }

    private static void composeSplitLighting(
            int unlitSurfaceColor,
            int surfaceColor,
            int surfaceLight,
            int unlitSeafloorColor,
            int seafloorColor,
            int seafloorLight,
            int unlitTransparentColor,
            int transparentColor,
            int transparentLight,
            int unlitFoliageColor,
            int foliageColor,
            int foliageLight,
            int surfaceHeight,
            int seafloorHeight,
            int transparentHeight,
            int foliageHeight,
            int bottomY,
            boolean waterTransparency,
            boolean waterSurface,
            SplitLightingAccumulator split) {
        // Component 0 contains the water surface and layers above it; component 1
        // contains the seafloor and all layers below the water surface.
        if (waterTransparency && seafloorHeight > bottomY) {
            split.addTop(unlitSeafloorColor, seafloorColor, seafloorLight, waterSurface ? 1 : 0);
            if (unlitFoliageColor != 0 && foliageHeight <= surfaceHeight) {
                split.addTop(unlitFoliageColor, foliageColor, foliageLight, waterSurface ? 1 : 0);
            }
            if (unlitTransparentColor != 0 && transparentHeight <= surfaceHeight) {
                split.addTop(unlitTransparentColor, transparentColor, transparentLight, waterSurface ? 1 : 0);
            }
            split.addTop(unlitSurfaceColor, surfaceColor, surfaceLight, 0);
        } else {
            split.addTop(unlitSurfaceColor, surfaceColor, surfaceLight, 0);
        }
        if (unlitFoliageColor != 0 && foliageHeight > surfaceHeight) {
            split.addTop(unlitFoliageColor, foliageColor, foliageLight, 0);
        }
        if (unlitTransparentColor != 0 && transparentHeight > surfaceHeight) {
            split.addTop(unlitTransparentColor, transparentColor, transparentLight, 0);
        }
    }

    static final class PixelRenderOutput {
        int unlitColor;
        int light;
        final LightAccumulator lightAccumulator = new LightAccumulator();
        final SplitLightingAccumulator splitLightingAccumulator = new SplitLightingAccumulator();
        boolean splitLightingPrepared;
        boolean overviewWater;

        private void reset() {
            this.splitLightingAccumulator.clear();
            this.splitLightingPrepared = false;
            this.overviewWater = false;
        }

        private void ensureSingleComponent(int displayedColor) {
            if (!this.splitLightingPrepared) {
                this.splitLightingAccumulator.setSingle(this.unlitColor, displayedColor, this.light);
                this.splitLightingPrepared = true;
            }
        }
    }

    static final class LightAccumulator {
        private double alpha;
        private double blockLight;
        private double skyLight;

        private void clear() {
            this.alpha = 0.0;
            this.blockLight = 0.0;
            this.skyLight = 0.0;
        }

        private void addTop(int color, int light) {
            double topAlpha = (color >> 24 & 0xFF) / 255.0;
            if (topAlpha == 0.0) {
                return;
            }
            double visibleBottom = 1.0 - topAlpha;
            this.blockLight = (light & 0xF) * topAlpha + this.blockLight * visibleBottom;
            this.skyLight = (light >> 4 & 0xF) * topAlpha + this.skyLight * visibleBottom;
            this.alpha = topAlpha + this.alpha * visibleBottom;
        }

        private int combinedLight() {
            if (this.alpha == 0.0) {
                return 255;
            }
            int block = Math.clamp((int) Math.round(this.blockLight / this.alpha), 0, 15);
            int sky = Math.clamp((int) Math.round(this.skyLight / this.alpha), 0, 15);
            return block | sky << 4;
        }
    }

    static final class SplitLightingAccumulator {
        private static final int COMPONENTS = 2;

        private final double[] alpha = new double[COMPONENTS];
        private final double[] unlitRed = new double[COMPONENTS];
        private final double[] unlitGreen = new double[COMPONENTS];
        private final double[] unlitBlue = new double[COMPONENTS];
        private final double[] litRed = new double[COMPONENTS];
        private final double[] litGreen = new double[COMPONENTS];
        private final double[] litBlue = new double[COMPONENTS];
        private final double[] blockLight = new double[COMPONENTS];
        private final double[] skyLight = new double[COMPONENTS];

        void clear() {
            this.alpha[0] = this.alpha[1] = 0.0;
            this.unlitRed[0] = this.unlitRed[1] = 0.0;
            this.unlitGreen[0] = this.unlitGreen[1] = 0.0;
            this.unlitBlue[0] = this.unlitBlue[1] = 0.0;
            this.litRed[0] = this.litRed[1] = 0.0;
            this.litGreen[0] = this.litGreen[1] = 0.0;
            this.litBlue[0] = this.litBlue[1] = 0.0;
            this.blockLight[0] = this.blockLight[1] = 0.0;
            this.skyLight[0] = this.skyLight[1] = 0.0;
        }

        void addTop(int unlitColor, int litColor, int light, int component) {
            int outputUnlitColor = ARGB.toABGR(unlitColor);
            int outputLitColor = ARGB.toABGR(litColor);
            double topAlpha = (outputUnlitColor >> 24 & 0xFF) / 255.0;
            if (topAlpha == 0.0) {
                return;
            }

            double visibleBottom = 1.0 - topAlpha;
            for (int index = 0; index < COMPONENTS; ++index) {
                this.alpha[index] *= visibleBottom;
                this.unlitRed[index] *= visibleBottom;
                this.unlitGreen[index] *= visibleBottom;
                this.unlitBlue[index] *= visibleBottom;
                this.litRed[index] *= visibleBottom;
                this.litGreen[index] *= visibleBottom;
                this.litBlue[index] *= visibleBottom;
                this.blockLight[index] *= visibleBottom;
                this.skyLight[index] *= visibleBottom;
            }

            this.alpha[component] += topAlpha;
            this.unlitRed[component] += (outputUnlitColor >> 16 & 0xFF) * topAlpha;
            this.unlitGreen[component] += (outputUnlitColor >> 8 & 0xFF) * topAlpha;
            this.unlitBlue[component] += (outputUnlitColor & 0xFF) * topAlpha;
            this.litRed[component] += (outputLitColor >> 16 & 0xFF) * topAlpha;
            this.litGreen[component] += (outputLitColor >> 8 & 0xFF) * topAlpha;
            this.litBlue[component] += (outputLitColor & 0xFF) * topAlpha;
            this.blockLight[component] += (light & 0xF) * topAlpha;
            this.skyLight[component] += (light >> 4 & 0xF) * topAlpha;
        }

        void setSingle(int unlitColor, int litColor, int light) {
            int unlitPremultiplied = ColorUtils.premultiplyWithAlpha(unlitColor);
            int litPremultiplied = ColorUtils.premultiplyWithAlpha(litColor);
            double sourceAlpha = (unlitColor >> 24 & 0xFF) / 255.0;
            if ((unlitPremultiplied & 0xFFFFFF) == 0 && (litPremultiplied & 0xFFFFFF) == 0) {
                return;
            }
            this.alpha[0] = sourceAlpha == 0.0 ? 1.0 : sourceAlpha;
            this.unlitRed[0] = unlitPremultiplied >> 16 & 0xFF;
            this.unlitGreen[0] = unlitPremultiplied >> 8 & 0xFF;
            this.unlitBlue[0] = unlitPremultiplied & 0xFF;
            this.litRed[0] = litPremultiplied >> 16 & 0xFF;
            this.litGreen[0] = litPremultiplied >> 8 & 0xFF;
            this.litBlue[0] = litPremultiplied & 0xFF;
            this.blockLight[0] = (light & 0xF) * this.alpha[0];
            this.skyLight[0] = (light >> 4 & 0xF) * this.alpha[0];
        }

        int unlitRed(int component) {
            return Math.clamp((int) Math.round(this.unlitRed[component]), 0, 255);
        }

        int unlitGreen(int component) {
            return Math.clamp((int) Math.round(this.unlitGreen[component]), 0, 255);
        }

        int unlitBlue(int component) {
            return Math.clamp((int) Math.round(this.unlitBlue[component]), 0, 255);
        }

        int litRed(int component) {
            return Math.clamp((int) Math.round(this.litRed[component]), 0, 255);
        }

        int litGreen(int component) {
            return Math.clamp((int) Math.round(this.litGreen[component]), 0, 255);
        }

        int litBlue(int component) {
            return Math.clamp((int) Math.round(this.litBlue[component]), 0, 255);
        }

        boolean hasComponent(int component) {
            return this.alpha[component] > 0.0;
        }

        int light(int component) {
            if (!this.hasComponent(component)) {
                return 255;
            }
            int block = Math.clamp((int) Math.round(this.blockLight[component] / this.alpha[component]), 0, 15);
            int sky = Math.clamp((int) Math.round(this.skyLight[component] / this.alpha[component]), 0, 15);
            return block | sky << 4;
        }
    }

    private int applyHeight(AbstractMapData mapData, int color24, boolean underground, int multi, int imageX, int imageY, int height, boolean solid, int layer) {
        if (color24 != this.colorManager.getAirColor() && color24 != 0) {
            int heightComp = Short.MIN_VALUE;
            if ((mapOptions.heightmap || mapOptions.slopemap) && !solid) {
                int diff;
                double sc = 0.0;
                boolean invert = false;
                if (!mapOptions.slopemap) {
                    sc = heightmapShade(height);
                } else {
                    if (imageX > 0 && imageY < 32 * multi - 1) {
                        if (layer == 0) {
                            heightComp = mapData.getOceanFloorHeight(imageX - 1, imageY + 1);
                        }

                        if (layer == 1) {
                            heightComp = mapData.getHeight(imageX - 1, imageY + 1);
                        }

                        if (layer == 2) {
                            heightComp = height;
                        }

                        if (layer == 3) {
                            heightComp = mapData.getTransparentHeight(imageX - 1, imageY + 1);
                            if (heightComp == Short.MIN_VALUE) {
                                BlockState transparentBlockState = mapData.getTransparentBlockstate(imageX, imageY);
                                if (transparentBlockState != null && transparentBlockState != BlockRepository.air.defaultBlockState()) {
                                    Block block = transparentBlockState.getBlock();
                                    if (block == Blocks.GLASS || block instanceof StainedGlassBlock) {
                                        heightComp = mapData.getHeight(imageX - 1, imageY + 1);
                                    }
                                }
                            }
                        }
                    } else if (imageX < 32 * multi - 1 && imageY > 0) {
                        if (layer == 0) {
                            heightComp = mapData.getOceanFloorHeight(imageX + 1, imageY - 1);
                        }

                        if (layer == 1) {
                            heightComp = mapData.getHeight(imageX + 1, imageY - 1);
                        }

                        if (layer == 2) {
                            heightComp = height;
                        }

                        if (layer == 3) {
                            heightComp = mapData.getTransparentHeight(imageX + 1, imageY - 1);
                            if (heightComp == Short.MIN_VALUE) {
                                BlockState transparentBlockState = mapData.getTransparentBlockstate(imageX, imageY);
                                if (transparentBlockState != null && transparentBlockState != BlockRepository.air.defaultBlockState()) {
                                    Block block = transparentBlockState.getBlock();
                                    if (block == Blocks.GLASS || block instanceof StainedGlassBlock) {
                                        heightComp = mapData.getHeight(imageX + 1, imageY - 1);
                                    }
                                }
                            }
                        }

                        invert = true;
                    } else {
                        heightComp = height;
                    }

                    if (heightComp == Short.MIN_VALUE) {
                        heightComp = height;
                    }

                    if (!invert) {
                        diff = heightComp - height;
                    } else {
                        diff = height - heightComp;
                    }

                    if (diff != 0) {
                        sc = diff > 0 ? 1.0 : -1.0;
                        sc /= 8.0;
                    }

                    if (mapOptions.heightmap) {
                        diff = height - 80;
                        double heightsc = heightmapWithSlopeShade(height);
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
        }

        return color24;
    }

    static double heightmapShade(int height) {
        if (height >= HEIGHT_SHADE_MIN && height <= HEIGHT_SHADE_MAX) {
            return HEIGHTMAP_SHADE[height - HEIGHT_SHADE_MIN];
        }
        int diff = height - 80;
        double shade = Math.log10(Math.abs(diff) / 8.0 + 1.0) / 1.8;
        return diff < 0 ? -shade : shade;
    }

    static double heightmapWithSlopeShade(int height) {
        if (height >= HEIGHT_SHADE_MIN && height <= HEIGHT_SHADE_MAX) {
            return HEIGHTMAP_WITH_SLOPE_SHADE[height - HEIGHT_SHADE_MIN];
        }
        int diff = height - 80;
        return Math.log10(Math.abs(diff) / 8.0 + 1.0) / 3.0;
    }

    static boolean useOverview(float zoom) {
        return zoom <= OVERVIEW_ZOOM_THRESHOLD;
    }

    long getOverviewRenderSignature() {
        return this.getOverviewRenderSignature(OVERVIEW_RENDER_VERSION);
    }

    private long getOverviewRenderSignature(long renderVersion) {
        long signature = 0xcbf29ce484222325L;
        signature = appendSignature(signature, renderVersion);
        signature = appendSignature(signature, this.colorManager.getResourcePackSignature());
        signature = appendSignature(signature, this.mapOptions.dynamicLighting);
        signature = appendSignature(signature, this.mapOptions.heightmap);
        signature = appendSignature(signature, this.mapOptions.slopemap);
        signature = appendSignature(signature, this.mapOptions.waterTransparency);
        signature = appendSignature(signature, this.mapOptions.blockTransparency);
        signature = appendSignature(signature, this.mapOptions.biomes);
        signature = appendSignature(signature, this.mapOptions.biomeOverlay);
        signature = appendSignature(signature, this.mapOptions.chunkGrid);
        signature = appendSignature(signature, this.mapOptions.slimeChunks);
        signature = appendSignature(signature, VoxelConstants.usesConnectedTextures());
        return signature;
    }

    private static long appendSignature(long signature, long value) {
        for (int shift = 0; shift < Long.SIZE; shift += Byte.SIZE) {
            signature = (signature ^ (value >>> shift & 0xffL)) * 0x100000001b3L;
        }
        return signature;
    }

    private static long appendSignature(long signature, boolean value) {
        return appendSignature(signature, value ? 1L : 0L);
    }

    private static double[] createHeightShadeLookup(double divisor, boolean signed) {
        double[] lookup = new double[HEIGHT_SHADE_MAX - HEIGHT_SHADE_MIN + 1];
        for (int height = HEIGHT_SHADE_MIN; height <= HEIGHT_SHADE_MAX; ++height) {
            int diff = height - 80;
            double shade = Math.log10(Math.abs(diff) / 8.0 + 1.0) / divisor;
            lookup[height - HEIGHT_SHADE_MIN] = signed && diff < 0 ? -shade : shade;
        }
        return lookup;
    }

    private int getLight(int light) {
        return this.lightmapColors[light];
    }

    public CachedRegion[] getRegions(int left, int right, int top, int bottom) {
        return this.getRegions(left, right, top, bottom, Float.POSITIVE_INFINITY);
    }

    public CachedRegion[] getRegions(int left, int right, int top, int bottom, float zoom) {
        boolean fullDetail = !useOverview(zoom) || this.options.outputImages || this.mapOptions.biomeOverlay != 0;
        int expectedRegionCount = Math.max(0, right - left + 1) * Math.max(0, bottom - top + 1);
        if (left == this.lastLeft
                && right == this.lastRight
                && top == this.lastTop
                && bottom == this.lastBottom
                && this.lastRegionsArray.length == expectedRegionCount) {
            boolean refreshVisibleRegions = this.visibleRegionRefreshRequested.getAndSet(false);
            if (this.lastFullDetailRequested != fullDetail || refreshVisibleRegions) {
                // Notifications only mark work on a region. Revisit visible
                // regions when lighting changed so work recorded while the map
                // was closed is actually queued on the unchanged fast path.
                for (CachedRegion region : this.lastRegionsArray) {
                    region.refresh(false, fullDetail);
                }
            }
            this.lastFullDetailRequested = fullDetail;
            return this.lastRegionsArray;
        } else {
            this.visibleRegionRefreshRequested.set(false);
            long selectionStartedNanos = PersistentMapProfiler.startTimer();
            CachedRegion[] previouslyVisibleRegions = this.lastRegionsArray;
            CachedRegion[] visibleCachedRegionsArray = new CachedRegion[(right - left + 1) * (bottom - top + 1)];
            String worldName = VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentWorldName();
            String subWorldName = VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentSubworldDescriptor(false);
            List<RegionCoordinates> regionsToDisplay = new ArrayList<>();
            int createdRegions = 0;
            int reusedRegions = 0;
            int knownEmptyRegions = 0;

            for (int t = left; t <= right; ++t) {
                for (int s = top; s <= bottom; ++s) {
                    RegionCoordinates regionCoordinates = new RegionCoordinates(t, s);
                    regionsToDisplay.add(regionCoordinates);
                }
            }

            regionsToDisplay.sort(this.distanceSorter);

            for (RegionCoordinates regionCoordinates : regionsToDisplay) {
                int x = regionCoordinates.x;
                int z = regionCoordinates.z;
                String key = x + "," + z;
                CachedRegion cachedRegion;
                synchronized (this.cachedRegions) {
                    cachedRegion = this.cachedRegions.get(key);
                    if (cachedRegion == null) {
                        cachedRegion = new CachedRegion(this, key, this.world, worldName, subWorldName, x, z);
                        this.cachedRegions.put(key, cachedRegion);
                        synchronized (this.cachedRegionsPool) {
                            this.cachedRegionsPool.add(cachedRegion);
                        }
                        ++createdRegions;
                    } else if (cachedRegion == CachedRegion.EMPTY_REGION) {
                        ++knownEmptyRegions;
                    } else {
                        ++reusedRegions;
                    }
                }

                cachedRegion.refresh(true, fullDetail);
                visibleCachedRegionsArray[(z - top) * (right - left + 1) + (x - left)] = cachedRegion;
            }

            Set<CachedRegion> visibleRegions = Collections.newSetFromMap(new IdentityHashMap<>());
            Collections.addAll(visibleRegions, visibleCachedRegionsArray);
            for (CachedRegion previouslyVisibleRegion : previouslyVisibleRegions) {
                if (previouslyVisibleRegion != CachedRegion.EMPTY_REGION && !visibleRegions.contains(previouslyVisibleRegion)) {
                    previouslyVisibleRegion.cancelRefreshIfQueued();
                }
            }

            this.prunePool();
            if (visibleCachedRegionsArray.length > 0) {
                PersistentMapProfiler.recordRegionSelection(
                        selectionStartedNanos,
                        visibleCachedRegionsArray.length,
                        createdRegions,
                        reusedRegions,
                        knownEmptyRegions);
            }
            synchronized (this.lastRegionsLock) {
                this.lastLeft = left;
                this.lastRight = right;
                this.lastTop = top;
                this.lastBottom = bottom;
                this.lastFullDetailRequested = fullDetail;
                this.lastRegionsArray = visibleCachedRegionsArray;
                return visibleCachedRegionsArray;
            }
        }
    }

    record LightmapSnapshot(int[] colors, long revision) {}

    private void prunePool() {
        synchronized (this.cachedRegionsPool) {
            Iterator<CachedRegion> iterator = this.cachedRegionsPool.iterator();

            while (iterator.hasNext()) {
                CachedRegion region = iterator.next();
                if (region.isLoaded() && region.isEmpty()) {
                    this.cachedRegions.put(region.getKey(), CachedRegion.EMPTY_REGION);
                    region.cleanup();
                    iterator.remove();
                }
            }

            if (this.cachedRegionsPool.size() > this.options.cacheSize) {
                this.cachedRegionsPool.sort(this.ageThenDistanceSorter);
                List<CachedRegion> toRemove = this.cachedRegionsPool.subList(this.options.cacheSize, this.cachedRegionsPool.size());

                for (CachedRegion cachedRegion : toRemove) {
                    this.cachedRegions.remove(cachedRegion.getKey());
                    cachedRegion.cleanup();
                }

                toRemove.clear();
            }

            this.compress();
        }
    }

    public void compress() {
        synchronized (this.cachedRegionsPool) {
            for (CachedRegion cachedRegion : this.cachedRegionsPool) {
                if (System.currentTimeMillis() - cachedRegion.getMostRecentChange() > 5000L) {
                    cachedRegion.compress();
                }
            }

        }
    }

    @Override
    public void handleChangeInWorld(int chunkX, int chunkZ) {
        if (this.world != null) {
            LevelChunk chunk = this.world.getChunk(chunkX, chunkZ);
            if (chunk != null && !chunk.isEmpty()) {
                if (this.isChunkReady(this.world, chunk)) {
                    this.processChunk(chunk);
                }

            }
        }
    }

    @Override
    public void processChunk(LevelChunk chunk) {
        if (mapOptions.worldmapAllowed) {
            this.chunkUpdateQueue.add(new ChunkWithAge(chunk, VoxelConstants.getElapsedTicks()));
        }
    }

    private void doProcessChunk(LevelChunk chunk) {
        this.queuedChangedChunks = true;

        try {
            if (this.world == null) {
                return;
            }

            if (chunk == null || chunk.isEmpty()) {
                return;
            }

            int chunkX = chunk.getPos().x();
            int chunkZ = chunk.getPos().z();
            int regionX = (int) Math.floor(chunkX / 16.0);
            int regionZ = (int) Math.floor(chunkZ / 16.0);
            String key = regionX + "," + regionZ;
            CachedRegion cachedRegion;
            synchronized (this.cachedRegions) {
                cachedRegion = this.cachedRegions.get(key);
                if (cachedRegion == null || cachedRegion == CachedRegion.EMPTY_REGION) {
                    String worldName = VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentWorldName();
                    String subWorldName = VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentSubworldDescriptor(false);
                    cachedRegion = new CachedRegion(this, key, this.world, worldName, subWorldName, regionX, regionZ);
                    this.cachedRegions.put(key, cachedRegion);
                    synchronized (this.cachedRegionsPool) {
                        this.cachedRegionsPool.add(cachedRegion);
                    }

                    synchronized (this.lastRegionsLock) {
                        if (regionX >= this.lastLeft && regionX <= this.lastRight && regionZ >= this.lastTop && regionZ <= this.lastBottom) {
                            this.lastRegionsArray[(regionZ - this.lastTop) * (this.lastRight - this.lastLeft + 1) + (regionX - this.lastLeft)] = cachedRegion;
                        }
                    }
                }
            }

            if (VoxelConstants.getMinecraft().gui.screen() != null && VoxelConstants.getMinecraft().gui.screen() instanceof GuiPersistentMap) {
                cachedRegion.registerChangeAt(chunkX, chunkZ);
                cachedRegion.refresh(false, this.lastFullDetailRequested);
            } else {
                cachedRegion.handleChangedChunk(chunk);
            }
        } catch (Exception var19) {
            VoxelConstants.getLogger().error(var19.getMessage(), var19);
        }

    }

    private boolean isChunkReady(ClientLevel world, LevelChunk chunk) {
        return this.chunkCache.isChunkSurroundedByLoaded(chunk.getPos().x(), chunk.getPos().z());
    }

    public boolean isRegionLoaded(int blockX, int blockZ) {
        int x = (int) Math.floor(blockX / 256.0F);
        int z = (int) Math.floor(blockZ / 256.0F);
        CachedRegion cachedRegion = this.cachedRegions.get(x + "," + z);
        return cachedRegion != null && cachedRegion.isLoaded();
    }

    public int getHeightAt(int blockX, int blockZ) {
        int x = (int) Math.floor(blockX / 256.0F);
        int z = (int) Math.floor(blockZ / 256.0F);
        CachedRegion cachedRegion = this.cachedRegions.get(x + "," + z);
        return cachedRegion == null ? Short.MIN_VALUE : cachedRegion.getHeightAt(blockX, blockZ);
    }

    public void debugLog(int blockX, int blockZ) {
        int x = (int) Math.floor(blockX / 256.0F);
        int z = (int) Math.floor(blockZ / 256.0F);
        CachedRegion cachedRegion = this.cachedRegions.get(x + "," + z);
        if (cachedRegion == null) {
            VoxelConstants.getLogger().info("No Region " + x + "," + z + " at " + blockX + "," + blockZ);
        } else {
            VoxelConstants.getLogger().info("Info for region " + x + "," + z + " block " + blockX + "," + blockZ);
            int localx = blockX - x * 256;
            int localz = blockZ - z * 256;
            CompressibleMapData data = cachedRegion.getMapData();
            if (data == null) {
                VoxelConstants.getLogger().info("  No map data!");
            } else {
                VoxelConstants.getLogger().info("  Base: " + data.getHeight(localx, localz) + " Block: " + data.getBlockstate(localx, localz) + " Light: " + Integer.toHexString(data.getLight(localx, localz)));
                VoxelConstants.getLogger().info("  Foilage: " + data.getFoliageHeight(localx, localz) + " Block: " + data.getFoliageBlockstate(localx, localz) + " Light: " + Integer.toHexString(data.getFoliageLight(localx, localz)));
                VoxelConstants.getLogger().info("  Ocean Floor: " + data.getOceanFloorHeight(localx, localz) + " Block: " + data.getOceanFloorBlockstate(localx, localz) + " Light: " + Integer.toHexString(data.getOceanFloorLight(localx, localz)));
                VoxelConstants.getLogger().info("  Transparent: " + data.getTransparentHeight(localx, localz) + " Block: " + data.getTransparentBlockstate(localx, localz) + " Light: " + Integer.toHexString(data.getTransparentLight(localx, localz)));
                VoxelConstants.getLogger().info("  Biome: " + world.registryAccess().lookupOrThrow(Registries.BIOME).getKey(data.getBiome(localx, localz)) + " (" + data.getBiomeId(localx, localz) + ")");
            }
        }
    }

    private record ChunkWithAge(LevelChunk chunk, int tick) {}
    private record RegionCoordinates(int x, int z) {}
}
