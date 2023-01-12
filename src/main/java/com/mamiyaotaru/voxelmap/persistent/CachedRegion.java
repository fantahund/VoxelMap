package com.mamiyaotaru.voxelmap.persistent;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mamiyaotaru.voxelmap.SettingsAndLightingChangeNotifier;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.interfaces.IPersistentMap;
import com.mamiyaotaru.voxelmap.util.BlockStateParser;
import com.mamiyaotaru.voxelmap.util.CommandUtils;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.MutableBlockPos;
import com.mamiyaotaru.voxelmap.util.ReflectionUtils;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.thread.ThreadExecutor;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import org.apache.logging.log4j.Level;

import javax.imageio.ImageIO;
import java.awt.image.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
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
    public static final EmptyCachedRegion emptyRegion = new EmptyCachedRegion();
    private long mostRecentView = 0L;
    private long mostRecentChange = 0L;
    private IPersistentMap persistentMap;
    private String key;
    private ClientWorld world;
    private ServerWorld worldServer;
    private ServerChunkManager chunkProvider;
    Class<?> executorClass;
    private ThreadExecutor<RefreshRunnable> executor;
    private ThreadedAnvilChunkStorage chunkLoader;
    private String subworldName;
    private String worldNamePathPart;
    private String subworldNamePathPart = "";
    private String dimensionNamePathPart;
    private boolean underground = false;
    private int x;
    private int z;
    private final int width = 256;
    private boolean empty = true;
    private boolean liveChunksUpdated = false;
    boolean remoteWorld;
    private final boolean[] liveChunkUpdateQueued = new boolean[256];
    private final boolean[] chunkUpdateQueued = new boolean[256];
    private CompressibleGLBufferedImage image;
    private CompressibleMapData data;
    final MutableBlockPos blockPos = new MutableBlockPos(0, 0, 0);
    final MutableBlockPos loopBlockPos = new MutableBlockPos(0, 0, 0);
    Future<?> future = null;
    private final ReentrantLock threadLock = new ReentrantLock();
    boolean displayOptionsChanged = false;
    boolean imageChanged = false;
    boolean refreshQueued = false;
    boolean refreshingImage = false;
    boolean dataUpdated = false;
    boolean dataUpdateQueued = false;
    boolean loaded = false;
    boolean closed = false;
    private static final Object anvilLock = new Object();
    private static final ReadWriteLock tickLock = new ReentrantReadWriteLock();
    private static int loadedChunkCount = 0;
    private boolean queuedToCompress = false;
    final boolean debug = false;

    public CachedRegion() {}

    public CachedRegion(IPersistentMap persistentMap, String key, ClientWorld world, String worldName, String subworldName, int x, int z) {
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
        this.underground = !world.getDimensionEffects().shouldBrightenLighting() && !world.getDimension().hasSkyLight() || world.getDimension().hasCeiling() || knownUnderground;
        this.remoteWorld = !VoxelConstants.getMinecraft().isIntegratedServerRunning();
        persistentMap.getSettingsAndLightingChangeNotifier().addObserver(this);
        this.x = x;
        this.z = z;
        if (!this.remoteWorld) {
            Optional<World> optionalWorld = VoxelConstants.getWorldByKey(world.getRegistryKey());

            if (optionalWorld.isEmpty()) {
                String error = "Attempted to fetch World, but none was found!";

                VoxelConstants.getLogger().fatal(error);
                throw new IllegalStateException(error);
            }

            this.worldServer = (ServerWorld) optionalWorld.get();
            this.chunkProvider = worldServer.getChunkManager();
            this.executorClass = chunkProvider.getClass().getDeclaredClasses()[0];
            this.executor = (ThreadExecutor<RefreshRunnable>) ReflectionUtils.getPrivateFieldValueByType(chunkProvider, ServerChunkManager.class, executorClass);
            this.chunkLoader = chunkProvider.threadedAnvilChunkStorage;
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

    public void handleChangedChunk(WorldChunk chunk) {
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
        this.data = new CompressibleMapData(256, 256);
        this.image = new CompressibleGLBufferedImage(256, 256, 6);
        this.loadCachedData();
        this.loadCurrentData(this.world);
        if (!this.remoteWorld) {
            this.loadAnvilData(this.world);
        }

        this.loaded = true;
    }

    private void loadCurrentData(ClientWorld world) {
        for (int chunkX = 0; chunkX < 16; ++chunkX) {
            for (int chunkZ = 0; chunkZ < 16; ++chunkZ) {
                WorldChunk chunk = world.getChunk(this.x * 16 + chunkX, this.z * 16 + chunkZ);
                if (chunk != null && !chunk.isEmpty() && world.isChunkLoaded(this.x * 16 + chunkX, this.z * 16 + chunkZ) && this.isSurroundedByLoaded(chunk)) {
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
                    WorldChunk chunk = this.world.getChunk(this.x * 16 + chunkX, this.z * 16 + chunkZ);
                    if (chunk != null && !chunk.isEmpty() && this.world.isChunkLoaded(this.x * 16 + chunkX, this.z * 16 + chunkZ)) {
                        this.loadChunkData(chunk, chunkX, chunkZ);
                    }
                }
            }
        }

    }

    private void loadChunkData(WorldChunk chunk, int chunkX, int chunkZ) {
        boolean isEmpty = this.isChunkEmptyOrUnlit(chunk);
        boolean isSurroundedByLoaded = this.isSurroundedByLoaded(chunk);
        if (!this.closed && this.world == GameVariableAccessShim.getWorld() && !isEmpty && isSurroundedByLoaded) {
            this.doLoadChunkData(chunk, chunkX, chunkZ);
        }

    }

    private void loadChunkDataSkipLightCheck(WorldChunk chunk, int chunkX, int chunkZ) {
        if (!this.closed && this.world == GameVariableAccessShim.getWorld() && !this.isChunkEmpty(chunk)) {
            this.doLoadChunkData(chunk, chunkX, chunkZ);
        }

    }

    private void doLoadChunkData(WorldChunk chunk, int chunkX, int chunkZ) {
        for (int t = 0; t < 16; ++t) {
            for (int s = 0; s < 16; ++s) {
                this.persistentMap.getAndStoreData(this.data, chunk.getWorld(), chunk, this.blockPos, this.underground, this.x * 256, this.z * 256, chunkX * 16 + t, chunkZ * 16 + s);
            }
        }

        this.empty = false;
        this.liveChunksUpdated = true;
        this.dataUpdated = true;
    }

    private boolean isChunkEmptyOrUnlit(WorldChunk chunk) {
        return this.closed || chunk.isEmpty() || !chunk.getStatus().isAtLeast(ChunkStatus.FULL);
    }

    private boolean isChunkEmpty(WorldChunk chunk) {
        return this.closed || chunk.isEmpty() || !chunk.getStatus().isAtLeast(ChunkStatus.FULL);
    }

    public boolean isSurroundedByLoaded(WorldChunk chunk) {
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        boolean neighborsLoaded = !chunk.isEmpty() && VoxelConstants.getMinecraft().world.isChunkLoaded(chunkX, chunkZ);

        for (int t = chunkX - 1; t <= chunkX + 1 && neighborsLoaded; ++t) {
            for (int s = chunkZ - 1; s <= chunkZ + 1 && neighborsLoaded; ++s) {
                WorldChunk neighborChunk = VoxelConstants.getMinecraft().world.getChunk(t, s);
                neighborsLoaded = neighborChunk != null && !neighborChunk.isEmpty() && VoxelConstants.getMinecraft().world.isChunkLoaded(t, s);
            }
        }

        return neighborsLoaded;
    }

    private void loadAnvilData(World world) {
        if (!this.remoteWorld) {
            boolean full = true;

            for (int t = 0; t < 16; ++t) {
                for (int s = 0; s < 16; ++s) {
                    if (!this.closed && this.data.getHeight(t * 16, s * 16) == 0 && this.data.getLight(t * 16, s * 16) == 0) {
                        full = false;
                    }
                }
            }

            if (!this.closed && !full) {
                File directory = new File(DimensionType.getSaveDirectory(this.worldServer.getRegistryKey(), this.worldServer.getServer().getSavePath(WorldSavePath.ROOT).normalize()).toString(), "region");
                File regionFile = new File(directory, "r." + (int) Math.floor(this.x / 2f) + "." + (int) Math.floor(this.z / 2f) + ".mca");
                if (regionFile.exists()) {
                    boolean dataChanged = false;
                    boolean loadedChunks = false;
                    Chunk[] chunks = new Chunk[256];
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
                                        if (!this.closed && this.data.getHeight(tx * 16, sx * 16) == 0 && this.data.getLight(tx * 16, sx * 16) == 0) {
                                            int index = tx + sx * 16;
                                            ChunkPos chunkPos = new ChunkPos(this.x * 16 + tx, this.z * 16 + sx);
                                            NbtCompound rawNbt = this.chunkLoader.getNbt(chunkPos).join().get();
                                            NbtCompound nbt = this.chunkLoader.updateChunkNbt(this.worldServer.getRegistryKey(), () -> this.worldServer.getPersistentStateManager(), rawNbt, Optional.empty());
                                            if (!this.closed && nbt.contains("Level", 10)) {
                                                NbtCompound level = nbt.getCompound("Level");
                                                int chunkX = level.getInt("xPos");
                                                int chunkZ = level.getInt("zPos");
                                                if (chunkPos.x == chunkX && chunkPos.z == chunkZ && level.contains("Status", 8) && ChunkStatus.byId(level.getString("Status")).isAtLeast(ChunkStatus.SPAWN) && level.contains("Sections")) {
                                                    NbtList sections = level.getList("Sections", 10);
                                                    if (!sections.isEmpty()) {
                                                        boolean hasInfo = false;

                                                        for (int i = 0; i < sections.size() && !hasInfo && !this.closed; ++i) {
                                                            NbtCompound section = sections.getCompound(i);
                                                            if (section.contains("Palette", 9) && section.contains("BlockStates", 12)) {
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
                                    WorldChunk loadedChunk = null;
                                    if (chunks[index] instanceof WorldChunk) {
                                        loadedChunk = (WorldChunk) chunks[index];
                                    } else {
                                        VoxelConstants.getLogger().warn("non world chunk at " + chunks[index].getPos().x + "," + chunks[index].getPos().z);
                                    }

                                    if (!this.closed && loadedChunk != null && loadedChunk.getStatus().isAtLeast(ChunkStatus.FULL)) {
                                        CompletableFuture<Chunk> lightFuture = this.chunkProvider.getLightingProvider().light(loadedChunk, false);

                                        while (!this.closed && !lightFuture.isDone()) {
                                            Thread.onSpinWait();
                                        }

                                        loadedChunk = (WorldChunk) lightFuture.getNow(loadedChunk);
                                        lightFuture.cancel(false);
                                    }

                                    if (!this.closed && loadedChunk != null && loadedChunk.getStatus().isAtLeast(ChunkStatus.FULL)) {
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
                            CompletableFuture<Void> tickFuture = CompletableFuture.runAsync(() -> this.chunkProvider.tick(() -> true, executor.isOnThread()));
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
                        } catch (Exception var38) {
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
            File cachedRegionFileDir = new File(VoxelConstants.getMinecraft().runDirectory, "/voxelmap/cache/" + this.worldNamePathPart + "/" + this.subworldNamePathPart + this.dimensionNamePathPart);
            cachedRegionFileDir.mkdirs();
            File cachedRegionFile = new File(cachedRegionFileDir, "/" + this.key + ".zip");
            if (cachedRegionFile.exists()) {
                ZipFile zFile = new ZipFile(cachedRegionFile);
                int total = 0;
                byte[] decompressedByteData = new byte[this.data.getWidth() * this.data.getHeight() * 17 * 4];
                ZipEntry ze = zFile.getEntry("data");
                InputStream is = zFile.getInputStream(ze);

                int count;
                for (byte[] byteData = new byte[2048]; (count = is.read(byteData, 0, 2048)) != -1 && count + total <= this.data.getWidth() * this.data.getHeight() * 17 * 4; total += count) {
                    System.arraycopy(byteData, 0, decompressedByteData, total, count);
                }

                is.close();
                ze = zFile.getEntry("key");
                is = zFile.getInputStream(ze);
                BiMap<BlockState, Integer> var18 = HashBiMap.create();
                Scanner sc = new Scanner(is);

                while (sc.hasNextLine()) {
                    BlockStateParser.parseLine(sc.nextLine(), var18);
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
                if (total == this.data.getWidth() * this.data.getHeight() * 18) {
                    byte[] var23 = new byte[this.data.getWidth() * this.data.getHeight() * 18];
                    System.arraycopy(decompressedByteData, 0, var23, 0, var23.length);
                    this.data.setData(var23, var18, version);
                    this.empty = false;
                    this.dataUpdated = true;
                } else {
                    VoxelConstants.getLogger().warn("failed to load data from " + cachedRegionFile.getPath());
                }

                if (version < 2) {
                    this.liveChunksUpdated = true;
                }
            }
        } catch (Exception var17) {
            VoxelConstants.getLogger().error("Failed to load region file for " + this.x + "," + this.z + " in " + this.worldNamePathPart + "/" + this.subworldNamePathPart + this.dimensionNamePathPart, var17);
        }

    }

    private void saveData(boolean newThread) {
        if (this.liveChunksUpdated && !this.worldNamePathPart.equals("")) {
            if (newThread) {
                ThreadManager.executorService.execute(() -> {
                    CachedRegion.this.threadLock.lock();

                    try {
                        CachedRegion.this.doSave();
                    } catch (IOException var5) {
                        VoxelConstants.getLogger().error("Failed to save region file for " + CachedRegion.this.x + "," + CachedRegion.this.z + " in " + CachedRegion.this.worldNamePathPart + "/" + CachedRegion.this.subworldNamePathPart + CachedRegion.this.dimensionNamePathPart, var5);
                    } finally {
                        CachedRegion.this.threadLock.unlock();
                    }

                });
            } else {
                try {
                    this.doSave();
                } catch (IOException var3) {
                    VoxelConstants.getLogger().error(var3);
                }
            }

            this.liveChunksUpdated = false;
        }

    }

    private void doSave() throws IOException {
        BiMap<BlockState, Integer> stateToInt = this.data.getStateToInt();
        byte[] byteArray = this.data.getData();
        int var10000 = byteArray.length;
        int var10001 = this.data.getWidth() * this.data.getHeight();
        if (var10000 == var10001 * 18) {
            File cachedRegionFileDir = new File(VoxelConstants.getMinecraft().runDirectory, "/voxelmap/cache/" + this.worldNamePathPart + "/" + this.subworldNamePathPart + this.dimensionNamePathPart);
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
                Iterator<Entry<BlockState, Integer>> iterator = stateToInt.entrySet().iterator();
                StringBuilder stringBuffer = new StringBuilder();

                while (iterator.hasNext()) {
                    Entry<BlockState, Integer> entry = iterator.next();
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

            String nextLine = "version:2\r\n";
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
        int color24;

        for (int t = 0; t < 256; ++t) {
            for (int s = 0; s < 256; ++s) {
                color24 = this.persistentMap.getPixelColor(this.data, this.world, this.blockPos, this.loopBlockPos, this.underground, 8, this.x * 256, this.z * 256, t, s);
                this.image.setRGB(t, s, color24);
            }
        }

    }

    private void saveImage() {
        if (!this.empty) {
            File imageFileDir = new File(VoxelConstants.getMinecraft().runDirectory, "/voxelmap/cache/" + this.worldNamePathPart + "/" + this.subworldNamePathPart + this.dimensionNamePathPart + "/images/z1");
            imageFileDir.mkdirs();
            final File imageFile = new File(imageFileDir, this.key + ".png");
            if (this.liveChunksUpdated || !imageFile.exists()) {
                ThreadManager.executorService.execute(() -> {
                    CachedRegion.this.threadLock.lock();

                    try {
                        BufferedImage realBufferedImage = new BufferedImage(CachedRegion.this.width, CachedRegion.this.width, 6);
                        byte[] dstArray = ((DataBufferByte) realBufferedImage.getRaster().getDataBuffer()).getData();
                        System.arraycopy(CachedRegion.this.image.getData(), 0, dstArray, 0, CachedRegion.this.image.getData().length);
                        ImageIO.write(realBufferedImage, "png", imageFile);
                    } catch (IOException var6) {
                        VoxelConstants.getLogger().error(var6);
                    } finally {
                        CachedRegion.this.threadLock.unlock();
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
        int y = this.data == null ? 0 : this.data.getHeight(x, z);
        if (this.underground && y == 255) {
            y = CommandUtils.getSafeHeight(blockX, 64, blockZ, this.world);
        }

        return y;
    }

    public void compress() {
        if (this.data != null && !this.isCompressed() && !this.queuedToCompress) {
            this.queuedToCompress = true;
            ThreadManager.executorService.execute(() -> {
                if (CachedRegion.this.threadLock.tryLock()) {
                    try {
                        CachedRegion.this.compressData();
                    } catch (Exception ignored) {
                    } finally {
                        CachedRegion.this.threadLock.unlock();
                    }
                }

                CachedRegion.this.queuedToCompress = false;
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
        private final WorldChunk chunk;
        private final int index;

        private FillChunkRunnable(WorldChunk chunk) {
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
            CachedRegion.this.threadLock.lock();
            CachedRegion.this.mostRecentChange = System.currentTimeMillis();

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
