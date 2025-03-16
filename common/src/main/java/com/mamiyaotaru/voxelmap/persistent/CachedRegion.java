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
import com.mamiyaotaru.voxelmap.util.ReflectionUtils;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import org.apache.logging.log4j.Level;

import javax.imageio.ImageIO;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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

import java.awt.image.*;
import java.io.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
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
    public static final EmptyCachedRegion emptyRegion = new EmptyCachedRegion();
    private long mostRecentView;
    private long mostRecentChange;
    private PersistentMap persistentMap;
    private String key;
    private final ClientLevel world;
    private ServerLevel worldServer;
    private ServerChunkCache chunkProvider;
    private BlockableEventLoop<RefreshRunnable> executor;
    private ChunkMap chunkLoader;
    private String subworldName;
    private String worldNamePathPart;
    private String subworldNamePathPart = "";
    private String dimensionNamePathPart;
    private boolean underground;
    private int x;
    private int z;
    private final int width = 256;
    private boolean empty = true;
    private boolean liveChunksUpdated;
    boolean remoteWorld;
    private final boolean[] liveChunkUpdateQueued = new boolean[256];
    private final boolean[] chunkUpdateQueued = new boolean[256];
    private CompressibleGLBufferedImage image;
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
        this.underground = !world.effects().forceBrightLightmap() && !world.dimensionType().hasSkyLight() || world.dimensionType().hasCeiling() || knownUnderground;
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
            Class<?> executorClass = chunkProvider.getClass().getDeclaredClasses()[0];
            this.executor = (BlockableEventLoop<RefreshRunnable>) ReflectionUtils.getPrivateFieldValueByType(chunkProvider, ServerChunkCache.class, executorClass);
            this.chunkLoader = chunkProvider.chunkMap;
        }

        Arrays.fill(this.liveChunkUpdateQueued, false);
        Arrays.fill(this.chunkUpdateQueued, false);
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
        chunkX -= this.x * 16;
        chunkZ -= this.z * 16;
        this.dataUpdateQueued = true;
        int index = chunkZ * 16 + chunkX;
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
        int chunkX = chunk.getPos().x - this.x * 16;
        int chunkZ = chunk.getPos().z - this.z * 16;
        int index = chunkZ * 16 + chunkX;
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
        this.image = new CompressibleGLBufferedImage(256, 256, 6);
        this.loadCachedData();
        this.loadCurrentData(this.world);
        if (!this.remoteWorld) {
            this.loadAnvilData(this.world);
        }

        this.loaded = true;
    }

    private void loadCurrentData(ClientLevel world) {
        for (int chunkX = 0; chunkX < 16; ++chunkX) {
            for (int chunkZ = 0; chunkZ < 16; ++chunkZ) {
                LevelChunk chunk = world.getChunk(this.x * 16 + chunkX, this.z * 16 + chunkZ);
                if (chunk != null && !chunk.isEmpty() && world.hasChunk(this.x * 16 + chunkX, this.z * 16 + chunkZ) && this.isSurroundedByLoaded(chunk)) {
                    this.loadChunkData(chunk, chunkX, chunkZ);
                }
            }
        }

    }

    private void loadModifiedData() {
        for (int chunkX = 0; chunkX < 16; ++chunkX) {
            for (int chunkZ = 0; chunkZ < 16; ++chunkZ) {
                if (this.liveChunkUpdateQueued[chunkZ * 16 + chunkX]) {
                    this.liveChunkUpdateQueued[chunkZ * 16 + chunkX] = false;
                    LevelChunk chunk = this.world.getChunk(this.x * 16 + chunkX, this.z * 16 + chunkZ);
                    if (chunk != null && !chunk.isEmpty() && this.world.hasChunk(this.x * 16 + chunkX, this.z * 16 + chunkZ)) {
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
        for (int t = 0; t < 16; ++t) {
            for (int s = 0; s < 16; ++s) {
                this.persistentMap.getAndStoreData(this.data, chunk.getLevel(), chunk, this.blockPos, this.underground, this.x * 256, this.z * 256, chunkX * 16 + t, chunkZ * 16 + s);
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

            for (int t = 0; t < 16; ++t) {
                for (int s = 0; s < 16; ++s) {
                    if (!this.closed && this.data.getHeight(t * 16, s * 16) == Short.MIN_VALUE && this.data.getLight(t * 16, s * 16) == 0) {
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
                    ChunkAccess[] chunks = new ChunkAccess[256];
                    boolean[] chunkChanged = new boolean[256];
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
                                for (int tx = 0; tx < 16; ++tx) {
                                    for (int sx = 0; sx < 16; ++sx) {
                                        if (!this.closed && this.data.getHeight(tx * 16, sx * 16) == Short.MIN_VALUE && this.data.getLight(tx * 16, sx * 16) == 0) {
                                            int index = tx + sx * 16;
                                            ChunkPos chunkPos = new ChunkPos(this.x * 16 + tx, this.z * 16 + sx);
                                            CompoundTag rawNbt = this.chunkLoader.read(chunkPos).join().get();
                                            CompoundTag nbt = this.chunkLoader.upgradeChunkTag(this.worldServer.dimension(), () -> this.worldServer.getDataStorage(), rawNbt, Optional.empty());
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

                        for (int t = 0; t < 16; ++t) {
                            for (int s = 0; s < 16; ++s) {
                                int index = t + s * 16;
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
            File cachedRegionFileDir = new File(VoxelConstants.getMinecraft().gameDirectory, "/voxelmap/cache/" + this.worldNamePathPart + "/" + this.subworldNamePathPart + this.dimensionNamePathPart);
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

    @SuppressWarnings("OverlyBroadThrowsClause")
    private void doSave() throws IOException {
        BiMap<BlockState, Integer> stateToInt = this.data.getStateToInt();
        BiMap<Biome, Integer> biomeToInt = this.data.getBiomeToInt();
        byte[] byteArray = this.data.getData();
        if (byteArray.length == this.data.getExpectedDataLength(CompressibleMapData.DATA_VERSION)) {
            File cachedRegionFileDir = new File(VoxelConstants.getMinecraft().gameDirectory, "/voxelmap/cache/" + this.worldNamePathPart + "/" + this.subworldNamePathPart + this.dimensionNamePathPart);
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
                Iterator<Map.Entry<BlockState, Integer>> iterator = stateToInt.entrySet().iterator();
                StringBuilder stringBuffer = new StringBuilder();

                while (iterator.hasNext()) {
                    Map.Entry<BlockState, Integer> entry = iterator.next();
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
                Iterator<Map.Entry<Biome, Integer>> iterator = biomeToInt.entrySet().iterator();
                StringBuilder stringBuffer = new StringBuilder();

                while (iterator.hasNext()) {
                    Map.Entry<Biome, Integer> entry = iterator.next();
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
        for (int t = 0; t < 256; ++t) {
            for (int s = 0; s < 256; ++s) {
                int color24 = this.persistentMap.getPixelColor(this.data, this.world, this.blockPos, this.loopBlockPos, this.underground, 8, this.x * 256, this.z * 256, t, s);
                this.image.setRGB(t, s, color24);
            }
        }
    }

    private void saveImage() {
        if (!this.empty) {
            File imageFileDir = new File(VoxelConstants.getMinecraft().gameDirectory, "/voxelmap/cache/" + this.worldNamePathPart + "/" + this.subworldNamePathPart + this.dimensionNamePathPart + "/images/z1");
            imageFileDir.mkdirs();
            final File imageFile = new File(imageFileDir, this.key + ".png");
            if (this.liveChunksUpdated || !imageFile.exists()) {
                ThreadManager.executorService.execute(() -> {
                    CachedRegion.this.threadLock.lock();

                    try {
                        BufferedImage realBufferedImage = new BufferedImage(this.width, this.width, 6);
                        byte[] dstArray = ((DataBufferByte) realBufferedImage.getRaster().getDataBuffer()).getData();
                        System.arraycopy(CachedRegion.this.image.getData(), 0, dstArray, 0, this.image.getData().length);
                        ImageIO.write(realBufferedImage, "png", imageFile);
                    } catch (IOException var6) {
                        VoxelConstants.getLogger().error(var6);
                    } finally {
                        this.threadLock.unlock();
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
        return this.width;
    }

    public int getGLID() {
        if (this.image != null) {
            if (!this.refreshingImage) {
                synchronized (this.image) {
                    if (this.imageChanged) {
                        this.imageChanged = false;
                        this.image.write();
                    }
                }
            }

            return this.image.getIndex();
        } else {
            return 0;
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

    public boolean isGroundAt(int blockX, int blockZ) {
        return this.isLoaded() && this.getHeightAt(blockX, blockZ) > 0;
    }

    public int getHeightAt(int blockX, int blockZ) {
        int x = blockX - this.x * 256;
        int z = blockZ - this.z * 256;
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
            this.image.baleet();
        }

        this.saveData(true);
        if (this.persistentMap.getOptions().outputImages) {
            this.saveImage();
        }

    }

    private final class FillChunkRunnable implements Runnable {
        private final LevelChunk chunk;
        private final int index;

        private FillChunkRunnable(LevelChunk chunk) {
            this.chunk = chunk;
            int chunkX = chunk.getPos().x - CachedRegion.this.x * 16;
            int chunkZ = chunk.getPos().z - CachedRegion.this.z * 16;
            this.index = chunkZ * 16 + chunkX;
        }

        public void run() {
            CachedRegion.this.threadLock.lock();

            try {
                if (!CachedRegion.this.loaded) {
                    CachedRegion.this.load();
                }

                int chunkX = this.chunk.getPos().x - CachedRegion.this.x * 16;
                int chunkZ = this.chunk.getPos().z - CachedRegion.this.z * 16;
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

                for (; CachedRegion.this.dataUpdated || CachedRegion.this.displayOptionsChanged; CachedRegion.this.refreshingImage = false) {
                    CachedRegion.this.dataUpdated = false;
                    CachedRegion.this.displayOptionsChanged = false;
                    CachedRegion.this.refreshingImage = true;
                    synchronized (CachedRegion.this.image) {
                        CachedRegion.this.fillImage();
                        CachedRegion.this.imageChanged = true;
                    }
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
