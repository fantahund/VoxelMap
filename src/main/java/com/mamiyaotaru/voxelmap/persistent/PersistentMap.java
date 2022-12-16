package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.interfaces.AbstractMapData;
import com.mamiyaotaru.voxelmap.interfaces.IChangeObserver;
import com.mamiyaotaru.voxelmap.interfaces.IColorManager;
import com.mamiyaotaru.voxelmap.interfaces.IPersistentMap;
import com.mamiyaotaru.voxelmap.interfaces.ISettingsAndLightingChangeNotifier;
import com.mamiyaotaru.voxelmap.interfaces.IVoxelMap;
import com.mamiyaotaru.voxelmap.util.BiomeRepository;
import com.mamiyaotaru.voxelmap.util.BlockRepository;
import com.mamiyaotaru.voxelmap.util.ColorUtils;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.MapChunkCache;
import com.mamiyaotaru.voxelmap.util.MapUtils;
import com.mamiyaotaru.voxelmap.util.MutableBlockPos;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import com.mamiyaotaru.voxelmap.util.TickCounter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.GlassBlock;
import net.minecraft.block.Material;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PersistentMap implements IPersistentMap, IChangeObserver {
    IVoxelMap master;
    MutableBlockPos blockPos = new MutableBlockPos(0, 0, 0);
    IColorManager colorManager;
    MapSettingsManager mapOptions;
    PersistentMapSettingsManager options;
    WorldMatcher worldMatcher;
    int[] lightmapColors;
    ClientWorld world;
    String subworldName = "";
    protected final List<CachedRegion> cachedRegionsPool = Collections.synchronizedList(new ArrayList<>());
    protected final ConcurrentHashMap<String, CachedRegion> cachedRegions = new ConcurrentHashMap<>(150, 0.9F, 2);
    int lastLeft = 0;
    int lastRight = 0;
    int lastTop = 0;
    int lastBottom = 0;
    CachedRegion[] lastRegionsArray = new CachedRegion[0];
    Comparator<CachedRegion> ageThenDistanceSorter = (region1, region2) -> {
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
    Comparator<RegionCoordinates> distanceSorter = (coordinates1, coordinates2) -> {
        double distance1sq = (coordinates1.x * 256 + 128 - PersistentMap.this.options.mapX) * (coordinates1.x * 256 + 128 - PersistentMap.this.options.mapX) + (coordinates1.z * 256 + 128 - PersistentMap.this.options.mapZ) * (coordinates1.z * 256 + 128 - PersistentMap.this.options.mapZ);
        double distance2sq = (coordinates2.x * 256 + 128 - PersistentMap.this.options.mapX) * (coordinates2.x * 256 + 128 - PersistentMap.this.options.mapX) + (coordinates2.z * 256 + 128 - PersistentMap.this.options.mapZ) * (coordinates2.z * 256 + 128 - PersistentMap.this.options.mapZ);
        return Double.compare(distance1sq, distance2sq);
    };
    private boolean queuedChangedChunks = false;
    private MapChunkCache chunkCache;
    private final ConcurrentLinkedQueue<ChunkWithAge> chunkUpdateQueue = new ConcurrentLinkedQueue<>();

    public PersistentMap(IVoxelMap master) {
        this.master = master;
        this.colorManager = master.getColorManager();
        mapOptions = master.getMapOptions();
        this.options = master.getPersistentMapOptions();
        this.lightmapColors = new int[256];
        Arrays.fill(this.lightmapColors, -16777216);
    }

    @Override
    public void newWorld(ClientWorld world) {
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
                public void run() {
                    try {
                        Thread.sleep(2000L);
                    } catch (InterruptedException var2) {
                        var2.printStackTrace();
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
        String worldName = TextUtils.scrubNameFile(this.master.getWaypointManager().getCurrentWorldName());
        File oldCacheDir = new File(MinecraftClient.getInstance().runDirectory, "/mods/mamiyaotaru/voxelmap/cache/" + worldName + "/");
        if (oldCacheDir.exists() && oldCacheDir.isDirectory()) {
            File newCacheDir = new File(MinecraftClient.getInstance().runDirectory, "/voxelmap/cache/" + worldName + "/");
            newCacheDir.getParentFile().mkdirs();
            boolean success = oldCacheDir.renameTo(newCacheDir);
            if (!success) {
                System.out.println("Failed moving Voxelmap cache files.  Please move " + oldCacheDir.getPath() + " to " + newCacheDir.getPath());
            } else {
                System.out.println("Moved Voxelmap cache files from " + oldCacheDir.getPath() + " to " + newCacheDir.getPath());
            }
        }

        if (this.master.getWaypointManager().isMultiworld() && !MinecraftClient.getInstance().isInSingleplayer() && !this.master.getWaypointManager().receivedAutoSubworldName()) {
            this.worldMatcher = new WorldMatcher(this.master, this, this.world);
            this.worldMatcher.findMatch();
        }

        this.chunkCache = new MapChunkCache(33, 33, this);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.currentScreen == null) {
            this.options.mapX = GameVariableAccessShim.xCoord();
            this.options.mapZ = GameVariableAccessShim.zCoord();
        }

        if (!this.master.getWaypointManager().getCurrentSubworldDescriptor(false).equals(this.subworldName)) {
            this.subworldName = this.master.getWaypointManager().getCurrentSubworldDescriptor(false);
            if (this.worldMatcher != null && !this.subworldName.equals("")) {
                this.worldMatcher.cancel();
            }

            this.purgeCachedRegions();
        }

        if (this.queuedChangedChunks) {
            this.queuedChangedChunks = false;
            this.prunePool();
        }

        if (this.world != null) {
            this.chunkCache.centerChunks(this.blockPos.withXYZ(GameVariableAccessShim.xCoord(), 0, GameVariableAccessShim.zCoord()));
            this.chunkCache.checkIfChunksBecameSurroundedByLoaded();

            while (!this.chunkUpdateQueue.isEmpty() && Math.abs(TickCounter.tickCounter - this.chunkUpdateQueue.peek().tick) >= 20) {
                this.doProcessChunk(this.chunkUpdateQueue.remove().chunk);
            }
        }

    }

    @Override
    public PersistentMapSettingsManager getOptions() {
        return this.options;
    }

    @Override
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

    @Override
    public void renameSubworld(String oldName, String newName) {
        synchronized (this.cachedRegionsPool) {
            for (CachedRegion cachedRegion : this.cachedRegionsPool) {
                cachedRegion.renameSubworld(oldName, newName);
            }

        }
    }

    @Override
    public ISettingsAndLightingChangeNotifier getSettingsAndLightingChangeNotifier() {
        return this.master.getSettingsAndLightingChangeNotifier();
    }

    @Override
    public void setLightMapArray(int[] lightmapColors) {
        boolean changed = false;
        int torchOffset = 0;
        int skylightMultiplier = 16;

        for (int t = 0; t < 16; ++t) {
            if (lightmapColors[t * skylightMultiplier + torchOffset] != this.lightmapColors[t * skylightMultiplier + torchOffset]) {
                changed = true;
                break;
            }
        }

        System.arraycopy(lightmapColors, 0, this.lightmapColors, 0, 256);
        if (changed) {
            this.getSettingsAndLightingChangeNotifier().notifyOfChanges();
        }

    }

    @Override
    public void getAndStoreData(AbstractMapData mapData, World world, WorldChunk chunk, MutableBlockPos blockPos, boolean underground, int startX, int startZ, int imageX, int imageY) {
        int surfaceHeight;
        int seafloorHeight = 0;
        int transparentHeight = 0;
        int foliageHeight = 0;
        BlockState surfaceBlockState;
        BlockState transparentBlockState = BlockRepository.air.getDefaultState();
        BlockState foliageBlockState = BlockRepository.air.getDefaultState();
        BlockState seafloorBlockState = BlockRepository.air.getDefaultState();
        blockPos = blockPos.withXYZ(startX + imageX, 64, startZ + imageY);
        int biomeID;
        if (!chunk.isEmpty()) {
            biomeID = world.getRegistryManager().get(Registry.BIOME_KEY).getRawId(world.getBiome(blockPos).value());
        } else {
            biomeID = -1;
        }

        mapData.setBiomeID(imageX, imageY, biomeID);
        if (biomeID != -1) {
            boolean solid = false;
            if (underground) {
                surfaceHeight = this.getNetherHeight(chunk, startX + imageX, startZ + imageY);
                surfaceBlockState = chunk.getBlockState(blockPos.withXYZ(startX + imageX, surfaceHeight - 1, startZ + imageY));
                if (surfaceHeight != -1) {
                    foliageHeight = surfaceHeight + 1;
                    blockPos.setXYZ(startX + imageX, foliageHeight - 1, startZ + imageY);
                    foliageBlockState = chunk.getBlockState(blockPos);
                    Material material = foliageBlockState.getMaterial();
                    if (material == Material.SNOW_LAYER || material == Material.AIR || material == Material.LAVA || material == Material.WATER) {
                        foliageHeight = 0;
                    }
                }
            } else {
                transparentHeight = chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, blockPos.getX() & 15, blockPos.getZ() & 15) + 1;
                transparentBlockState = chunk.getBlockState(blockPos.withXYZ(startX + imageX, transparentHeight - 1, startZ + imageY));
                FluidState fluidState = transparentBlockState.getFluidState();
                if (fluidState != Fluids.EMPTY.getDefaultState()) {
                    transparentBlockState = fluidState.getBlockState();
                }

                surfaceHeight = transparentHeight;
                surfaceBlockState = transparentBlockState;
                VoxelShape voxelShape;
                boolean hasOpacity = transparentBlockState.getOpacity(world, blockPos) > 0;
                if (!hasOpacity && transparentBlockState.isOpaque() && transparentBlockState.hasSidedTransparency()) {
                    voxelShape = transparentBlockState.getCullingFace(world, blockPos, Direction.DOWN);
                    hasOpacity = VoxelShapes.unionCoversFullCube(voxelShape, VoxelShapes.empty());
                    voxelShape = transparentBlockState.getCullingFace(world, blockPos, Direction.UP);
                    hasOpacity = hasOpacity || VoxelShapes.unionCoversFullCube(VoxelShapes.empty(), voxelShape);
                }

                while (!hasOpacity && surfaceHeight > 0) {
                    foliageBlockState = surfaceBlockState;
                    --surfaceHeight;
                    surfaceBlockState = chunk.getBlockState(blockPos.withXYZ(startX + imageX, surfaceHeight - 1, startZ + imageY));
                    fluidState = surfaceBlockState.getFluidState();
                    if (fluidState != Fluids.EMPTY.getDefaultState()) {
                        surfaceBlockState = fluidState.getBlockState();
                    }

                    hasOpacity = surfaceBlockState.getOpacity(world, blockPos) > 0;
                    if (!hasOpacity && surfaceBlockState.isOpaque() && surfaceBlockState.hasSidedTransparency()) {
                        voxelShape = surfaceBlockState.getCullingFace(world, blockPos, Direction.DOWN);
                        hasOpacity = VoxelShapes.unionCoversFullCube(voxelShape, VoxelShapes.empty());
                        voxelShape = surfaceBlockState.getCullingFace(world, blockPos, Direction.UP);
                        hasOpacity = hasOpacity || VoxelShapes.unionCoversFullCube(VoxelShapes.empty(), voxelShape);
                    }
                }

                if (surfaceHeight == transparentHeight) {
                    transparentHeight = 0;
                    transparentBlockState = BlockRepository.air.getDefaultState();
                    foliageBlockState = chunk.getBlockState(blockPos.withXYZ(startX + imageX, surfaceHeight, startZ + imageY));
                }

                if (foliageBlockState.getMaterial() == Material.SNOW_LAYER) {
                    surfaceBlockState = foliageBlockState;
                    foliageBlockState = BlockRepository.air.getDefaultState();
                }

                if (foliageBlockState == transparentBlockState) {
                    foliageBlockState = BlockRepository.air.getDefaultState();
                }

                if (foliageBlockState != null && foliageBlockState.getMaterial() != Material.AIR) {
                    foliageHeight = surfaceHeight + 1;
                } else {
                    foliageBlockState = BlockRepository.air.getDefaultState();
                }

                Material material = surfaceBlockState.getMaterial();
                if (material == Material.WATER || material == Material.ICE) {
                    seafloorHeight = surfaceHeight;

                    for (seafloorBlockState = chunk.getBlockState(blockPos.withXYZ(startX + imageX, surfaceHeight - 1, startZ + imageY)); seafloorBlockState.getOpacity(world, blockPos) < 5 && seafloorBlockState.getMaterial() != Material.LEAVES && seafloorHeight > 1; seafloorBlockState = chunk.getBlockState(blockPos.withXYZ(startX + imageX, seafloorHeight - 1, startZ + imageY))) {
                        material = seafloorBlockState.getMaterial();
                        if (transparentHeight == 0 && material != Material.ICE && material != Material.WATER && material.blocksMovement()) {
                            transparentHeight = seafloorHeight;
                            transparentBlockState = seafloorBlockState;
                        }

                        if (foliageHeight == 0 && seafloorHeight != transparentHeight && transparentBlockState != seafloorBlockState && material != Material.ICE && material != Material.WATER && material != Material.AIR && material != Material.BUBBLE_COLUMN) {
                            foliageHeight = seafloorHeight;
                            foliageBlockState = seafloorBlockState;
                        }

                        --seafloorHeight;
                    }

                    if (seafloorBlockState.getMaterial() == Material.WATER) {
                        seafloorBlockState = BlockRepository.air.getDefaultState();
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
            if (surfaceHeight == -1) {
                surfaceHeight = 80;
                solid = true;
            }

            if (surfaceBlockState.getMaterial() == Material.LAVA) {
                solid = false;
            }

            int light = solid ? 0 : 255;
            if (!solid) {
                light = this.getLight(surfaceBlockState, world, blockPos, startX + imageX, startZ + imageY, surfaceHeight, solid);
            }

            mapData.setLight(imageX, imageY, light);
            int seafloorLight = 0;
            if (seafloorBlockState != null && seafloorBlockState != BlockRepository.air.getDefaultState()) {
                seafloorLight = this.getLight(seafloorBlockState, world, blockPos, startX + imageX, startZ + imageY, seafloorHeight, solid);
            }

            mapData.setOceanFloorLight(imageX, imageY, seafloorLight);
            int transparentLight = 0;
            if (transparentBlockState != null && transparentBlockState != BlockRepository.air.getDefaultState()) {
                transparentLight = this.getLight(transparentBlockState, world, blockPos, startX + imageX, startZ + imageY, transparentHeight, solid);
            }

            mapData.setTransparentLight(imageX, imageY, transparentLight);
            int foliageLight = 0;
            if (foliageBlockState != null && foliageBlockState != BlockRepository.air.getDefaultState()) {
                foliageLight = this.getLight(foliageBlockState, world, blockPos, startX + imageX, startZ + imageY, foliageHeight, solid);
            }

            mapData.setFoliageLight(imageX, imageY, foliageLight);
        }
    }

    private int getNetherHeight(WorldChunk chunk, int x, int z) {
        int y = 80;
        this.blockPos.setXYZ(x, y, z);
        BlockState blockState = chunk.getBlockState(this.blockPos);
        if (blockState.getOpacity(this.world, this.blockPos) == 0 && blockState.getMaterial() != Material.LAVA) {
            while (y > 0) {
                --y;
                this.blockPos.setXYZ(x, y, z);
                blockState = chunk.getBlockState(this.blockPos);
                if (blockState.getOpacity(this.world, this.blockPos) > 0 || blockState.getMaterial() == Material.LAVA) {
                    return y + 1;
                }
            }

            return y;
        } else {
            while (y <= 90) {
                ++y;
                this.blockPos.setXYZ(x, y, z);
                blockState = chunk.getBlockState(this.blockPos);
                if (blockState.getOpacity(this.world, this.blockPos) == 0 && blockState.getMaterial() != Material.LAVA) {
                    return y;
                }
            }

            return -1;
        }
    }

    private int getLight(BlockState blockState, World world, MutableBlockPos blockPos, int x, int z, int height, boolean solid) {
        int i3 = 255;
        if (solid) {
            i3 = 0;
        } else if (blockState != null && blockState.getMaterial() != Material.AIR) {
            blockPos.setXYZ(x, Math.max(Math.min(height, 255), 0), z);
            int blockLight = world.getLightLevel(LightType.BLOCK, blockPos) & 15;
            int skyLight = world.getLightLevel(LightType.SKY, blockPos);
            if (blockState.getMaterial() == Material.LAVA || blockState.getBlock() == Blocks.MAGMA_BLOCK) {
                blockLight = 14;
            }

            i3 = blockLight + skyLight * 16;
        }

        return i3;
    }

    @Override
    public int getPixelColor(AbstractMapData mapData, ClientWorld world, MutableBlockPos blockPos, MutableBlockPos loopBlockPos, boolean underground, int multi, int startX, int startZ, int imageX, int imageY) {
        int mcX = startX + imageX;
        int mcZ = startZ + imageY;
        BlockState surfaceBlockState;
        BlockState transparentBlockState;
        BlockState foliageBlockState;
        BlockState seafloorBlockState;
        int surfaceHeight;
        int seafloorHeight = 0;
        int transparentHeight = 0;
        int foliageHeight = 0;
        int surfaceColor;
        int seafloorColor = 0;
        int transparentColor = 0;
        int foliageColor = 0;
        blockPos = blockPos.withXYZ(mcX, 0, mcZ);
        int color24;
        int biomeID = mapData.getBiomeID(imageX, imageY);
        surfaceBlockState = mapData.getBlockstate(imageX, imageY);
        if (surfaceBlockState != null && (surfaceBlockState.getBlock() != BlockRepository.air || mapData.getLight(imageX, imageY) != 0 || mapData.getHeight(imageX, imageY) != 0) && biomeID != -1 && biomeID != 255) {
            if (mapOptions.biomeOverlay == 1) {
                if (biomeID >= 0) {
                    color24 = BiomeRepository.getBiomeColor(biomeID) | 0xFF000000;
                } else {
                    color24 = 0;
                }

            } else {
                boolean solid = false;
                int blockStateID;
                surfaceHeight = mapData.getHeight(imageX, imageY);
                blockStateID = BlockRepository.getStateId(surfaceBlockState);
                if (surfaceHeight == -1 || surfaceHeight == 255) {
                    surfaceHeight = 80;
                    solid = true;
                }

                blockPos.setXYZ(mcX, surfaceHeight - 1, mcZ);
                if (surfaceBlockState.getMaterial() == Material.LAVA) {
                    solid = false;
                }

                if (mapOptions.biomes) {
                    surfaceColor = this.colorManager.getBlockColor(blockPos, blockStateID, biomeID);
                    int tint = -1;
                    tint = this.colorManager.getBiomeTint(mapData, world, surfaceBlockState, blockStateID, blockPos, loopBlockPos, startX, startZ);
                    if (tint != -1) {
                        surfaceColor = ColorUtils.colorMultiplier(surfaceColor, tint);
                    }
                } else {
                    surfaceColor = this.colorManager.getBlockColorWithDefaultTint(blockPos, blockStateID);
                }

                surfaceColor = this.applyHeight(mapData, surfaceColor, underground, multi, imageX, imageY, surfaceHeight, solid, 1);
                int light = mapData.getLight(imageX, imageY);
                if (solid) {
                    surfaceColor = 0;
                } else if (mapOptions.lightmap) {
                    int lightValue = this.getLight(light);
                    surfaceColor = ColorUtils.colorMultiplier(surfaceColor, lightValue);
                }

                if (mapOptions.waterTransparency && !solid) {
                    seafloorHeight = mapData.getOceanFloorHeight(imageX, imageY);
                    if (seafloorHeight > 0) {
                        blockPos.setXYZ(mcX, seafloorHeight - 1, mcZ);
                        seafloorBlockState = mapData.getOceanFloorBlockstate(imageX, imageY);
                        if (seafloorBlockState != null && seafloorBlockState != BlockRepository.air.getDefaultState()) {
                            blockStateID = BlockRepository.getStateId(seafloorBlockState);
                            if (mapOptions.biomes) {
                                seafloorColor = this.colorManager.getBlockColor(blockPos, blockStateID, biomeID);
                                int tint = -1;
                                tint = this.colorManager.getBiomeTint(mapData, world, seafloorBlockState, blockStateID, blockPos, loopBlockPos, startX, startZ);
                                if (tint != -1) {
                                    seafloorColor = ColorUtils.colorMultiplier(seafloorColor, tint);
                                }
                            } else {
                                seafloorColor = this.colorManager.getBlockColorWithDefaultTint(blockPos, blockStateID);
                            }

                            seafloorColor = this.applyHeight(mapData, seafloorColor, underground, multi, imageX, imageY, seafloorHeight, solid, 0);
                            int seafloorLight;
                            seafloorLight = mapData.getOceanFloorLight(imageX, imageY);
                            if (mapOptions.lightmap) {
                                int lightValue = this.getLight(seafloorLight);
                                seafloorColor = ColorUtils.colorMultiplier(seafloorColor, lightValue);
                            }
                        }
                    }
                }

                if (mapOptions.blockTransparency && !solid) {
                    transparentHeight = mapData.getTransparentHeight(imageX, imageY);
                    if (transparentHeight > 0) {
                        blockPos.setXYZ(mcX, transparentHeight - 1, mcZ);
                        transparentBlockState = mapData.getTransparentBlockstate(imageX, imageY);
                        if (transparentBlockState != null && transparentBlockState != BlockRepository.air.getDefaultState()) {
                            blockStateID = BlockRepository.getStateId(transparentBlockState);
                            if (mapOptions.biomes) {
                                transparentColor = this.colorManager.getBlockColor(blockPos, blockStateID, biomeID);
                                int tint = -1;
                                tint = this.colorManager.getBiomeTint(mapData, world, transparentBlockState, blockStateID, blockPos, loopBlockPos, startX, startZ);
                                if (tint != -1) {
                                    transparentColor = ColorUtils.colorMultiplier(transparentColor, tint);
                                }
                            } else {
                                transparentColor = this.colorManager.getBlockColorWithDefaultTint(blockPos, blockStateID);
                            }

                            transparentColor = this.applyHeight(mapData, transparentColor, underground, multi, imageX, imageY, transparentHeight, solid, 3);
                            int transparentLight;
                            transparentLight = mapData.getTransparentLight(imageX, imageY);
                            if (mapOptions.lightmap) {
                                int lightValue = this.getLight(transparentLight);
                                transparentColor = ColorUtils.colorMultiplier(transparentColor, lightValue);
                            }
                        }
                    }

                    foliageHeight = mapData.getFoliageHeight(imageX, imageY);
                    if (foliageHeight > 0) {
                        blockPos.setXYZ(mcX, foliageHeight - 1, mcZ);
                        foliageBlockState = mapData.getFoliageBlockstate(imageX, imageY);
                        if (foliageBlockState != null && foliageBlockState != BlockRepository.air.getDefaultState()) {
                            blockStateID = BlockRepository.getStateId(foliageBlockState);
                            if (mapOptions.biomes) {
                                foliageColor = this.colorManager.getBlockColor(blockPos, blockStateID, biomeID);
                                int tint;
                                tint = this.colorManager.getBiomeTint(mapData, world, foliageBlockState, blockStateID, blockPos, loopBlockPos, startX, startZ);
                                if (tint != -1) {
                                    foliageColor = ColorUtils.colorMultiplier(foliageColor, tint);
                                }
                            } else {
                                foliageColor = this.colorManager.getBlockColorWithDefaultTint(blockPos, blockStateID);
                            }

                            foliageColor = this.applyHeight(mapData, foliageColor, underground, multi, imageX, imageY, foliageHeight, solid, 2);
                            int foliageLight;
                            foliageLight = mapData.getFoliageLight(imageX, imageY);
                            if (mapOptions.lightmap) {
                                int lightValue = this.getLight(foliageLight);
                                foliageColor = ColorUtils.colorMultiplier(foliageColor, lightValue);
                            }
                        }
                    }
                }

                if (mapOptions.waterTransparency && seafloorHeight > 0) {
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

                if (mapOptions.biomeOverlay == 2) {
                    int bc = 0;
                    if (biomeID >= 0) {
                        bc = BiomeRepository.getBiomeColor(biomeID);
                    }

                    bc = 2130706432 | bc;
                    color24 = ColorUtils.colorAdder(bc, color24);
                }

            }
            return MapUtils.doSlimeAndGrid(color24, mcX, mcZ);
        } else {
            return 0;
        }
    }

    private int applyHeight(AbstractMapData mapData, int color24, boolean underground, int multi, int imageX, int imageY, int height, boolean solid, int layer) {
        if (color24 != this.colorManager.getAirColor() && color24 != 0) {
            int heightComp = -1;
            if ((mapOptions.heightmap || mapOptions.slopemap) && !solid) {
                int diff = 0;
                double sc = 0.0;
                boolean invert = false;
                if (!mapOptions.slopemap) {
                    diff = height - 80;
                    sc = Math.log10((double) Math.abs(diff) / 8.0 + 1.0) / 1.8;
                    if (diff < 0) {
                        sc = 0.0 - sc;
                    }
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
                            if (heightComp == -1) {
                                BlockState transparentBlockState = mapData.getTransparentBlockstate(imageX, imageY);
                                if (transparentBlockState != null && transparentBlockState != BlockRepository.air.getDefaultState()) {
                                    Block block = transparentBlockState.getBlock();
                                    if (block instanceof GlassBlock || block instanceof StainedGlassBlock) {
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
                            if (heightComp == -1) {
                                BlockState transparentBlockState = mapData.getTransparentBlockstate(imageX, imageY);
                                if (transparentBlockState != null && transparentBlockState != BlockRepository.air.getDefaultState()) {
                                    Block block = transparentBlockState.getBlock();
                                    if (block instanceof GlassBlock || block instanceof StainedGlassBlock) {
                                        heightComp = mapData.getHeight(imageX + 1, imageY - 1);
                                    }
                                }
                            }
                        }

                        invert = true;
                    } else {
                        heightComp = height;
                    }

                    if (heightComp == -1) {
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
                        double heightsc = Math.log10((double) Math.abs(diff) / 8.0 + 1.0) / 3.0;
                        sc = diff > 0 ? sc + heightsc : sc - heightsc;
                    }
                }

                int alpha = color24 >> 24 & 0xFF;
                int r = color24 >> 16 & 0xFF;
                int g = color24 >> 8 & 0xFF;
                int b = color24 & 0xFF;
                if (sc > 0.0) {
                    r += (int) (sc * (double) (255 - r));
                    g += (int) (sc * (double) (255 - g));
                    b += (int) (sc * (double) (255 - b));
                } else if (sc < 0.0) {
                    sc = Math.abs(sc);
                    r -= (int) (sc * (double) r);
                    g -= (int) (sc * (double) g);
                    b -= (int) (sc * (double) b);
                }

                color24 = alpha * 16777216 + r * 65536 + g * 256 + b;
            }
        }

        return color24;
    }

    private int getLight(int light) {
        return this.lightmapColors[light];
    }

    @Override
    public CachedRegion[] getRegions(int left, int right, int top, int bottom) {
        if (left == this.lastLeft && right == this.lastRight && top == this.lastTop && bottom == this.lastBottom) {
            return this.lastRegionsArray;
        } else {
            ThreadManager.emptyQueue();
            CachedRegion[] visibleCachedRegionsArray = new CachedRegion[(right - left + 1) * (bottom - top + 1)];
            String worldName = this.master.getWaypointManager().getCurrentWorldName();
            String subWorldName = this.master.getWaypointManager().getCurrentSubworldDescriptor(false);
            List<RegionCoordinates> regionsToDisplay = new ArrayList<>();

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
                String key = "" + x + "," + z;
                CachedRegion cachedRegion;
                synchronized (this.cachedRegions) {
                    cachedRegion = this.cachedRegions.get(key);
                    if (cachedRegion == null) {
                        cachedRegion = new CachedRegion(this, key, this.world, worldName, subWorldName, x, z);
                        this.cachedRegions.put(key, cachedRegion);
                        synchronized (this.cachedRegionsPool) {
                            this.cachedRegionsPool.add(cachedRegion);
                        }
                    }
                }

                cachedRegion.refresh(true);
                visibleCachedRegionsArray[(z - top) * (right - left + 1) + (x - left)] = cachedRegion;
            }

            this.prunePool();
            synchronized (this.lastRegionsArray) {
                this.lastLeft = left;
                this.lastRight = right;
                this.lastTop = top;
                this.lastBottom = bottom;
                this.lastRegionsArray = visibleCachedRegionsArray;
                return visibleCachedRegionsArray;
            }
        }
    }

    private void prunePool() {
        synchronized (this.cachedRegionsPool) {
            Iterator<CachedRegion> iterator = this.cachedRegionsPool.iterator();

            while (iterator.hasNext()) {
                CachedRegion region = iterator.next();
                if (region.isLoaded() && region.isEmpty()) {
                    this.cachedRegions.put(region.getKey(), CachedRegion.emptyRegion);
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

    @Override
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
            WorldChunk chunk = this.world.getChunk(chunkX, chunkZ);
            if (chunk != null && !chunk.isEmpty()) {
                if (this.isChunkReady(this.world, chunk)) {
                    this.processChunk(chunk);
                }

            }
        }
    }

    @Override
    public void processChunk(WorldChunk chunk) {
        this.chunkUpdateQueue.add(new ChunkWithAge(chunk, TickCounter.tickCounter));
    }

    private void doProcessChunk(WorldChunk chunk) {
        this.queuedChangedChunks = true;

        try {
            if (this.world == null) {
                return;
            }

            if (chunk == null || chunk.isEmpty()) {
                return;
            }

            int chunkX = chunk.getPos().x;
            int chunkZ = chunk.getPos().z;
            int regionX = (int) Math.floor((double) chunkX / 16.0);
            int regionZ = (int) Math.floor((double) chunkZ / 16.0);
            String key = "" + regionX + "," + regionZ;
            CachedRegion cachedRegion;
            synchronized (this.cachedRegions) {
                cachedRegion = this.cachedRegions.get(key);
                if (cachedRegion == null || cachedRegion == CachedRegion.emptyRegion) {
                    String worldName = this.master.getWaypointManager().getCurrentWorldName();
                    String subWorldName = this.master.getWaypointManager().getCurrentSubworldDescriptor(false);
                    cachedRegion = new CachedRegion(this, key, this.world, worldName, subWorldName, regionX, regionZ);
                    this.cachedRegions.put(key, cachedRegion);
                    synchronized (this.cachedRegionsPool) {
                        this.cachedRegionsPool.add(cachedRegion);
                    }

                    synchronized (this.lastRegionsArray) {
                        if (regionX >= this.lastLeft && regionX <= this.lastRight && regionZ >= this.lastTop && regionZ <= this.lastBottom) {
                            this.lastRegionsArray[(regionZ - this.lastTop) * (this.lastRight - this.lastLeft + 1) + (regionX - this.lastLeft)] = cachedRegion;
                        }
                    }
                }
            }

            if (MinecraftClient.getInstance().currentScreen != null && MinecraftClient.getInstance().currentScreen instanceof GuiPersistentMap) {
                cachedRegion.registerChangeAt(chunkX, chunkZ);
                cachedRegion.refresh(false);
            } else {
                cachedRegion.handleChangedChunk(chunk);
            }
        } catch (Exception var19) {
            System.out.println(var19.getMessage());
            var19.printStackTrace();
        }

    }

    private boolean isChunkReady(ClientWorld world, WorldChunk chunk) {
        return this.chunkCache.isChunkSurroundedByLoaded(chunk.getPos().x, chunk.getPos().z);
    }

    @Override
    public boolean isRegionLoaded(int blockX, int blockZ) {
        int x = (int) Math.floor((float) blockX / 256.0F);
        int z = (int) Math.floor((float) blockZ / 256.0F);
        CachedRegion cachedRegion = this.cachedRegions.get(x + "," + z);
        return cachedRegion != null && cachedRegion.isLoaded();
    }

    @Override
    public boolean isGroundAt(int blockX, int blockZ) {
        int x = (int) Math.floor((float) blockX / 256.0F);
        int z = (int) Math.floor((float) blockZ / 256.0F);
        CachedRegion cachedRegion = this.cachedRegions.get(x + "," + z);
        return cachedRegion != null && cachedRegion.isGroundAt(blockX, blockZ);
    }

    @Override
    public int getHeightAt(int blockX, int blockZ) {
        int x = (int) Math.floor((float) blockX / 256.0F);
        int z = (int) Math.floor((float) blockZ / 256.0F);
        CachedRegion cachedRegion = this.cachedRegions.get(x + "," + z);
        return cachedRegion == null ? 64 : cachedRegion.getHeightAt(blockX, blockZ);
    }

    private static class ChunkWithAge {
        WorldChunk chunk;
        int tick;

        public ChunkWithAge(WorldChunk chunk, int tick) {
            this.chunk = chunk;
            this.tick = tick;
        }
    }

    private static class RegionCoordinates {
        int x;
        int z;

        public RegionCoordinates(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }
}
