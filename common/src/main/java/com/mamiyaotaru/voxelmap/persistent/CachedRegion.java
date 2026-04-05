package com.mamiyaotaru.voxelmap.persistent;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mamiyaotaru.voxelmap.SettingsAndLightingChangeNotifier;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.BiomeParser;
import com.mamiyaotaru.voxelmap.util.BlockStateParser;
import com.mamiyaotaru.voxelmap.util.CommandUtils;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.MutableBlockPos;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
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

public class CachedRegion {
    private final static int CHUNKS_WIDTH = 16;
    private final static int CHUNK_BLOCKS = 16;
    public final static int REGION_WIDTH = CHUNKS_WIDTH * CHUNK_BLOCKS;
    public static final EmptyCachedRegion EMPTY_REGION = new EmptyCachedRegion();

    private long mostRecentView;
    private long mostRecentChange;
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
    private int sectionY;
    private boolean empty = true;
    private boolean liveChunksUpdated;
    boolean remoteWorld;
    private final boolean[] liveChunkUpdateQueued = new boolean[CHUNKS_WIDTH * CHUNKS_WIDTH];
    private final boolean[] chunkUpdateQueued = new boolean[CHUNKS_WIDTH * CHUNKS_WIDTH];
    private CompressibleMapRegionTexture image;
    private CompressibleMapData data;
    final MutableBlockPos blockPos = new MutableBlockPos(0, 0, 0);
    final MutableBlockPos loopBlockPos = new MutableBlockPos(0, 0, 0);
    Future<?> future;
    private final ReentrantLock threadLock = new ReentrantLock();
    boolean displayOptionsChanged;
    boolean imageChanged;
    boolean refreshQueued;
    boolean refreshingImage;
    boolean dataUpdated;
    boolean dataUpdateQueued;
    boolean loaded;
    boolean closed;
    private static final Object anvilLock = new Object();
    private static final ReadWriteLock tickLock = new ReentrantReadWriteLock();
    private static int loadedChunkCount;
    private boolean queuedToCompress;
    final boolean debug = false;

    public CachedRegion() {
        this.world = null;
        this.persistentMap = null;
    }

    public CachedRegion(PersistentMap persistentMap, String key, ClientLevel world, String worldName, String subworldName, int x, int z, boolean underground, int sectionY) {
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
        this.underground = underground;
        this.remoteWorld = !VoxelConstants.getMinecraft().hasSingleplayerServer();
        persistentMap.getSettingsAndLightingChangeNotifier().addObserver(this);
        this.x = x;
        this.z = z;
        this.sectionY = sectionY;
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

        Arrays.fill(this.liveChunkUpdateQueued, false);
        Arrays.fill(this.chunkUpdateQueued, false);
    }

    public static String buildFullPath(String worldName, String subWorldName, String dimensionName, boolean underground, int sectionY) {
        return String.join("/", buildRootPath(worldName), buildLayerPath(subWorldName, dimensionName, underground, sectionY));
    }

    public static String buildRootPath(String worldName) {
        return String.join("/", "voxelmap", "cache", worldName);
    }

    public static String buildLayerPath(String subWorldName, String dimensionName, boolean underground, int sectionY) {
        if (underground) {
            return String.join("/", subWorldName, dimensionName, String.valueOf(sectionY));
        }
        return String.join("/", subWorldName, dimensionName);
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
        this.dataUpdateQueued = true;
        int index = chunkZ * CHUNKS_WIDTH + chunkX;
        this.liveChunkUpdateQueued[index] = true;
    }

    public void notifyOfActionableChange(SettingsAndLightingChangeNotifier notifier) {
        this.displayOptionsChanged = true;
    }

    public void refresh(boolean forceCompress) {
        this.mostRecentView = System.currentTimeMillis();
        if (this.future != null && (this.future.isDone() || this.future.isCancelled())) {
            this.refreshQueued = false;
        }

        if (!this.refreshQueued) {
            this.refreshQueued = true;
            if (this.loaded && !this.dataUpdated && !this.dataUpdateQueued && !this.displayOptionsChanged) {
                this.refreshQueued = false;
            } else {
                RefreshRunnable regionProcessingRunnable = new RefreshRunnable(forceCompress);
                this.future = ThreadManager.executorService.submit(regionProcessingRunnable);
            }

        }
    }

    public void handleChangedChunk(LevelChunk chunk) {
        int chunkX = chunk.getPos().x - this.x * CHUNKS_WIDTH;
        int chunkZ = chunk.getPos().z - this.z * CHUNKS_WIDTH;
        int index = chunkZ * CHUNKS_WIDTH + chunkX;
        if (!this.chunkUpdateQueued[index]) {
            this.chunkUpdateQueued[index] = true;
            this.mostRecentView = System.currentTimeMillis();
            this.mostRecentChange = this.mostRecentView;
            FillChunkRunnable fillChunkRunnable = new FillChunkRunnable(chunk);
            ThreadManager.executorService.execute(fillChunkRunnable);
        }
    }

    private void load() {
        this.data = new CompressibleMapData(world);
        this.image = new CompressibleMapRegionTexture();
        this.loadCachedData();
        this.loadCurrentData(this.world);
        if (!this.remoteWorld) {
            this.loadAnvilData(this.world);
        }

        this.loaded = true;
    }

    private void loadCurrentData(ClientLevel world) {
        for (int chunkX = 0; chunkX < CHUNKS_WIDTH; ++chunkX) {
            for (int chunkZ = 0; chunkZ < CHUNKS_WIDTH; ++chunkZ) {
                LevelChunk chunk = world.getChunk(this.x * CHUNKS_WIDTH + chunkX, this.z * CHUNKS_WIDTH + chunkZ);
                if (chunk != null && !chunk.isEmpty() && world.hasChunk(this.x * CHUNKS_WIDTH + chunkX, this.z * CHUNKS_WIDTH + chunkZ) && this.isSurroundedByLoaded(chunk)) {
                    this.loadChunkData(chunk, chunkX, chunkZ);
                }
            }
        }

    }

    private void loadModifiedData() {
        for (int chunkX = 0; chunkX < CHUNKS_WIDTH; ++chunkX) {
            for (int chunkZ = 0; chunkZ < CHUNKS_WIDTH; ++chunkZ) {
                if (this.liveChunkUpdateQueued[chunkZ * CHUNKS_WIDTH + chunkX]) {
                    this.liveChunkUpdateQueued[chunkZ * CHUNKS_WIDTH + chunkX] = false;
                    LevelChunk chunk = this.world.getChunk(this.x * CHUNKS_WIDTH + chunkX, this.z * CHUNKS_WIDTH + chunkZ);
                    if (chunk != null && !chunk.isEmpty() && this.world.hasChunk(this.x * CHUNKS_WIDTH + chunkX, this.z * CHUNKS_WIDTH + chunkZ)) {
                        this.loadChunkData(chunk, chunkX, chunkZ);
                    }
                }
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
                this.persistentMap.getAndStoreData(this.data, chunk.getLevel(), chunk, this.blockPos, this.underground, this.x * REGION_WIDTH, this.z * REGION_WIDTH, chunkX * CHUNK_BLOCKS + t, chunkZ * CHUNK_BLOCKS + s, this.sectionY);
            }
        }

        this.empty = false;
        this.liveChunksUpdated = true;
        this.dataUpdated = true;
    }

    private boolean isChunkEmptyOrUnlit(LevelChunk chunk) {
        return this.closed || chunk.isEmpty() || !chunk.getPersistedStatus().isOrAfter(ChunkStatus.FULL);
    }

    private boolean isChunkEmpty(LevelChunk chunk) {
        return this.closed || chunk.isEmpty() || !chunk.getPersistedStatus().isOrAfter(ChunkStatus.FULL);
    }

    public boolean isSurroundedByLoaded(LevelChunk chunk) {
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
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
                                                if (chunkPos.x == chunkX && chunkPos.z == chunkZ && level.contains("Status") && ChunkStatus.byName(level.getString("Status").get()).isOrAfter(ChunkStatus.SPAWN) && level.contains("Sections")) {
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
                                                            chunks[index] = this.worldServer.getChunk(chunkPos.x, chunkPos.z);
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
                                        VoxelConstants.getLogger().warn("non world chunk at " + chunks[index].getPos().x + "," + chunks[index].getPos().z);
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
        try {
            File cachedRegionFileDir = new File(VoxelConstants.getMinecraft().gameDirectory, buildFullPath(this.worldNamePathPart, this.subworldNamePathPart, this.dimensionNamePathPart, this.underground, this.sectionY));
            cachedRegionFileDir.mkdirs();
            File cachedRegionFile = new File(cachedRegionFileDir, "/" + this.key + ".zip");
            if (cachedRegionFile.exists()) {
                ZipFile zFile = new ZipFile(cachedRegionFile);
                ZipEntry ze = zFile.getEntry("data");
                InputStream is = zFile.getInputStream(ze);
                byte[] decompressedByteData = is.readAllBytes();
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

                zFile.close();
                if (decompressedByteData.length == this.data.getExpectedDataLength(version)) {
                    this.data.setData(decompressedByteData, blockstateMap, biomeMap, version);
                    this.empty = false;
                    this.dataUpdated = true;
                } else {
                    VoxelConstants.getLogger().warn("failed to load data from " + cachedRegionFile.getPath());
                }

                if (version < 2) {
                    this.liveChunksUpdated = true;
                }
            }
        } catch (Exception ex) {
            VoxelConstants.getLogger().error("Failed to load region file for " + this.x + "," + this.z + " in " + this.worldNamePathPart + "/" + this.subworldNamePathPart + this.dimensionNamePathPart, ex);
        }

    }

    private void saveData(boolean newThread) {
        if (this.liveChunksUpdated && !this.worldNamePathPart.isEmpty()) {
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
            File cachedRegionFileDir = new File(VoxelConstants.getMinecraft().gameDirectory, buildFullPath(this.worldNamePathPart, this.subworldNamePathPart, this.dimensionNamePathPart, this.underground, this.sectionY));
            cachedRegionFileDir.mkdirs();
            File cachedRegionFile = new File(cachedRegionFileDir, "/" + this.key + ".zip");
            FileOutputStream fos = new FileOutputStream(cachedRegionFile);
            ZipOutputStream zos = new ZipOutputStream(fos);
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
            zos.close();
            fos.close();
        } else {
            VoxelConstants.getLogger().warn("Data array wrong size: " + byteArray.length + "for " + this.x + "," + this.z + " in " + this.worldNamePathPart + "/" + this.subworldNamePathPart + this.dimensionNamePathPart);
        }

    }

    private void fillImage() {
        for (int t = 0; t < REGION_WIDTH; ++t) {
            for (int s = 0; s < REGION_WIDTH; ++s) {
                int color24 = this.persistentMap.getPixelColor(this.data, this.world, this.blockPos, this.loopBlockPos, this.underground, 8, this.x * REGION_WIDTH, this.z * REGION_WIDTH, t, s, this.sectionY);
                this.image.setRGB(t, s, color24);
            }
        }
        this.image.generateMipmaps();
    }

    private void saveImage() {
        if (!this.empty && this.image != null) {

            File imageFileDir = new File(VoxelConstants.getMinecraft().gameDirectory, buildFullPath(this.worldNamePathPart, this.subworldNamePathPart, this.dimensionNamePathPart, this.underground, this.sectionY) + "/images/z1");
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
        this.data.compress();
    }

    private boolean isCompressed() {
        return this.data.isCompressed();
    }

    public void cleanup() {
        this.closed = true;
        this.queuedToCompress = true;
        if (this.future != null) {
            this.future.cancel(false);
        }

        this.persistentMap.getSettingsAndLightingChangeNotifier().removeObserver(this);
        if (this.image != null) {
            if (this.persistentMap.getOptions().outputImages) {
                this.saveImage();
            }
            this.image.deleteTexture();
        }

        this.saveData(true);
    }

    private final class FillChunkRunnable implements Runnable {
        private final LevelChunk chunk;
        private final int index;

        private FillChunkRunnable(LevelChunk chunk) {
            this.chunk = chunk;
            int chunkX = chunk.getPos().x - CachedRegion.this.x * CHUNKS_WIDTH;
            int chunkZ = chunk.getPos().z - CachedRegion.this.z * CHUNKS_WIDTH;
            this.index = chunkZ * CHUNKS_WIDTH + chunkX;
        }

        @Override
        public void run() {
            CachedRegion.this.threadLock.lock();

            try {
                if (!CachedRegion.this.loaded) {
                    CachedRegion.this.load();
                }

                int chunkX = this.chunk.getPos().x - CachedRegion.this.x * CHUNKS_WIDTH;
                int chunkZ = this.chunk.getPos().z - CachedRegion.this.z * CHUNKS_WIDTH;
                CachedRegion.this.loadChunkData(this.chunk, chunkX, chunkZ);
            } catch (Exception ex) {
                VoxelConstants.getLogger().log(Level.ERROR, "Error in FillChunkRunnable", ex);
            } finally {
                CachedRegion.this.threadLock.unlock();
                CachedRegion.this.chunkUpdateQueued[this.index] = false;
            }

        }
    }

    private final class RefreshRunnable implements Runnable {
        private final boolean forceCompress;

        private RefreshRunnable(boolean forceCompress) { this.forceCompress = forceCompress; }

        @Override
        public void run() {
            CachedRegion.this.mostRecentChange = System.currentTimeMillis();
            CachedRegion.this.threadLock.lock();

            try {
                if (!CachedRegion.this.loaded) {
                    CachedRegion.this.load();
                }

                if (CachedRegion.this.dataUpdateQueued) {
                    CachedRegion.this.loadModifiedData();
                    CachedRegion.this.dataUpdateQueued = false;
                }

                if (CachedRegion.this.dataUpdated || CachedRegion.this.displayOptionsChanged) {
                    CachedRegion.this.dataUpdated = false;
                    CachedRegion.this.displayOptionsChanged = false;
                    CachedRegion.this.refreshingImage = true;
                    synchronized (CachedRegion.this.image) {
                        CachedRegion.this.fillImage();
                        CachedRegion.this.imageChanged = true;
                    }
                    CachedRegion.this.refreshingImage = false;
                }

                if (this.forceCompress) {
                    CachedRegion.this.compressData();
                }
            } catch (Exception var8) {
                VoxelConstants.getLogger().error("Exception loading region: " + var8.getLocalizedMessage(), var8);
            } finally {
                CachedRegion.this.threadLock.unlock();
                CachedRegion.this.refreshQueued = false;
            }

        }
    }
}
