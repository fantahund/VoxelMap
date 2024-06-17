package com.mamiyaotaru.voxelmap.persistent;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mamiyaotaru.voxelmap.interfaces.AbstractMapData;
import com.mamiyaotaru.voxelmap.util.CompressionUtils;
import net.minecraft.block.BlockState;
import net.minecraft.world.biome.Biome;
import java.util.Arrays;
import java.util.zip.DataFormatException;

public class CompressibleMapData extends AbstractMapData {
    public final static int DATA_VERSION = 4;
    public final static int LAYERS = 22;

    private static final int HEIGHTPOS = 0;
    private static final int BLOCKSTATEPOS = 2;
    private static final int LIGHTPOS = 4;

    private static final int OCEANFLOORHEIGHTPOS = 5;
    private static final int OCEANFLOORBLOCKSTATEPOS = 7;
    private static final int OCEANFLOORLIGHTPOS = 9;

    private static final int TRANSPARENTHEIGHTPOS = 10;
    private static final int TRANSPARENTBLOCKSTATEPOS = 12;
    private static final int TRANSPARENTLIGHTPOS = 14;

    private static final int FOLIAGEHEIGHTPOS = 15;
    private static final int FOLIAGEBLOCKSTATEPOS = 17;
    private static final int FOLIAGELIGHTPOS = 19;

    private static final int BIOMEIDPOS = 20;

    private final static int REGION_SIZE = 256;
    private final static byte[] compressedEmptyData = CompressionUtils.compress(generateEmptyData());

    private byte[] data;
    private boolean isCompressed;
    private BiMap<BlockState, Integer> blockStateToInt;
    int blockStateCount = 1;
    private BiMap<Biome, Integer> biomeToInt;
    int biomeCount = 1;

    public CompressibleMapData() {
        this.width = REGION_SIZE;
        this.height = REGION_SIZE;
        this.data = compressedEmptyData;
        this.isCompressed = true;
    }

    private static byte[] generateEmptyData() {
        byte[] data = new byte[REGION_SIZE * REGION_SIZE * LAYERS];
        // set height to Short.MIN_VALUE for all
        int value = Short.MIN_VALUE;
        byte b0 = (byte) (value >> 8);
        byte b1 = (byte) value;
        Arrays.fill(data, 0, REGION_SIZE * REGION_SIZE, b0);
        Arrays.fill(data, REGION_SIZE * REGION_SIZE, REGION_SIZE * REGION_SIZE * 2, b1);
        return data;
    }

    @Override
    public int getHeight(int x, int z) {
        return this.getDataSignedShort(x, z, HEIGHTPOS);
    }

    @Override
    public BlockState getBlockstate(int x, int z) {
        int id = this.getDataUnsignedShort(x, z, BLOCKSTATEPOS);
        return this.getStateFromID(id);
    }

    @Override
    public int getBiomeTint(int x, int z) {
        return 0;
    }

    @Override
    public int getLight(int x, int z) {
        return this.getData(x, z, LIGHTPOS) & 0xFF;
    }

    @Override
    public int getOceanFloorHeight(int x, int z) {
        return this.getDataSignedShort(x, z, OCEANFLOORHEIGHTPOS);
    }

    @Override
    public BlockState getOceanFloorBlockstate(int x, int z) {
        int id = this.getDataUnsignedShort(x, z, OCEANFLOORBLOCKSTATEPOS);
        return this.getStateFromID(id);
    }

    @Override
    public int getOceanFloorBiomeTint(int x, int z) {
        return 0;
    }

    @Override
    public int getOceanFloorLight(int x, int z) {
        return this.getData(x, z, OCEANFLOORLIGHTPOS) & 0xFF;
    }

    @Override
    public int getTransparentHeight(int x, int z) {
        return this.getDataSignedShort(x, z, TRANSPARENTHEIGHTPOS);
    }

    @Override
    public BlockState getTransparentBlockstate(int x, int z) {
        int id = this.getDataUnsignedShort(x, z, TRANSPARENTBLOCKSTATEPOS);
        return this.getStateFromID(id);
    }

    @Override
    public int getTransparentBiomeTint(int x, int z) {
        return 0;
    }

    @Override
    public int getTransparentLight(int x, int z) {
        return this.getData(x, z, TRANSPARENTLIGHTPOS) & 0xFF;
    }

    @Override
    public int getFoliageHeight(int x, int z) {
        return this.getDataSignedShort(x, z, FOLIAGEHEIGHTPOS);
    }

    @Override
    public BlockState getFoliageBlockstate(int x, int z) {
        int id = this.getDataUnsignedShort(x, z, FOLIAGEBLOCKSTATEPOS);
        return this.getStateFromID(id);
    }

    @Override
    public int getFoliageBiomeTint(int x, int z) {
        return 0;
    }

    @Override
    public int getFoliageLight(int x, int z) {
        return this.getData(x, z, FOLIAGELIGHTPOS) & 0xFF;
    }

    @Override
    public Biome getBiome(int x, int z) {
        if (this.isCompressed) {
            this.decompress();
        }
        return this.getBiomeFromID(this.getDataUnsignedShort(x, z, BIOMEIDPOS));
    }

    private synchronized byte getData(int x, int z, int layer) {
        if (this.isCompressed) {
            this.decompress();
        }

        int index = x + z * this.width + this.width * this.height * layer;
        return this.data[index];
    }

    private synchronized int getDataUnsignedShort(int x, int z, int layer) {
        return ((this.getData(x, z, layer) & 0xFF) << 8) | (this.getData(x, z, layer + 1) & 0xFF);
    }

    private synchronized int getDataSignedShort(int x, int z, int layer) {
        return (this.getData(x, z, layer) << 8) | (this.getData(x, z, layer + 1) & 0xFF);
    }

    @Override
    public void setHeight(int x, int z, int height) {
        this.setDataShort(x, z, HEIGHTPOS, height);
    }

    @Override
    public void setBlockstate(int x, int z, BlockState state) {
        int id = this.getIDFromState(state);
        this.setDataShort(x, z, BLOCKSTATEPOS, id);
    }

    @Override
    public void setBiomeTint(int x, int z, int tint) {
    }

    @Override
    public void setLight(int x, int z, int light) {
        this.setData(x, z, LIGHTPOS, (byte) light);
    }

    @Override
    public void setOceanFloorHeight(int x, int z, int height) {
        this.setDataShort(x, z, OCEANFLOORHEIGHTPOS, height);
    }

    @Override
    public void setOceanFloorBlockstate(int x, int z, BlockState state) {
        int id = this.getIDFromState(state);
        this.setDataShort(x, z, OCEANFLOORBLOCKSTATEPOS, id);
    }

    @Override
    public void setOceanFloorBiomeTint(int x, int z, int tint) {
    }

    @Override
    public void setOceanFloorLight(int x, int z, int light) {
        this.setData(x, z, OCEANFLOORLIGHTPOS, (byte) light);
    }

    @Override
    public void setTransparentHeight(int x, int z, int height) {
        this.setDataShort(x, z, TRANSPARENTHEIGHTPOS, height);
    }

    @Override
    public void setTransparentBlockstate(int x, int z, BlockState state) {
        int id = this.getIDFromState(state);
        this.setDataShort(x, z, TRANSPARENTBLOCKSTATEPOS, id);
    }

    @Override
    public void setTransparentBiomeTint(int x, int z, int tint) {
    }

    @Override
    public void setTransparentLight(int x, int z, int light) {
        this.setData(x, z, TRANSPARENTLIGHTPOS, (byte) light);
    }

    @Override
    public void setFoliageHeight(int x, int z, int height) {
        this.setDataShort(x, z, FOLIAGEHEIGHTPOS, height);
    }

    @Override
    public void setFoliageBlockstate(int x, int z, BlockState state) {
        int id = this.getIDFromState(state);
        this.setDataShort(x, z, FOLIAGEBLOCKSTATEPOS, id);
    }

    @Override
    public void setFoliageBiomeTint(int x, int z, int tint) {
    }

    @Override
    public void setFoliageLight(int x, int z, int light) {
        this.setData(x, z, FOLIAGELIGHTPOS, (byte) light);
    }

    @Override
    public void setBiome(int x, int z, Biome biome) {
        if (this.isCompressed) {
            this.decompress();
        }
        int id = this.getIDFromBiome(biome);
        this.setDataShort(x, z, BIOMEIDPOS, id);
    }

    private synchronized void setData(int x, int z, int layer, byte value) {
        if (this.isCompressed) {
            this.decompress();
        }

        int index = x + z * this.width + this.width * this.height * layer;
        this.data[index] = value;
    }

    private synchronized void setDataShort(int x, int z, int layer, int value) {
        this.setData(x, z, layer, (byte) (value >> 8));
        this.setData(x, z, layer + 1, (byte) value);
    }

    @Override
    public synchronized void moveX(int x) {
        if (this.isCompressed) {
            this.decompress();
        }
        if (x > 0) {
            System.arraycopy(this.data, x * LAYERS, this.data, 0, this.data.length - x * LAYERS);
        } else if (x < 0) {
            System.arraycopy(this.data, 0, this.data, -x * LAYERS, this.data.length + x * LAYERS);
        }
    }

    @Override
    public synchronized void moveZ(int z) {
        if (this.isCompressed) {
            this.decompress();
        }
        if (z > 0) {
            System.arraycopy(this.data, z * this.width * LAYERS, this.data, 0, this.data.length - z * this.width * LAYERS);
        } else if (z < 0) {
            System.arraycopy(this.data, 0, this.data, -z * this.width * LAYERS, this.data.length + z * this.width * LAYERS);
        }
    }

    public synchronized void setData(byte[] is, BiMap<BlockState, Integer> newStateToInt, BiMap<Biome, Integer> newBiomeToInt, int version) {
        this.data = is;
        this.isCompressed = false;
        if (version < DATA_VERSION) {
            this.convertData(version);
        }

        this.blockStateToInt = newStateToInt;
        this.blockStateCount = this.blockStateToInt.size();
        this.biomeToInt = newBiomeToInt;
        this.biomeCount = this.biomeToInt.size();
    }

    private synchronized void convertData(int version) {
        if (this.isCompressed) {
            this.decompress();
        }
        if (version < 2) {
            final int OLD_LAYERS = 18;
            byte[] newData = new byte[this.data.length];

            for (int x = 0; x < this.width; ++x) {
                for (int z = 0; z < this.height; ++z) {
                    for (int layer = 0; layer < OLD_LAYERS; ++layer) {
                        int oldIndex = (x + z * this.width) * OLD_LAYERS + layer;
                        int newIndex = x + z * this.width + this.width * this.height * layer;
                        newData[newIndex] = this.data[oldIndex];
                    }
                }
            }
        }
        if (version < 4) {
            final int OLD_HEIGHTPOS = 0;
            final int OLD_BLOCKSTATEPOS = 1;
            final int OLD_LIGHTPOS = 3;

            final int OLD_OCEANFLOORHEIGHTPOS = 4;
            final int OLD_OCEANFLOORBLOCKSTATEPOS = 5;
            final int OLD_OCEANFLOORLIGHTPOS = 7;

            final int OLD_TRANSPARENTHEIGHTPOS = 8;
            final int OLD_TRANSPARENTBLOCKSTATEPOS = 9;
            final int OLD_TRANSPARENTLIGHTPOS = 11;

            final int OLD_FOLIAGEHEIGHTPOS = 12;
            final int OLD_FOLIAGEBLOCKSTATEPOS = 13;
            final int OLD_FOLIAGELIGHTPOS = 15;

            final int OLD_BIOMEIDPOS = 16;

            byte[] newData = new byte[this.width * this.height * LAYERS];

            copyUnsignedByteLayerToShortLayer(this.data, newData, OLD_HEIGHTPOS, HEIGHTPOS);
            copyShortLayer(this.data, newData, OLD_BLOCKSTATEPOS, BLOCKSTATEPOS);
            copyByteLayer(this.data, newData, OLD_LIGHTPOS, LIGHTPOS);

            copyUnsignedByteLayerToShortLayer(this.data, newData, OLD_OCEANFLOORHEIGHTPOS, OCEANFLOORHEIGHTPOS);
            copyShortLayer(this.data, newData, OLD_OCEANFLOORBLOCKSTATEPOS, OCEANFLOORBLOCKSTATEPOS);
            copyByteLayer(this.data, newData, OLD_OCEANFLOORLIGHTPOS, OCEANFLOORLIGHTPOS);

            copyUnsignedByteLayerToShortLayer(this.data, newData, OLD_TRANSPARENTHEIGHTPOS, TRANSPARENTHEIGHTPOS);
            copyShortLayer(this.data, newData, OLD_TRANSPARENTBLOCKSTATEPOS, TRANSPARENTBLOCKSTATEPOS);
            copyByteLayer(this.data, newData, OLD_TRANSPARENTLIGHTPOS, TRANSPARENTLIGHTPOS);

            copyUnsignedByteLayerToShortLayer(this.data, newData, OLD_FOLIAGEHEIGHTPOS, FOLIAGEHEIGHTPOS);
            copyShortLayer(this.data, newData, OLD_FOLIAGEBLOCKSTATEPOS, FOLIAGEBLOCKSTATEPOS);
            copyByteLayer(this.data, newData, OLD_FOLIAGELIGHTPOS, FOLIAGELIGHTPOS);

            copyShortLayer(this.data, newData, OLD_BIOMEIDPOS, BIOMEIDPOS);

            this.data = newData;
            // set new "unknown" height
            for (int z = 0; z < width; z++) {
                for (int x = 0; x < height; x++) {
                    if (getHeight(x, z) == 0) {
                        setHeight(x, z, Short.MIN_VALUE);
                    }
                }
            }
        }
    }

    private void copyUnsignedByteLayerToShortLayer(byte[] oldData, byte[] newData, int oldLayer, int newLayer) {
        copyByteLayer(oldData, newData, oldLayer, newLayer + 1); // newLayer + 0 will stay empty
    }

    private void copyByteLayer(byte[] oldData, byte[] newData, int oldLayer, int newLayer) {
        int start = this.width * this.height * oldLayer;
        int newStart = this.width * this.height * newLayer;
        int length = this.width * this.height;
        System.arraycopy(oldData, start, newData, newStart, length);
    }

    private void copyShortLayer(byte[] oldData, byte[] newData, int oldLayer, int newLayer) {
        int start = this.width * this.height * oldLayer;
        int newStart = this.width * this.height * newLayer;
        int length = this.width * this.height * 2;
        System.arraycopy(oldData, start, newData, newStart, length);
    }

    public synchronized byte[] getData() {
        if (this.isCompressed) {
            this.decompress();
        }

        return this.data;
    }

    public synchronized void compress() {
        if (!this.isCompressed) {
            this.isCompressed = true;
            this.data = CompressionUtils.compress(this.data);
        }
    }

    private synchronized void decompress() {
        if (this.blockStateToInt == null) {
            this.blockStateToInt = HashBiMap.create();
        }
        if (this.biomeToInt == null) {
            this.biomeToInt = HashBiMap.create();
        }

        if (this.isCompressed) {
            try {
                this.data = CompressionUtils.decompress(this.data);
                this.isCompressed = false;
            } catch (DataFormatException ignored) {
            }
        }
    }

    public synchronized boolean isCompressed() {
        return this.isCompressed;
    }

    private synchronized int getIDFromState(BlockState blockState) {
        Integer id = this.blockStateToInt.get(blockState);
        if (id == null && blockState != null) {
            while (this.blockStateToInt.inverse().containsKey(this.blockStateCount)) {
                ++this.blockStateCount;
            }

            id = this.blockStateCount;
            this.blockStateToInt.put(blockState, id);
        }

        return id;
    }

    private BlockState getStateFromID(int id) {
        return this.blockStateToInt.inverse().get(id);
    }

    public BiMap<BlockState, Integer> getStateToInt() {
        this.blockStateToInt = this.createKeyFromCurrentBlocks(this.blockStateToInt);
        return this.blockStateToInt;
    }

    private BiMap<BlockState, Integer> createKeyFromCurrentBlocks(BiMap<BlockState, Integer> oldMap) {
        this.blockStateCount = 1;
        BiMap<BlockState, Integer> newMap = HashBiMap.create();

        for (int x = 0; x < this.width; ++x) {
            for (int z = 0; z < this.height; ++z) {
                int oldID = (this.getData(x, z, BLOCKSTATEPOS) & 255) << 8 | this.getData(x, z, BLOCKSTATEPOS + 1) & 255;
                if (oldID != 0) {
                    BlockState blockState = oldMap.inverse().get(oldID);
                    Integer id = newMap.get(blockState);
                    if (id == null && blockState != null) {
                        while (newMap.inverse().containsKey(this.blockStateCount)) {
                            ++this.blockStateCount;
                        }

                        id = this.blockStateCount;
                        newMap.put(blockState, id);
                    }

                    this.setData(x, z, BLOCKSTATEPOS, (byte) (id >> 8));
                    this.setData(x, z, BLOCKSTATEPOS + 1, (byte) id.intValue());
                }

                oldID = (this.getData(x, z, OCEANFLOORBLOCKSTATEPOS) & 255) << 8 | this.getData(x, z, OCEANFLOORBLOCKSTATEPOS + 1) & 255;
                if (oldID != 0) {
                    BlockState blockState = oldMap.inverse().get(oldID);
                    Integer id = newMap.get(blockState);
                    if (id == null && blockState != null) {
                        while (newMap.inverse().containsKey(this.blockStateCount)) {
                            ++this.blockStateCount;
                        }

                        id = this.blockStateCount;
                        newMap.put(blockState, id);
                    }

                    this.setData(x, z, OCEANFLOORBLOCKSTATEPOS, (byte) (id >> 8));
                    this.setData(x, z, OCEANFLOORBLOCKSTATEPOS + 1, (byte) id.intValue());
                }

                oldID = (this.getData(x, z, TRANSPARENTBLOCKSTATEPOS) & 255) << 8 | this.getData(x, z, TRANSPARENTBLOCKSTATEPOS + 1) & 255;
                if (oldID != 0) {
                    BlockState blockState = oldMap.inverse().get(oldID);
                    Integer id = newMap.get(blockState);
                    if (id == null && blockState != null) {
                        while (newMap.inverse().containsKey(this.blockStateCount)) {
                            ++this.blockStateCount;
                        }

                        id = this.blockStateCount;
                        newMap.put(blockState, id);
                    }

                    this.setData(x, z, TRANSPARENTBLOCKSTATEPOS, (byte) (id >> 8));
                    this.setData(x, z, TRANSPARENTBLOCKSTATEPOS + 1, (byte) id.intValue());
                }

                oldID = (this.getData(x, z, FOLIAGEBLOCKSTATEPOS) & 255) << 8 | this.getData(x, z, FOLIAGEBLOCKSTATEPOS + 1) & 255;
                if (oldID != 0) {
                    BlockState blockState = oldMap.inverse().get(oldID);
                    Integer id = newMap.get(blockState);
                    if (id == null && blockState != null) {
                        while (newMap.inverse().containsKey(this.blockStateCount)) {
                            ++this.blockStateCount;
                        }

                        id = this.blockStateCount;
                        newMap.put(blockState, id);
                    }

                    this.setData(x, z, FOLIAGEBLOCKSTATEPOS, (byte) (id >> 8));
                    this.setData(x, z, FOLIAGEBLOCKSTATEPOS + 1, (byte) id.intValue());
                }
            }
        }

        return newMap;
    }

    private synchronized int getIDFromBiome(Biome biome) {
        if (biome == null) {
            return 0;
        }
        Integer id = this.biomeToInt.get(biome);
        if (id == null && biome != null) {
            while (this.biomeToInt.inverse().containsKey(this.biomeCount)) {
                ++this.biomeCount;
            }

            id = this.biomeCount;
            this.biomeToInt.put(biome, id);
        }

        return id;
    }

    private Biome getBiomeFromID(int id) {
        return id == 0 ? null : this.biomeToInt.inverse().get(id);
    }

    public BiMap<Biome, Integer> getBiomeToInt() {
        this.biomeToInt = this.createKeyFromCurrentBiomes(this.biomeToInt);
        return this.biomeToInt;
    }

    private BiMap<Biome, Integer> createKeyFromCurrentBiomes(BiMap<Biome, Integer> oldMap) {
        this.biomeCount = 1;
        BiMap<Biome, Integer> newMap = HashBiMap.create();

        for (int x = 0; x < this.width; ++x) {
            for (int z = 0; z < this.height; ++z) {
                int oldID = (this.getData(x, z, BIOMEIDPOS) & 255) << 8 | this.getData(x, z, BIOMEIDPOS + 1) & 255;
                if (oldID != 0) {
                    Biome biome = oldMap.inverse().get(oldID);
                    Integer id = newMap.get(biome);
                    if (id == null && biome != null) {
                        while (newMap.inverse().containsKey(this.biomeCount)) {
                            ++this.biomeCount;
                        }

                        id = this.biomeCount;
                        newMap.put(biome, id);
                    }

                    this.setData(x, z, BIOMEIDPOS, (byte) (id >> 8));
                    this.setData(x, z, BIOMEIDPOS + 1, (byte) id.intValue());
                }
            }
        }

        return newMap;
    }

    public int getExpectedDataLength(int version) {
        final int OLD_LAYERS_BEFORE_V4 = 18;
        return getWidth() * getHeight() * (version < 4 ? OLD_LAYERS_BEFORE_V4 : LAYERS);
    }
}
