package com.mamiyaotaru.voxelmap.persistent;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mamiyaotaru.voxelmap.ColorManager;
import com.mamiyaotaru.voxelmap.SettingsAndLightingChangeNotifier;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.BiomeParser;
import com.mamiyaotaru.voxelmap.util.BlockStateParser;
import com.mamiyaotaru.voxelmap.util.CommandUtils;
import com.mamiyaotaru.voxelmap.util.ColorUtils;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.MutableBlockPos;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.Level;

public class CachedRegion {
    private final static int CHUNKS_WIDTH = 16;
    private final static int CHUNK_BLOCKS = 16;
    public final static int REGION_WIDTH = CHUNKS_WIDTH * CHUNK_BLOCKS;
    public static final EmptyCachedRegion EMPTY_REGION = new EmptyCachedRegion();

    private volatile long mostRecentView;
    private volatile long mostRecentChange;
    private final PersistentMap persistentMap;
    private String key;
    private final ClientLevel world;
    private ServerLevel worldServer;
    private ServerChunkCache chunkProvider;
    private BlockableEventLoop<Runnable> executor;
    private ChunkMap chunkLoader;
    private String subworldName;
    private String worldNamePathPart;
    private String subworldNamePathPart = "";
    private String dimensionNamePathPart;
    private boolean underground;
    private int x;
    private int z;
    private volatile boolean empty = true;
    private boolean liveChunksUpdated;
    boolean remoteWorld;
    private final BitSet liveChunkUpdateQueued = new BitSet(CHUNKS_WIDTH * CHUNKS_WIDTH);
    private final BitSet dirtyImageChunks = new BitSet(CHUNKS_WIDTH * CHUNKS_WIDTH);
    private final BitSet chunkUpdateQueued = new BitSet(CHUNKS_WIDTH * CHUNKS_WIDTH);
    private final Object refreshStateLock = new Object();
    private CompressibleMapRegionTexture image;
    private CompressibleMapData data;
    private volatile PersistentMapOverviewCache.OverviewData overviewData;
    private volatile PersistentMapOverviewCache.OverviewData latestOverviewData;
    private volatile long latestOverviewSignature;
    private volatile int[] appliedOverviewLightmap;
    private volatile long evaluatedOverviewLightmapRevision = -1L;
    private volatile boolean overviewLightingPending;
    private volatile boolean overviewRelightRequired;
    final MutableBlockPos blockPos = new MutableBlockPos(0, 0, 0);
    final MutableBlockPos loopBlockPos = new MutableBlockPos(0, 0, 0);
    Future<?> future;
    private final ReentrantLock threadLock = new ReentrantLock();
    boolean displayOptionsChanged;
    boolean fullDetailLightingPending;
    volatile boolean imageChanged;
    boolean refreshQueued;
    volatile boolean refreshingImage;
    boolean dataUpdated;
    boolean dataUpdateQueued;
    boolean forceCompressRequested;
    boolean queuedRefreshForceCompress;
    boolean retainImageRequested;
    volatile boolean loaded;
    volatile boolean dataLoaded;
    volatile boolean fullImageReady;
    boolean fullDetailRequested;
    boolean overviewLookupAttempted;
    volatile boolean closed;
    private static final Object anvilLock = new Object();
    private static final ReadWriteLock tickLock = new ReentrantReadWriteLock();
    private static int loadedChunkCount;
    private boolean queuedToCompress;
    final boolean debug = false;

    public CachedRegion() {
        this.world = null;
        this.persistentMap = null;
    }

    public CachedRegion(PersistentMap persistentMap, String key, ClientLevel world, String worldName, String subworldName, int x, int z) {
        this.persistentMap = persistentMap;
        this.key = key;
        this.world = world;
        this.subworldName = subworldName;
        this.worldNamePathPart = TextUtils.scrubNameFile(worldName);
        if (!Objects.equals(subworldName, "")) {
            this.subworldNamePathPart = TextUtils.scrubNameFile(subworldName) + "/";
        }

        String dimensionName = VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(world).getStorageName();
        this.dimensionNamePathPart = TextUtils.scrubNameFile(dimensionName);
        boolean knownUnderground;
        knownUnderground = dimensionName.toLowerCase().contains("erebus");
        this.underground = world.dimensionType().cardinalLightType() != CardinalLighting.Type.NETHER && !world.dimensionType().hasSkyLight() || world.dimensionType().hasCeiling() || knownUnderground;
        this.remoteWorld = !VoxelConstants.getMinecraft().hasSingleplayerServer();
        persistentMap.getSettingsAndLightingChangeNotifier().addObserver(this);
        this.x = x;
        this.z = z;
        if (!this.remoteWorld) {
            Optional<net.minecraft.world.level.Level> optionalWorld = VoxelConstants.getWorldByKey(world.dimension());

            if (optionalWorld.isEmpty()) {
                String error = "Attempted to fetch World, but none was found!";

                VoxelConstants.getLogger().fatal(error);
                throw new IllegalStateException(error);
            }

            this.worldServer = (ServerLevel) optionalWorld.get();
            this.chunkProvider = worldServer.getChunkSource();
            this.executor = chunkProvider.mainThreadProcessor;
            this.chunkLoader = chunkProvider.chunkMap;
        }

    }

    public void renameSubworld(String oldName, String newName) {
        if (oldName.equals(this.subworldName)) {
            this.closed = true;
            this.threadLock.lock();

            try {
                this.subworldName = newName;
                if (!Objects.equals(this.subworldName, "")) {
                    this.subworldNamePathPart = TextUtils.scrubNameFile(this.subworldName) + "/";
                }
            } catch (Exception ignored) {
            } finally {
                this.threadLock.unlock();
                this.closed = false;
            }
        }

    }

    public void registerChangeAt(int chunkX, int chunkZ) {
        chunkX -= this.x * CHUNKS_WIDTH;
        chunkZ -= this.z * CHUNKS_WIDTH;
        int index = chunkZ * CHUNKS_WIDTH + chunkX;
        synchronized (this.refreshStateLock) {
            this.dataUpdateQueued = true;
            this.retainImageRequested = true;
            this.liveChunkUpdateQueued.set(index);
        }
    }

    public void notifyOfActionableChange(SettingsAndLightingChangeNotifier notifier) {
        synchronized (this.refreshStateLock) {
            this.displayOptionsChanged = true;
        }
        PersistentMapProfiler.recordDisplayChangeRequest();
    }

    public void notifyOfLightingChange(SettingsAndLightingChangeNotifier notifier) {
        boolean applied;
        synchronized (this.refreshStateLock) {
            if (this.persistentMap != null && !this.persistentMap.mapOptions.dynamicLighting) {
                applied = false;
            } else if (this.fullDetailRequested) {
                applied = true;
                this.fullDetailLightingPending = true;
                this.overviewLightingPending |= this.overviewData != null;
            } else {
                applied = this.overviewData != null;
                this.overviewLightingPending |= this.overviewData != null;
            }
        }
        PersistentMapProfiler.recordLightingChange(applied);
    }

    public void refresh(boolean forceCompress) {
        this.refresh(forceCompress, true);
    }

    void refresh(boolean forceCompress, boolean fullDetail) {
        this.mostRecentView = System.currentTimeMillis();
        synchronized (this.refreshStateLock) {
            this.forceCompressRequested |= forceCompress && (!this.loaded || this.data != null && !this.data.isCompressed());
            boolean reducingDetail = this.fullDetailRequested && !fullDetail;
            this.fullDetailRequested = fullDetail;
            if (this.future != null && (this.future.isDone() || this.future.isCancelled())) {
                if (this.future.isCancelled()) {
                    this.forceCompressRequested |= this.queuedRefreshForceCompress;
                }
                this.queuedRefreshForceCompress = false;
                this.refreshQueued = false;
            }
            if (reducingDetail) {
                this.cancelQueuedRefreshLocked();
            }
            if (!this.refreshQueued && hasRefreshWorkLocked()) {
                submitRefreshLocked();
            }
        }
    }

    private boolean hasRefreshWorkLocked() {
        return !this.closed && (!this.loaded
                || this.fullDetailRequested && (!this.dataLoaded || !this.fullImageReady)
                || this.dataUpdated
                || this.dataUpdateQueued
                || this.displayOptionsChanged
                || this.fullDetailRequested && this.fullDetailLightingPending
                || this.forceCompressRequested && (!this.loaded || this.data != null && !this.data.isCompressed()));
    }

    private void submitRefreshLocked() {
        boolean forceCompress = this.forceCompressRequested;
        this.forceCompressRequested = false;
        this.queuedRefreshForceCompress = forceCompress;
        this.refreshQueued = true;
        RefreshRunnable regionProcessingRunnable = new RefreshRunnable(forceCompress, PersistentMapProfiler.recordRefreshScheduled());
        this.future = ThreadManager.executorService.submit(regionProcessingRunnable);
    }

    void cancelRefreshIfQueued() {
        synchronized (this.refreshStateLock) {
            this.cancelQueuedRefreshLocked();
        }
    }

    private boolean cancelQueuedRefreshLocked() {
        if (this.future == null || !ThreadManager.cancelQueued(this.future)) {
            return false;
        }
        this.forceCompressRequested |= this.queuedRefreshForceCompress;
        this.queuedRefreshForceCompress = false;
        this.refreshQueued = false;
        return true;
    }

    public void handleChangedChunk(LevelChunk chunk) {
        int chunkX = chunk.getPos().x() - this.x * CHUNKS_WIDTH;
        int chunkZ = chunk.getPos().z() - this.z * CHUNKS_WIDTH;
        int index = chunkZ * CHUNKS_WIDTH + chunkX;
        synchronized (this.refreshStateLock) {
            if (this.chunkUpdateQueued.get(index)) {
                return;
            }
            this.chunkUpdateQueued.set(index);
            this.retainImageRequested = true;
            this.mostRecentView = System.currentTimeMillis();
            this.mostRecentChange = this.mostRecentView;
            FillChunkRunnable fillChunkRunnable = new FillChunkRunnable(chunk);
            ThreadManager.executorService.execute(fillChunkRunnable);
        }
    }

    private void loadFullData() {
        if (this.dataLoaded) {
            return;
        }
        long loadStartedNanos = PersistentMapProfiler.startTimer();
        try {
            this.data = new CompressibleMapData(world);
            if (this.image == null) {
                this.image = new CompressibleMapRegionTexture();
            }
            this.loadCachedData();
            this.loadCurrentData(this.world);
            if (!this.remoteWorld) {
                long anvilStartedNanos = PersistentMapProfiler.startTimer();
                try {
                    this.loadAnvilData(this.world);
                } finally {
                    PersistentMapProfiler.recordAnvilLoad(anvilStartedNanos);
                }
            }
            this.dataLoaded = true;
            this.loaded = true;
        } finally {
            PersistentMapProfiler.recordRegionLoad(loadStartedNanos, !this.empty);
        }
    }

    private boolean loadOverview() {
        File overviewFile = this.getOverviewCacheFile();
        File sourceFile = this.getCachedRegionFile();
        long startedNanos = PersistentMapProfiler.startTimer();
        Optional<PersistentMapOverviewCache.OverviewData> cachedOverview = PersistentMapOverviewCache.read(overviewFile, sourceFile, this.persistentMap.getOverviewRenderSignature());
        if (cachedOverview.isEmpty()) {
            PersistentMapProfiler.recordOverviewRead(startedNanos, overviewFile.isFile(), false);
            return false;
        }
        PersistentMap.LightmapSnapshot lightmap = this.persistentMap.getLightmapSnapshotWithRevision();
        this.overviewData = cachedOverview.get();
        byte[] displayedPixels = PersistentMapOverviewCache.applyLighting(this.overviewData, lightmap.colors(), this.persistentMap.mapOptions.dynamicLighting);
        PersistentMapProfiler.recordOverviewRead(startedNanos, overviewFile.isFile(), true);

        this.image = new CompressibleMapRegionTexture(PersistentMapOverviewCache.SIZE);
        this.image.replacePixels(PersistentMapOverviewCache.SIZE, displayedPixels);
        this.image.generateMipmaps();
        this.recordAppliedOverviewLightmap(lightmap);
        this.empty = false;
        this.loaded = true;
        this.fullImageReady = false;
        this.imageChanged = true;
        return true;
    }

    private void loadCurrentData(ClientLevel world) {
        long startedNanos = PersistentMapProfiler.startTimer();
        int chunksChecked = 0;
        int chunksLoaded = 0;
        try {
            for (int chunkX = 0; chunkX < CHUNKS_WIDTH; ++chunkX) {
                for (int chunkZ = 0; chunkZ < CHUNKS_WIDTH; ++chunkZ) {
                    ++chunksChecked;
                    LevelChunk chunk = world.getChunk(this.x * CHUNKS_WIDTH + chunkX, this.z * CHUNKS_WIDTH + chunkZ);
                    if (chunk != null && !chunk.isEmpty() && world.hasChunk(this.x * CHUNKS_WIDTH + chunkX, this.z * CHUNKS_WIDTH + chunkZ) && this.isSurroundedByLoaded(chunk)) {
                        this.loadChunkData(chunk, chunkX, chunkZ);
                        ++chunksLoaded;
                    }
                }
            }
        } finally {
            PersistentMapProfiler.recordLiveChunkScan(startedNanos, chunksChecked, chunksLoaded);
        }

    }

    private void loadModifiedData(BitSet chunksToLoad) {
        for (int index = chunksToLoad.nextSetBit(0); index >= 0; index = chunksToLoad.nextSetBit(index + 1)) {
            int chunkX = index % CHUNKS_WIDTH;
            int chunkZ = index / CHUNKS_WIDTH;
            LevelChunk chunk = this.world.getChunk(this.x * CHUNKS_WIDTH + chunkX, this.z * CHUNKS_WIDTH + chunkZ);
            if (chunk != null && !chunk.isEmpty() && this.world.hasChunk(this.x * CHUNKS_WIDTH + chunkX, this.z * CHUNKS_WIDTH + chunkZ)) {
                this.loadChunkData(chunk, chunkX, chunkZ);
            }
        }
    }

    private void loadChunkData(LevelChunk chunk, int chunkX, int chunkZ) {
        boolean isEmpty = this.isChunkEmptyOrUnlit(chunk);
        boolean isSurroundedByLoaded = this.isSurroundedByLoaded(chunk);
        if (!this.closed && this.world == GameVariableAccessShim.getWorld() && !isEmpty && isSurroundedByLoaded) {
            this.doLoadChunkData(chunk, chunkX, chunkZ);
        }

    }

    private void loadChunkDataSkipLightCheck(LevelChunk chunk, int chunkX, int chunkZ) {
        if (!this.closed && this.world == GameVariableAccessShim.getWorld() && !this.isChunkEmpty(chunk)) {
            this.doLoadChunkData(chunk, chunkX, chunkZ);
        }

    }

    private void doLoadChunkData(LevelChunk chunk, int chunkX, int chunkZ) {
        for (int t = 0; t < CHUNK_BLOCKS; ++t) {
            for (int s = 0; s < CHUNK_BLOCKS; ++s) {
                this.persistentMap.getAndStoreData(this.data, chunk.getLevel(), chunk, this.blockPos, this.underground, this.x * REGION_WIDTH, this.z * REGION_WIDTH, chunkX * CHUNK_BLOCKS + t, chunkZ * CHUNK_BLOCKS + s);
            }
        }

        this.empty = false;
        this.liveChunksUpdated = true;
        synchronized (this.refreshStateLock) {
            this.dataUpdated = true;
            this.dirtyImageChunks.set(chunkZ * CHUNKS_WIDTH + chunkX);
        }
    }

    private boolean isChunkEmptyOrUnlit(LevelChunk chunk) {
        return this.closed || chunk.isEmpty() || !chunk.getPersistedStatus().isOrAfter(ChunkStatus.FULL);
    }

    private boolean isChunkEmpty(LevelChunk chunk) {
        return this.closed || chunk.isEmpty() || !chunk.getPersistedStatus().isOrAfter(ChunkStatus.FULL);
    }

    public boolean isSurroundedByLoaded(LevelChunk chunk) {
        int chunkX = chunk.getPos().x();
        int chunkZ = chunk.getPos().z();
        boolean neighborsLoaded = !chunk.isEmpty() && VoxelConstants.getPlayer().level().hasChunk(chunkX, chunkZ);

        for (int t = chunkX - 1; t <= chunkX + 1 && neighborsLoaded; ++t) {
            for (int s = chunkZ - 1; s <= chunkZ + 1 && neighborsLoaded; ++s) {
                LevelChunk neighborChunk = VoxelConstants.getPlayer().level().getChunk(t, s);
                neighborsLoaded = neighborChunk != null && !neighborChunk.isEmpty() && VoxelConstants.getPlayer().level().hasChunk(t, s);
            }
        }

        return neighborsLoaded;
    }

    private void loadAnvilData(net.minecraft.world.level.Level world) {
        if (!this.remoteWorld) {
            boolean full = true;

            for (int t = 0; t < CHUNKS_WIDTH; ++t) {
                for (int s = 0; s < CHUNKS_WIDTH; ++s) {
                    if (!this.closed && this.data.getHeight(t * CHUNK_BLOCKS, s * CHUNK_BLOCKS) == Short.MIN_VALUE && this.data.getLight(t * CHUNK_BLOCKS, s * CHUNK_BLOCKS) == 0) {
                        full = false;
                    }
                }
            }

            if (!this.closed && !full) {
                File directory = new File(DimensionType.getStorageFolder(this.worldServer.dimension(), this.worldServer.getServer().getWorldPath(LevelResource.ROOT).normalize()).toString(), "region");
                File regionFile = new File(directory, "r." + (int) Math.floor(this.x / 2f) + "." + (int) Math.floor(this.z / 2f) + ".mca");
                if (regionFile.exists()) {
                    boolean dataChanged = false;
                    boolean loadedChunks = false;
                    ChunkAccess[] chunks = new ChunkAccess[CHUNKS_WIDTH * CHUNKS_WIDTH];
                    boolean[] chunkChanged = new boolean[CHUNKS_WIDTH * CHUNKS_WIDTH];
                    Arrays.fill(chunks, null);
                    Arrays.fill(chunkChanged, false);
                    tickLock.readLock().lock();

                    try {
                        synchronized (anvilLock) {
                            if (debug) {
                                VoxelConstants.getLogger().warn(Thread.currentThread().getName() + " starting load");
                            }

                            long loadTime = System.currentTimeMillis();
                            CompletableFuture<?> loadFuture = CompletableFuture.runAsync(() -> {
                                for (int tx = 0; tx < CHUNKS_WIDTH; ++tx) {
                                    for (int sx = 0; sx < CHUNKS_WIDTH; ++sx) {
                                        if (!this.closed && this.data.getHeight(tx * CHUNK_BLOCKS, sx * CHUNK_BLOCKS) == Short.MIN_VALUE && this.data.getLight(tx * CHUNK_BLOCKS, sx * CHUNK_BLOCKS) == 0) {
                                            int index = tx + sx * CHUNKS_WIDTH;
                                            ChunkPos chunkPos = new ChunkPos(this.x * CHUNKS_WIDTH + tx, this.z * CHUNKS_WIDTH + sx);
                                            CompoundTag rawNbt = this.chunkLoader.read(chunkPos).join().get();
                                            CompoundTag nbt = this.chunkLoader.upgradeChunkTag(rawNbt, -1);
                                            if (!this.closed && nbt.contains("Level")) {
                                                CompoundTag level = nbt.getCompound("Level").get();
                                                int chunkX = level.getInt("xPos").get();
                                                int chunkZ = level.getInt("zPos").get();
                                                if (chunkPos.x() == chunkX && chunkPos.z() == chunkZ && level.contains("Status") && ChunkStatus.byName(level.getString("Status").get()).isOrAfter(ChunkStatus.SPAWN) && level.contains("Sections")) {
                                                    ListTag sections = level.getListOrEmpty("Sections");
                                                    if (!sections.isEmpty()) {
                                                        boolean hasInfo = false;

                                                        for (int i = 0; i < sections.size() && !hasInfo && !this.closed; ++i) {
                                                            CompoundTag section = sections.getCompound(i).get();
                                                            if (section.contains("Palette") && section.contains("BlockStates")) {
                                                                hasInfo = true;
                                                            }
                                                        }

                                                        if (hasInfo) {
                                                            chunks[index] = this.worldServer.getChunk(chunkPos.x(), chunkPos.z());
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                            }, this.executor);

                            while (!this.closed && !loadFuture.isDone()) {
                                Thread.onSpinWait();
                            }

                            loadFuture.cancel(false);
                            if (debug) {
                                VoxelConstants.getLogger().warn(Thread.currentThread().getName() + " finished load after " + (System.currentTimeMillis() - loadTime) + " milliseconds");
                            }
                        }

                        if (debug) {
                            VoxelConstants.getLogger().warn(Thread.currentThread().getName() + " starting calculation");
                        }

                        long calcTime = System.currentTimeMillis();

                        for (int t = 0; t < CHUNKS_WIDTH; ++t) {
                            for (int s = 0; s < CHUNKS_WIDTH; ++s) {
                                int index = t + s * CHUNKS_WIDTH;
                                if (!this.closed && chunks[index] != null) {
                                    loadedChunks = true;
                                    ++loadedChunkCount;
                                    LevelChunk loadedChunk = null;
                                    if (chunks[index] instanceof LevelChunk) {
                                        loadedChunk = (LevelChunk) chunks[index];
                                    } else {
                                        VoxelConstants.getLogger().warn("non world chunk at " + chunks[index].getPos().x() + "," + chunks[index].getPos().z());
                                    }

                                    if (!this.closed && loadedChunk != null && loadedChunk.getPersistedStatus().isOrAfter(ChunkStatus.FULL)) {
                                        CompletableFuture<ChunkAccess> lightFuture = this.chunkProvider.getLightEngine().lightChunk(loadedChunk, false);

                                        while (!this.closed && !lightFuture.isDone()) {
                                            Thread.onSpinWait();
                                        }

                                        loadedChunk = (LevelChunk) lightFuture.getNow(loadedChunk);
                                        lightFuture.cancel(false);
                                    }

                                    if (!this.closed && loadedChunk != null && loadedChunk.getPersistedStatus().isOrAfter(ChunkStatus.FULL)) {
                                        this.loadChunkDataSkipLightCheck(loadedChunk, t, s);
                                        dataChanged = true;
                                    }
                                }
                            }
                        }

                        if (debug) {
                            VoxelConstants.getLogger().warn(Thread.currentThread().getName() + " finished calculating after " + (System.currentTimeMillis() - calcTime) + " milliseconds");
                        }
                    } catch (Exception var41) {
                        VoxelConstants.getLogger().warn("error in anvil loading");
                    } finally {
                        tickLock.readLock().unlock();
                    }

                    if (!this.closed && dataChanged) {
                        this.saveData(false);
                    }

                    if (!this.closed && loadedChunks && loadedChunkCount > 4096) {
                        loadedChunkCount = 0;
                        tickLock.writeLock().lock();

                        try {
                            CompletableFuture<Void> tickFuture = CompletableFuture.runAsync(() -> this.chunkProvider.tick(() -> true, executor.isSameThread()));
                            long tickTime = System.currentTimeMillis();
                            if (debug) {
                                VoxelConstants.getLogger().warn(Thread.currentThread().getName() + " starting chunk GC tick");
                            }

                            while (!this.closed && !tickFuture.isDone()) {
                                Thread.onSpinWait();
                            }

                            if (debug) {
                                VoxelConstants.getLogger().warn(Thread.currentThread().getName() + " finished chunk GC tick after " + (System.currentTimeMillis() - tickTime) + " milliseconds");
                            }
                        } catch (RuntimeException var38) {
                            VoxelConstants.getLogger().warn("error ticking from anvil loading");
                        } finally {
                            tickLock.writeLock().unlock();
                        }
                    }

                }
            }
        }
    }

    private void loadCachedData() {
        long lookupStartedNanos = PersistentMapProfiler.startTimer();
        boolean lookupRecorded = false;
        boolean filePresent = false;
        long decompressedBytes = 0L;
        try {
            File cachedRegionFileDir = this.getRegionCacheDirectory();
            cachedRegionFileDir.mkdirs();
            File cachedRegionFile = this.getCachedRegionFile();
            filePresent = cachedRegionFile.exists();
            PersistentMapProfiler.recordCacheLookup(lookupStartedNanos, filePresent);
            lookupRecorded = true;
            if (filePresent) {
                long readStartedNanos = PersistentMapProfiler.startTimer();
                try (ZipFile zFile = new ZipFile(cachedRegionFile)) {
                    ZipEntry ze = zFile.getEntry("data");
                    InputStream is = zFile.getInputStream(ze);
                    byte[] decompressedByteData = is.readAllBytes();
                    decompressedBytes = decompressedByteData.length;
                    is.close();
                    ze = zFile.getEntry("key");
                    is = zFile.getInputStream(ze);
                    BiMap<BlockState, Integer> blockstateMap = HashBiMap.create();
                    Scanner sc = new Scanner(is);

                    while (sc.hasNextLine()) {
                        BlockStateParser.parseLine(sc.nextLine(), blockstateMap);
                    }
                    sc.close();
                    is.close();

                    BiMap<Biome, Integer> biomeMap = HashBiMap.create();
                    ze = zFile.getEntry("biomes");
                    if (ze != null) {
                        is = zFile.getInputStream(ze);
                        sc = new Scanner(is);

                        while (sc.hasNextLine()) {
                            BiomeParser.parseLine(world, sc.nextLine(), biomeMap);
                        }
                    } else {
                        BiomeParser.populateLegacyBiomeMap(world, biomeMap);
                    }

                    sc.close();
                    is.close();
                    int version = 1;
                    ze = zFile.getEntry("control");
                    if (ze != null) {
                        is = zFile.getInputStream(ze);
                        if (is != null) {
                            Properties properties = new Properties();
                            properties.load(is);
                            String versionString = properties.getProperty("version", "1");

                            try {
                                version = Integer.parseInt(versionString);
                            } catch (NumberFormatException ignored) {}

                            is.close();
                        }
                    }

                    if (decompressedByteData.length == this.data.getExpectedDataLength(version)) {
                        this.data.setData(decompressedByteData, blockstateMap, biomeMap, version);
                        this.empty = false;
                        synchronized (this.refreshStateLock) {
                            this.dataUpdated = true;
                        }
                    } else {
                        VoxelConstants.getLogger().warn("failed to load data from " + cachedRegionFile.getPath());
                    }

                    if (version < 2) {
                        this.liveChunksUpdated = true;
                    }
                } finally {
                    PersistentMapProfiler.recordCacheRead(readStartedNanos, decompressedBytes);
                }
            }
        } catch (Exception ex) {
            VoxelConstants.getLogger().error("Failed to load region file for " + this.x + "," + this.z + " in " + this.worldNamePathPart + "/" + this.subworldNamePathPart + this.dimensionNamePathPart, ex);
        } finally {
            if (!lookupRecorded) {
                PersistentMapProfiler.recordCacheLookup(lookupStartedNanos, filePresent);
            }
        }

    }

    private void saveData(boolean newThread) {
        if (this.data != null && this.liveChunksUpdated && !this.worldNamePathPart.isEmpty()) {
            if (newThread) {
                ThreadManager.saveExecutorService.execute(() -> {
                    if (VoxelConstants.DEBUG) {
                        VoxelConstants.getLogger().info("Saving region file for " + CachedRegion.this.x + "," + CachedRegion.this.z + " in " + CachedRegion.this.worldNamePathPart + "/" + CachedRegion.this.subworldNamePathPart + CachedRegion.this.dimensionNamePathPart);
                    }
                    CachedRegion.this.threadLock.lock();

                    try {
                        CachedRegion.this.doSave();
                    } catch (Exception ex) {
                        VoxelConstants.getLogger().error("Failed to save region file for " + CachedRegion.this.x + "," + CachedRegion.this.z + " in " + CachedRegion.this.worldNamePathPart + "/" + CachedRegion.this.subworldNamePathPart + CachedRegion.this.dimensionNamePathPart, ex);
                    } finally {
                        CachedRegion.this.threadLock.unlock();
                    }
                    if (VoxelConstants.DEBUG) {
                        VoxelConstants.getLogger().info("Finished saving region file for " + CachedRegion.this.x + "," + CachedRegion.this.z + " in " + CachedRegion.this.worldNamePathPart + "/" + CachedRegion.this.subworldNamePathPart + CachedRegion.this.dimensionNamePathPart + " ("
                                + ThreadManager.saveExecutorService.getQueue().size() + ")");
                    }
                });
            } else {
                try {
                    this.doSave();
                } catch (Exception ex) {
                    VoxelConstants.getLogger().error(ex);
                }
            }

            this.liveChunksUpdated = false;
        }

    }

    private void doSave() throws IOException {
        BiMap<BlockState, Integer> stateToInt = this.data.getStateToInt();
        BiMap<Biome, Integer> biomeToInt = this.data.getBiomeToInt();
        byte[] byteArray = this.data.getData();
        if (byteArray.length == this.data.getExpectedDataLength(CompressibleMapData.DATA_VERSION)) {
            File cachedRegionFileDir = this.getRegionCacheDirectory();
            cachedRegionFileDir.mkdirs();
            File cachedRegionFile = this.getCachedRegionFile();
            try (FileOutputStream fos = new FileOutputStream(cachedRegionFile); ZipOutputStream zos = new ZipOutputStream(fos)) {
                ZipEntry ze = new ZipEntry("data");
                ze.setSize(byteArray.length);
                zos.putNextEntry(ze);
                zos.write(byteArray);
                zos.closeEntry();
                if (stateToInt != null) {
                    StringBuilder stringBuffer = new StringBuilder();

                    for (Entry<BlockState, Integer> entry : stateToInt.entrySet()) {
                        String nextLine = entry.getValue() + " " + entry.getKey().toString() + "\r\n";
                        stringBuffer.append(nextLine);
                    }

                    byte[] keyByteArray = String.valueOf(stringBuffer).getBytes();
                    ze = new ZipEntry("key");
                    ze.setSize(keyByteArray.length);
                    zos.putNextEntry(ze);
                    zos.write(keyByteArray);
                    zos.closeEntry();
                }
                if (biomeToInt != null) {
                    StringBuilder stringBuffer = new StringBuilder();

                    for (Entry<Biome, Integer> entry : biomeToInt.entrySet()) {
                        try {
                            String nextLine = entry.getValue() + " " + world.registryAccess().lookupOrThrow(Registries.BIOME).getKey(entry.getKey()).toString() + "\r\n";
                            stringBuffer.append(nextLine);
                        } catch (NullPointerException ex) {
                            VoxelConstants.getLogger().warn("Nullpointer for Biome: " + entry.getValue() + " at " + this.x + "," + this.z + " in " + this.worldNamePathPart + "/" + this.subworldNamePathPart + this.dimensionNamePathPart);
                        }
                    }

                    byte[] keyByteArray = String.valueOf(stringBuffer).getBytes();
                    ze = new ZipEntry("biomes");
                    ze.setSize(keyByteArray.length);
                    zos.putNextEntry(ze);
                    zos.write(keyByteArray);
                    zos.closeEntry();
                }

                String nextLine = "version:" + CompressibleMapData.DATA_VERSION + "\r\n";
                byte[] keyByteArray = nextLine.getBytes();
                ze = new ZipEntry("control");
                ze.setSize(keyByteArray.length);
                zos.putNextEntry(ze);
                zos.write(keyByteArray);
                zos.closeEntry();
            }
            this.rewriteOverviewAfterSourceSave();
        } else {
            VoxelConstants.getLogger().warn("Data array wrong size: " + byteArray.length + "for " + this.x + "," + this.z + " in " + this.worldNamePathPart + "/" + this.subworldNamePathPart + this.dimensionNamePathPart);
        }

    }

    private File getRegionCacheDirectory() {
        return VoxelConstants.getVoxelMapInstance().getDataStore().getWorldCacheDir(this.subworldNamePathPart + this.dimensionNamePathPart);
    }

    private File getCachedRegionFile() {
        return new File(this.getRegionCacheDirectory(), this.key + ".zip");
    }

    private File getOverviewCacheFile() {
        return new File(new File(this.getRegionCacheDirectory(), "overview-v3"), this.key + ".vmo");
    }

    private void queueOverviewSave(PersistentMapOverviewCache.OverviewData overview) {
        File overviewFile = this.getOverviewCacheFile();
        File sourceFile = this.getCachedRegionFile();
        long renderSignature = this.persistentMap.getOverviewRenderSignature();
        this.latestOverviewData = overview;
        this.latestOverviewSignature = renderSignature;
        ThreadManager.saveExecutorService.execute(() -> {
            long startedNanos = PersistentMapProfiler.startTimer();
            boolean success = false;
            try {
                PersistentMapOverviewCache.write(overviewFile, sourceFile, renderSignature, overview);
                success = true;
            } catch (IOException | RuntimeException exception) {
                VoxelConstants.getLogger().warn("Failed to save overview for region " + this.x + "," + this.z, exception);
            } finally {
                PersistentMapProfiler.recordOverviewWrite(startedNanos, PersistentMapOverviewCache.RAW_BYTES, success);
            }
        });
    }

    private void rewriteOverviewAfterSourceSave() {
        PersistentMapOverviewCache.OverviewData overview = this.latestOverviewData;
        if (overview == null) {
            return;
        }
        long startedNanos = PersistentMapProfiler.startTimer();
        boolean success = false;
        try {
            PersistentMapOverviewCache.write(this.getOverviewCacheFile(), this.getCachedRegionFile(), this.latestOverviewSignature, overview);
            success = true;
        } catch (IOException | RuntimeException exception) {
            VoxelConstants.getLogger().warn("Failed to refresh overview metadata for region " + this.x + "," + this.z, exception);
        } finally {
            PersistentMapProfiler.recordOverviewWrite(startedNanos, PersistentMapOverviewCache.RAW_BYTES, success);
        }
    }

    private void fillImage(BitSet dirtyPixels) {
        this.image.prepareSize(REGION_WIDTH);
        long renderViewStartedNanos = PersistentMapProfiler.startTimer();
        CompressibleMapData.RenderView renderView;
        ColorManager.PersistentMapRenderContext renderContext;
        try {
            renderView = this.data.openRenderView();
            renderContext = this.persistentMap.colorManager.createPersistentMapRenderContext(
                    renderView, this.world, this.x * REGION_WIDTH, this.z * REGION_WIDTH, renderView::getBiomeRegistryId);
        } finally {
            PersistentMapProfiler.recordRenderViewCreation(renderViewStartedNanos);
        }

        long coloringStartedNanos = PersistentMapProfiler.startTimer();
        int pixelsColored = dirtyPixels == null ? REGION_WIDTH * REGION_WIDTH : dirtyPixels.cardinality();
        int[] overviewLightmap = this.persistentMap.getLightmapSnapshot();
        OverviewAccumulator overviewAccumulator = null;
        try {
            NativeImage target = this.image.getData();
            PersistentMap.PixelRenderOutput renderOutput = new PersistentMap.PixelRenderOutput();
            if (dirtyPixels == null) {
                overviewAccumulator = new OverviewAccumulator();
                for (int z = 0; z < REGION_WIDTH; ++z) {
                    for (int x = 0; x < REGION_WIDTH; ++x) {
                        int color24 = this.persistentMap.getPixelColor(renderView, renderContext, this.world, this.blockPos, this.loopBlockPos, this.underground, 8, this.x * REGION_WIDTH, this.z * REGION_WIDTH, x, z, renderOutput);
                        target.setPixel(x, z, ColorUtils.premultiplyWithAlpha(color24));
                        overviewAccumulator.accept(x, z, renderOutput, color24);
                    }
                }
                this.overviewData = overviewAccumulator.build(overviewLightmap);
            } else {
                for (int index = dirtyPixels.nextSetBit(0); index >= 0; index = dirtyPixels.nextSetBit(index + 1)) {
                    int x = index % REGION_WIDTH;
                    int z = index / REGION_WIDTH;
                    int color24 = this.persistentMap.getPixelColor(renderView, renderContext, this.world, this.blockPos, this.loopBlockPos, this.underground, 8, this.x * REGION_WIDTH, this.z * REGION_WIDTH, x, z);
                    target.setPixel(x, z, ColorUtils.premultiplyWithAlpha(color24));
                }
                this.overviewData = this.updateOverviewCells(renderView, renderContext, dirtyPixels, renderOutput, overviewLightmap);
            }
            this.image.markContentValid();
        } finally {
            PersistentMapProfiler.recordImageColoring(coloringStartedNanos, pixelsColored, dirtyPixels != null);
        }

        long mipmapStartedNanos = PersistentMapProfiler.startTimer();
        try {
            this.image.generateMipmaps();
        } finally {
            PersistentMapProfiler.recordMipmapGeneration(mipmapStartedNanos, REGION_WIDTH * REGION_WIDTH);
        }
        if (overviewAccumulator != null && overviewAccumulator.hasComparisonData()) {
            long comparisonStartedNanos = PersistentMapProfiler.startTimer();
            overviewAccumulator.compare(
                    this.overviewData,
                    overviewLightmap,
                    this.persistentMap.mapOptions.dynamicLighting,
                    this.image.copyMipLevelBytes(2),
                    comparisonStartedNanos);
        }
    }

    private PersistentMapOverviewCache.OverviewData updateOverviewCells(
            CompressibleMapData.RenderView renderView,
            ColorManager.PersistentMapRenderContext renderContext,
            BitSet dirtyPixels,
            PersistentMap.PixelRenderOutput renderOutput,
            int[] lightmap) {
        if (this.overviewData == null) {
            OverviewAccumulator accumulator = new OverviewAccumulator();
            for (int z = 0; z < REGION_WIDTH; ++z) {
                for (int x = 0; x < REGION_WIDTH; ++x) {
                    int displayedColor = this.persistentMap.getPixelColor(renderView, renderContext, this.world, this.blockPos, this.loopBlockPos, this.underground, 8, this.x * REGION_WIDTH, this.z * REGION_WIDTH, x, z, renderOutput);
                    accumulator.accept(x, z, renderOutput, displayedColor);
                }
            }
            return accumulator.build(lightmap);
        }

        PersistentMapOverviewCache.OverviewData updated = this.overviewData.copy();
        BitSet dirtyOverviewPixels = new BitSet(PersistentMapOverviewCache.PIXEL_COUNT);
        for (int pixel = dirtyPixels.nextSetBit(0); pixel >= 0; pixel = dirtyPixels.nextSetBit(pixel + 1)) {
            int x = pixel % REGION_WIDTH;
            int z = pixel / REGION_WIDTH;
            dirtyOverviewPixels.set((z / 4) * PersistentMapOverviewCache.SIZE + x / 4);
        }

        for (int overviewPixel = dirtyOverviewPixels.nextSetBit(0); overviewPixel >= 0; overviewPixel = dirtyOverviewPixels.nextSetBit(overviewPixel + 1)) {
            int overviewX = overviewPixel % PersistentMapOverviewCache.SIZE;
            int overviewZ = overviewPixel / PersistentMapOverviewCache.SIZE;
            OverviewPixelAccumulator accumulator = new OverviewPixelAccumulator();
            for (int z = overviewZ * 4; z < overviewZ * 4 + 4; ++z) {
                for (int x = overviewX * 4; x < overviewX * 4 + 4; ++x) {
                    int displayedColor = this.persistentMap.getPixelColor(renderView, renderContext, this.world, this.blockPos, this.loopBlockPos, this.underground, 8, this.x * REGION_WIDTH, this.z * REGION_WIDTH, x, z, renderOutput);
                    accumulator.accept(renderOutput, displayedColor);
                }
            }
            accumulator.write(updated, overviewPixel, lightmap);
        }
        return updated;
    }

    private void saveImage() {
        if (!this.empty && this.image != null) {

            File imageFileDir = VoxelConstants.getVoxelMapInstance().getDataStore().getWorldCacheDir(this.subworldNamePathPart + this.dimensionNamePathPart + "/images/z1");
            imageFileDir.mkdirs();
            final File imageFile = new File(imageFileDir, this.key + ".png");
                       
            if (this.liveChunksUpdated || !imageFile.exists()) {
                NativeImage toSave = new NativeImage(REGION_WIDTH, REGION_WIDTH, false);
                toSave.copyFrom(this.image.getData());
                ThreadManager.executorService.execute(() -> {
                    try {
                        toSave.writeToFile(imageFile);
                    } catch (IOException e) {
                        VoxelConstants.getLogger().error(e);
                    } finally {
                        toSave.close();
                    }
                });
            }
        }

    }

    public long getMostRecentView() {
        return this.mostRecentView;
    }

    public long getMostRecentChange() {
        return this.mostRecentChange;
    }

    public String getKey() {
        return this.key;
    }

    public int getX() {
        return this.x;
    }

    public int getZ() {
        return this.z;
    }

    public int getWidth() {
        return REGION_WIDTH;
    }

    public int getTextureWidth() {
        return this.image == null ? REGION_WIDTH : this.image.getDisplaySize();
    }

    public Identifier getTextureLocation(float zoom) {
        if (this.image != null) {
            if (!this.refreshingImage) {
                synchronized (this.image) {
                    if (this.imageChanged) {
                        this.imageChanged = false;
                        this.image.uploadToTexture();
                    }
                }
            }

            return this.image.getTextureLocation(zoom);
        } else {
            return null;
        }
    }

    public CompressibleMapData getMapData() {
        return this.data;
    }

    public boolean isLoaded() {
        return this.loaded;
    }

    public boolean isEmpty() {
        return this.empty;
    }

    public int getHeightAt(int blockX, int blockZ) {
        int x = blockX - this.x * REGION_WIDTH;
        int z = blockZ - this.z * REGION_WIDTH;
        int y = this.data == null ? Short.MIN_VALUE : this.data.getHeight(x, z);
        if (this.underground && y == 255) {
            y = CommandUtils.getSafeHeight(blockX, 64, blockZ, this.world);
        }

        return y;
    }

    public void compress() {
        if (this.data != null && !this.isCompressed() && !this.queuedToCompress) {
            this.queuedToCompress = true;
            ThreadManager.executorService.execute(() -> {
                if (this.threadLock.tryLock()) {
                    try {
                        this.compressData();
                    } catch (RuntimeException ignored) {
                    } finally {
                        this.threadLock.unlock();
                    }
                }

                this.queuedToCompress = false;
            });
        }

    }

    private void compressData() {
        if (!PersistentMapProfiler.isActive() || this.data.isCompressed()) {
            this.data.compress();
            return;
        }

        long startedNanos = PersistentMapProfiler.startTimer();
        try {
            this.data.compress();
        } finally {
            PersistentMapProfiler.recordDataCompression(startedNanos, this.data.getExpectedDataLength(CompressibleMapData.DATA_VERSION));
        }
    }

    private boolean isCompressed() {
        return this.data.isCompressed();
    }

    static final class OverviewAccumulator {
        private static final int COMPONENTS = 2;

        private final int[][] red = new int[COMPONENTS][PersistentMapOverviewCache.PIXEL_COUNT];
        private final int[][] green = new int[COMPONENTS][PersistentMapOverviewCache.PIXEL_COUNT];
        private final int[][] blue = new int[COMPONENTS][PersistentMapOverviewCache.PIXEL_COUNT];
        private final int[][] litRed = new int[COMPONENTS][PersistentMapOverviewCache.PIXEL_COUNT];
        private final int[][] litGreen = new int[COMPONENTS][PersistentMapOverviewCache.PIXEL_COUNT];
        private final int[][] litBlue = new int[COMPONENTS][PersistentMapOverviewCache.PIXEL_COUNT];
        private final int[][] blockLight = new int[COMPONENTS][PersistentMapOverviewCache.PIXEL_COUNT];
        private final int[][] skyLight = new int[COMPONENTS][PersistentMapOverviewCache.PIXEL_COUNT];
        private final byte[][] lightSamples = new byte[COMPONENTS][PersistentMapOverviewCache.PIXEL_COUNT];
        private final int[] alpha = new int[PersistentMapOverviewCache.PIXEL_COUNT];
        private final byte[] waterSamples = PersistentMapProfiler.isActive()
                ? new byte[PersistentMapOverviewCache.PIXEL_COUNT]
                : null;

        void accept(int x, int z, PersistentMap.PixelRenderOutput output, int displayedColor) {
            int overviewPixel = (z / 4) * PersistentMapOverviewCache.SIZE + x / 4;
            this.alpha[overviewPixel] += ColorUtils.premultiplyWithAlpha(displayedColor) >> 24 & 0xFF;
            PersistentMap.SplitLightingAccumulator split = output.splitLightingAccumulator;
            for (int component = 0; component < COMPONENTS; ++component) {
                this.red[component][overviewPixel] += split.unlitRed(component);
                this.green[component][overviewPixel] += split.unlitGreen(component);
                this.blue[component][overviewPixel] += split.unlitBlue(component);
                this.litRed[component][overviewPixel] += split.litRed(component);
                this.litGreen[component][overviewPixel] += split.litGreen(component);
                this.litBlue[component][overviewPixel] += split.litBlue(component);
                if (split.hasComponent(component)) {
                    int light = split.light(component);
                    this.blockLight[component][overviewPixel] += light & 0xF;
                    this.skyLight[component][overviewPixel] += light >> 4 & 0xF;
                    ++this.lightSamples[component][overviewPixel];
                }
            }
            if (this.waterSamples != null && output.overviewWater) {
                ++this.waterSamples[overviewPixel];
            }
        }

        PersistentMapOverviewCache.OverviewData build(int[] lightmap) {
            byte[] primaryPixels = new byte[PersistentMapOverviewCache.PRIMARY_COLOR_BYTES];
            byte[] secondaryPixels = new byte[PersistentMapOverviewCache.SECONDARY_COLOR_BYTES];
            byte[] primaryLightValues = new byte[PersistentMapOverviewCache.LIGHT_BYTES];
            byte[] secondaryLightValues = new byte[PersistentMapOverviewCache.LIGHT_BYTES];
            for (int pixel = 0; pixel < PersistentMapOverviewCache.PIXEL_COUNT; ++pixel) {
                writeComponent(
                        primaryPixels,
                        pixel * 4,
                        primaryLightValues,
                        pixel,
                        this.red[0][pixel],
                        this.green[0][pixel],
                        this.blue[0][pixel],
                        this.litRed[0][pixel],
                        this.litGreen[0][pixel],
                        this.litBlue[0][pixel],
                        this.blockLight[0][pixel],
                        this.skyLight[0][pixel],
                        Byte.toUnsignedInt(this.lightSamples[0][pixel]),
                        lightmap);
                primaryPixels[pixel * 4 + 3] = roundedAverage(this.alpha[pixel]);
                writeComponent(
                        secondaryPixels,
                        pixel * 3,
                        secondaryLightValues,
                        pixel,
                        this.red[1][pixel],
                        this.green[1][pixel],
                        this.blue[1][pixel],
                        this.litRed[1][pixel],
                        this.litGreen[1][pixel],
                        this.litBlue[1][pixel],
                        this.blockLight[1][pixel],
                        this.skyLight[1][pixel],
                        Byte.toUnsignedInt(this.lightSamples[1][pixel]),
                        lightmap);
            }
            return new PersistentMapOverviewCache.OverviewData(
                    primaryPixels, secondaryPixels, primaryLightValues, secondaryLightValues);
        }

        boolean hasComparisonData() {
            return this.waterSamples != null;
        }

        void compare(
                PersistentMapOverviewCache.OverviewData overview,
                int[] lightmap,
                boolean dynamicLighting,
                byte[] referencePixels,
                long startedNanos) {
            byte[] overviewPixels = PersistentMapOverviewCache.applyLighting(overview, lightmap, dynamicLighting);
            PersistentMapProfiler.OverviewComparisonMetrics metrics = new PersistentMapProfiler.OverviewComparisonMetrics();
            for (int pixel = 0; pixel < PersistentMapOverviewCache.PIXEL_COUNT; ++pixel) {
                int offset = pixel * 4;
                metrics.record(
                        this.waterSamples[pixel] != 0,
                        Byte.toUnsignedInt(referencePixels[offset]),
                        Byte.toUnsignedInt(referencePixels[offset + 1]),
                        Byte.toUnsignedInt(referencePixels[offset + 2]),
                        Byte.toUnsignedInt(overviewPixels[offset]),
                        Byte.toUnsignedInt(overviewPixels[offset + 1]),
                        Byte.toUnsignedInt(overviewPixels[offset + 2]));
            }
            PersistentMapProfiler.recordOverviewComparison(startedNanos, metrics);
        }
    }

    OverviewLightingUpdateResult updateOverviewLighting(PersistentMap.LightmapSnapshot currentLightmap, boolean allowUpdate) {
        PersistentMapOverviewCache.OverviewData overview = this.overviewData;
        if (this.closed || overview == null || this.image == null) {
            return OverviewLightingUpdateResult.NO_WORK;
        }

        boolean switchToOverview = this.image.getImageSize() != PersistentMapOverviewCache.SIZE;
        boolean dynamicLighting = this.persistentMap.mapOptions.dynamicLighting;
        if (dynamicLighting && this.evaluatedOverviewLightmapRevision != currentLightmap.revision()) {
            this.overviewLightingPending = true;
        }
        if (!switchToOverview && (!this.overviewLightingPending || !dynamicLighting)) {
            return OverviewLightingUpdateResult.NO_WORK;
        }

        boolean thresholdChecked = false;
        if (switchToOverview) {
            this.overviewRelightRequired = true;
        } else if (!this.overviewRelightRequired) {
            long checkStartedNanos = PersistentMapProfiler.startTimer();
            boolean updateRequired = PersistentMapOverviewCache.lightingDifferenceExceedsThreshold(
                    overview, this.appliedOverviewLightmap, currentLightmap.colors());
            PersistentMapProfiler.recordOverviewLightingCheck(checkStartedNanos, updateRequired);
            thresholdChecked = true;
            if (!updateRequired) {
                this.recordEvaluatedOverviewLightmap(currentLightmap.revision());
                return OverviewLightingUpdateResult.CHECKED_NO_UPDATE;
            }
            this.overviewRelightRequired = true;
        }

        if (!allowUpdate) {
            PersistentMapProfiler.recordOverviewLightingDeferral();
            return OverviewLightingUpdateResult.pending(thresholdChecked);
        }

        long startedNanos = PersistentMapProfiler.startTimer();
        synchronized (this.image) {
            if (this.closed || this.overviewData != overview) {
                this.overviewRelightRequired = true;
                return OverviewLightingUpdateResult.pending(thresholdChecked);
            }
            boolean changedResolution = this.image.getImageSize() != PersistentMapOverviewCache.SIZE;
            byte[] pixels = PersistentMapOverviewCache.applyLighting(overview, currentLightmap.colors(), dynamicLighting);
            this.image.replacePixels(PersistentMapOverviewCache.SIZE, pixels);
            this.image.generateMipmaps();
            this.recordAppliedOverviewLightmap(currentLightmap);
            if (changedResolution) {
                this.fullImageReady = false;
            }
            this.imageChanged = true;
        }
        PersistentMapProfiler.recordOverviewRelight(startedNanos);
        PersistentMapProfiler.recordOverviewLightingUpdate();
        return OverviewLightingUpdateResult.updated(thresholdChecked);
    }

    record OverviewLightingUpdateResult(boolean thresholdChecked, boolean updated, boolean updatePending) {
        private static final OverviewLightingUpdateResult NO_WORK = new OverviewLightingUpdateResult(false, false, false);
        private static final OverviewLightingUpdateResult CHECKED_NO_UPDATE = new OverviewLightingUpdateResult(true, false, false);

        private static OverviewLightingUpdateResult pending(boolean thresholdChecked) {
            return new OverviewLightingUpdateResult(thresholdChecked, false, true);
        }

        private static OverviewLightingUpdateResult updated(boolean thresholdChecked) {
            return new OverviewLightingUpdateResult(thresholdChecked, true, false);
        }
    }

    static final class OverviewPixelAccumulator {
        private static final int COMPONENTS = 2;

        private final int[] red = new int[COMPONENTS];
        private final int[] green = new int[COMPONENTS];
        private final int[] blue = new int[COMPONENTS];
        private final int[] litRed = new int[COMPONENTS];
        private final int[] litGreen = new int[COMPONENTS];
        private final int[] litBlue = new int[COMPONENTS];
        private final int[] blockLight = new int[COMPONENTS];
        private final int[] skyLight = new int[COMPONENTS];
        private final int[] lightSamples = new int[COMPONENTS];
        private int alpha;

        void accept(PersistentMap.PixelRenderOutput output, int displayedColor) {
            this.alpha += ColorUtils.premultiplyWithAlpha(displayedColor) >> 24 & 0xFF;
            PersistentMap.SplitLightingAccumulator split = output.splitLightingAccumulator;
            for (int component = 0; component < COMPONENTS; ++component) {
                this.red[component] += split.unlitRed(component);
                this.green[component] += split.unlitGreen(component);
                this.blue[component] += split.unlitBlue(component);
                this.litRed[component] += split.litRed(component);
                this.litGreen[component] += split.litGreen(component);
                this.litBlue[component] += split.litBlue(component);
                if (split.hasComponent(component)) {
                    int light = split.light(component);
                    this.blockLight[component] += light & 0xF;
                    this.skyLight[component] += light >> 4 & 0xF;
                    ++this.lightSamples[component];
                }
            }
        }

        void write(PersistentMapOverviewCache.OverviewData overview, int pixel, int[] lightmap) {
            writeComponent(
                    overview.primaryPixels(),
                    pixel * 4,
                    overview.primaryLightValues(),
                    pixel,
                    this.red[0],
                    this.green[0],
                    this.blue[0],
                    this.litRed[0],
                    this.litGreen[0],
                    this.litBlue[0],
                    this.blockLight[0],
                    this.skyLight[0],
                    this.lightSamples[0],
                    lightmap);
            overview.primaryPixels()[pixel * 4 + 3] = roundedAverage(this.alpha);
            writeComponent(
                    overview.secondaryPixels(),
                    pixel * 3,
                    overview.secondaryLightValues(),
                    pixel,
                    this.red[1],
                    this.green[1],
                    this.blue[1],
                    this.litRed[1],
                    this.litGreen[1],
                    this.litBlue[1],
                    this.blockLight[1],
                    this.skyLight[1],
                    this.lightSamples[1],
                    lightmap);
        }
    }

    private void recordAppliedOverviewLightmap(PersistentMap.LightmapSnapshot lightmap) {
        synchronized (this.refreshStateLock) {
            this.appliedOverviewLightmap = lightmap.colors();
            this.evaluatedOverviewLightmapRevision = lightmap.revision();
            this.overviewLightingPending = lightmap.revision() != this.persistentMap.getLightmapRevision();
            this.overviewRelightRequired = false;
        }
    }

    private void recordEvaluatedOverviewLightmap(long revision) {
        synchronized (this.refreshStateLock) {
            this.evaluatedOverviewLightmapRevision = revision;
            this.overviewLightingPending = revision != this.persistentMap.getLightmapRevision();
            this.overviewRelightRequired = false;
        }
    }

    private static void writeComponent(
            byte[] colors,
            int colorOffset,
            byte[] lights,
            int pixel,
            int redSum,
            int greenSum,
            int blueSum,
            int litRedSum,
            int litGreenSum,
            int litBlueSum,
            int blockLightSum,
            int skyLightSum,
            int lightSamples,
            int[] lightmap) {
        int baseRed = roundedAverageInt(redSum);
        int baseGreen = roundedAverageInt(greenSum);
        int baseBlue = roundedAverageInt(blueSum);
        colors[colorOffset] = (byte) baseRed;
        colors[colorOffset + 1] = (byte) baseGreen;
        colors[colorOffset + 2] = (byte) baseBlue;
        if (lightSamples == 0) {
            lights[pixel] = (byte) 0xFF;
            return;
        }
        int preferredLight = averageLight(blockLightSum, skyLightSum, lightSamples);
        lights[pixel] = (byte) PersistentMapOverviewCache.findBestLight(
                baseRed,
                baseGreen,
                baseBlue,
                roundedAverageInt(litRedSum),
                roundedAverageInt(litGreenSum),
                roundedAverageInt(litBlueSum),
                preferredLight,
                lightmap);
    }

    private static byte roundedAverage(int sumOfSixteen) {
        return (byte) roundedAverageInt(sumOfSixteen);
    }

    private static int roundedAverageInt(int sumOfSixteen) {
        return (sumOfSixteen + 8) / 16;
    }

    private static int averageLight(int blockLightSum, int skyLightSum, int samples) {
        int blockLight = Math.min(15, (blockLightSum + samples / 2) / samples);
        int skyLight = Math.min(15, (skyLightSum + samples / 2) / samples);
        return blockLight | skyLight << 4;
    }

    public void cleanup() {
        this.closed = true;
        this.queuedToCompress = true;
        if (this.future != null) {
            this.future.cancel(false);
        }

        this.persistentMap.getSettingsAndLightingChangeNotifier().removeObserver(this);
        if (this.image != null) {
            if (this.persistentMap.getOptions().outputImages && this.fullImageReady) {
                this.saveImage();
            }

            this.threadLock.lock();
            try {
                this.image.deleteTexture();
            } finally {
                this.threadLock.unlock();
            }
        }

        this.saveData(true);
    }

    private final class FillChunkRunnable implements Runnable {
        private final LevelChunk chunk;
        private final int index;

        private FillChunkRunnable(LevelChunk chunk) {
            this.chunk = chunk;
            int chunkX = chunk.getPos().x() - CachedRegion.this.x * CHUNKS_WIDTH;
            int chunkZ = chunk.getPos().z() - CachedRegion.this.z * CHUNKS_WIDTH;
            this.index = chunkZ * CHUNKS_WIDTH + chunkX;
        }

        @Override
        public void run() {
            CachedRegion.this.threadLock.lock();

            try {
                if (!CachedRegion.this.dataLoaded) {
                    CachedRegion.this.loadFullData();
                }

                int chunkX = this.chunk.getPos().x() - CachedRegion.this.x * CHUNKS_WIDTH;
                int chunkZ = this.chunk.getPos().z() - CachedRegion.this.z * CHUNKS_WIDTH;
                CachedRegion.this.loadChunkData(this.chunk, chunkX, chunkZ);
            } catch (Exception ex) {
                VoxelConstants.getLogger().log(Level.ERROR, "Error in FillChunkRunnable", ex);
            } finally {
                CachedRegion.this.threadLock.unlock();
                synchronized (CachedRegion.this.refreshStateLock) {
                    CachedRegion.this.chunkUpdateQueued.clear(this.index);
                }
            }

        }
    }

    private final class RefreshRunnable implements Runnable {
        private final boolean forceCompress;
        private final long queuedAtNanos;

        private RefreshRunnable(boolean forceCompress, long queuedAtNanos) {
            this.forceCompress = forceCompress;
            this.queuedAtNanos = queuedAtNanos;
        }

        @Override
        public void run() {
            long taskStartedNanos = PersistentMapProfiler.recordRefreshStarted(this.queuedAtNanos);
            synchronized (CachedRegion.this.refreshStateLock) {
                CachedRegion.this.queuedRefreshForceCompress = false;
            }
            CachedRegion.this.mostRecentChange = System.currentTimeMillis();
            CachedRegion.this.threadLock.lock();

            BitSet chunksToLoad = new BitSet(CHUNKS_WIDTH * CHUNKS_WIDTH);
            BitSet dirtyChunks = new BitSet(CHUNKS_WIDTH * CHUNKS_WIDTH);
            boolean renderRequested = false;
            boolean fullRender = false;
            boolean lightingRenderRequested = false;
            boolean failed = false;

            try {
                boolean fullDetail;
                boolean attemptOverview;
                boolean overviewLoaded = false;
                synchronized (CachedRegion.this.refreshStateLock) {
                    fullDetail = CachedRegion.this.fullDetailRequested;
                    attemptOverview = !CachedRegion.this.loaded && !CachedRegion.this.overviewLookupAttempted;
                    CachedRegion.this.overviewLookupAttempted |= attemptOverview;
                }
                if (attemptOverview) {
                    overviewLoaded = CachedRegion.this.loadOverview();
                }
                synchronized (CachedRegion.this.refreshStateLock) {
                    fullDetail = CachedRegion.this.fullDetailRequested;
                }
                if (overviewLoaded && !fullDetail) {
                    PersistentMapProfiler.recordRawRegionLoadSkipped(PersistentMapOverviewCache.RAW_BYTES);
                }
                if (!CachedRegion.this.loaded || fullDetail && !CachedRegion.this.dataLoaded) {
                    CachedRegion.this.loadFullData();
                }

                synchronized (CachedRegion.this.refreshStateLock) {
                    chunksToLoad.or(CachedRegion.this.liveChunkUpdateQueued);
                    CachedRegion.this.liveChunkUpdateQueued.clear();
                    CachedRegion.this.dataUpdateQueued = false;
                }

                if (!chunksToLoad.isEmpty()) {
                    if (!CachedRegion.this.dataLoaded) {
                        CachedRegion.this.loadFullData();
                    }
                    CachedRegion.this.loadModifiedData(chunksToLoad);
                }

                boolean retainImage;
                synchronized (CachedRegion.this.refreshStateLock) {
                    retainImage = CachedRegion.this.retainImageRequested;
                    CachedRegion.this.retainImageRequested = false;
                    lightingRenderRequested = CachedRegion.this.fullDetailLightingPending;
                    renderRequested = !CachedRegion.this.empty && (CachedRegion.this.dataUpdated
                            || CachedRegion.this.displayOptionsChanged
                            || CachedRegion.this.fullDetailRequested && lightingRenderRequested
                            || CachedRegion.this.fullDetailRequested && !CachedRegion.this.fullImageReady);
                    fullRender = CachedRegion.this.displayOptionsChanged || lightingRenderRequested || !CachedRegion.this.fullImageReady;
                    dirtyChunks.or(CachedRegion.this.dirtyImageChunks);
                    if (renderRequested) {
                        CachedRegion.this.dataUpdated = false;
                        CachedRegion.this.displayOptionsChanged = false;
                        CachedRegion.this.fullDetailLightingPending = false;
                        CachedRegion.this.dirtyImageChunks.clear();
                    }
                }

                if (retainImage) {
                    CachedRegion.this.image.enableRetainedBacking();
                }

                if (renderRequested) {
                    if (!CachedRegion.this.dataLoaded) {
                        CachedRegion.this.loadFullData();
                    }
                    BitSet dirtyPixels = null;
                    if (!fullRender
                            && !dirtyChunks.isEmpty()
                            && dirtyChunks.cardinality() < CHUNKS_WIDTH * CHUNKS_WIDTH
                            && CachedRegion.this.image.getImageSize() == REGION_WIDTH
                            && CachedRegion.this.image.canPartiallyUpdate()) {
                        dirtyPixels = DirtyPixelMask.fromChunks(dirtyChunks);
                    }
                    CachedRegion.this.refreshingImage = true;
                    synchronized (CachedRegion.this.image) {
                        CachedRegion.this.fillImage(dirtyPixels);
                        PersistentMapOverviewCache.OverviewData renderedOverview = CachedRegion.this.overviewData;
                        CachedRegion.this.queueOverviewSave(renderedOverview);
                        CachedRegion.this.fullImageReady = true;
                        boolean keepFullResolution;
                        synchronized (CachedRegion.this.refreshStateLock) {
                            keepFullResolution = CachedRegion.this.fullDetailRequested;
                        }
                        if (!keepFullResolution) {
                            PersistentMap.LightmapSnapshot lightmap = CachedRegion.this.persistentMap.getLightmapSnapshotWithRevision();
                            byte[] overviewPixels = PersistentMapOverviewCache.applyLighting(renderedOverview, lightmap.colors(), CachedRegion.this.persistentMap.mapOptions.dynamicLighting);
                            CachedRegion.this.image.replacePixels(PersistentMapOverviewCache.SIZE, overviewPixels);
                            CachedRegion.this.image.generateMipmaps();
                            CachedRegion.this.recordAppliedOverviewLightmap(lightmap);
                            CachedRegion.this.fullImageReady = false;
                        }
                        CachedRegion.this.imageChanged = true;
                    }
                    CachedRegion.this.refreshingImage = false;
                }

                if (this.forceCompress && CachedRegion.this.data != null) {
                    CachedRegion.this.compressData();
                }
            } catch (Exception exception) {
                failed = true;
                synchronized (CachedRegion.this.refreshStateLock) {
                    CachedRegion.this.liveChunkUpdateQueued.or(chunksToLoad);
                    CachedRegion.this.dataUpdateQueued |= !chunksToLoad.isEmpty();
                    if (renderRequested) {
                        CachedRegion.this.dataUpdated = true;
                        CachedRegion.this.displayOptionsChanged |= fullRender;
                        CachedRegion.this.fullDetailLightingPending |= lightingRenderRequested;
                        CachedRegion.this.dirtyImageChunks.or(dirtyChunks);
                    }
                }
                VoxelConstants.getLogger().error("Exception loading region: " + exception.getLocalizedMessage(), exception);
            } finally {
                CachedRegion.this.refreshingImage = false;
                CachedRegion.this.threadLock.unlock();
                synchronized (CachedRegion.this.refreshStateLock) {
                    CachedRegion.this.refreshQueued = false;
                    if (!failed && CachedRegion.this.hasRefreshWorkLocked()) {
                        CachedRegion.this.submitRefreshLocked();
                    }
                }
                PersistentMapProfiler.recordRefreshCompleted(taskStartedNanos);
            }

        }
    }
}
