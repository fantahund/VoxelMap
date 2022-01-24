package com.mamiyaotaru.voxelmap.persistent;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mamiyaotaru.voxelmap.interfaces.AbstractMapData;
import com.mamiyaotaru.voxelmap.util.CompressionUtils;
import net.minecraft.block.BlockState;

import java.io.IOException;
import java.util.Arrays;
import java.util.zip.DataFormatException;

public class CompressibleMapData extends AbstractMapData {
    // assign successive byte indices
    private static int byteIdx = 0;

    private static int nextByteIdx() {
        int n = byteIdx;
        byteIdx += 1;
        return n;
    }

    private static int nextShortIdx() {
        int n = byteIdx;
        byteIdx += 2;
        return n;
    }

    private static final int HEIGHT_POS = nextShortIdx();
    private static final int BLOCKSTATE_POS = nextShortIdx();
    private static final int LIGHT_POS = nextByteIdx();
    private static final int OCEANFLOOR_HEIGHT_POS = nextShortIdx();
    private static final int OCEANFLOOR_BLOCKSTATE_POS = nextShortIdx();
    private static final int OCEANFLOOR_LIGHT_POS = nextByteIdx();
    private static final int TRANSPARENT_HEIGHT_POS = nextShortIdx();
    private static final int TRANSPARENT_BLOCKSTATE_POS = nextShortIdx();
    private static final int TRANSPARENT_LIGHT_POS = nextByteIdx();
    private static final int FOLIAGE_HEIGHT_POS = nextShortIdx();
    private static final int FOLIAGE_BLOCKSTATE_POS = nextShortIdx();
    private static final int FOLIAGE_LIGHT_POS = nextByteIdx();
    private static final int BIOMEID_POS = nextShortIdx();
    public static final int DATABYTES = byteIdx;
    public static final int VERSION = 3;
    private byte[] data;
    private boolean isCompressed = false;
    private BiMap<BlockState, Integer> stateToInt = null;
    int blockStateCount = 1;

    private static byte[] compressedEmptyData = new byte[DATABYTES * 256 * 256];

    static {
        Arrays.fill(compressedEmptyData, (byte) 0);

        try {
            compressedEmptyData = CompressionUtils.compress(compressedEmptyData);
        } catch (IOException var1) {
            var1.printStackTrace();
        }

    }

    public CompressibleMapData(int width, int height) {
        this.width = width;
        this.height = height;
        // check against compressedEmptyData size; otherwise would read past its bounds
        if (this.width * this.height > 256 * 256) throw new IllegalArgumentException("width*height must be <= 256*256");
        this.data = compressedEmptyData;
        this.isCompressed = true;
    }

    @Override
    public int getHeight(int x, int z) {
        return getSignedShort(x, z, HEIGHT_POS);
    }

    @Override
    public BlockState getBlockstate(int x, int z) {
        int id = getUnsignedShort(x, z, BLOCKSTATE_POS);
        return this.getStateFromID(id);
    }

    @Override
    public int getBiomeTint(int x, int z) {
        return 0;
    }

    @Override
    public int getLight(int x, int z) {
        return getUnsignedByte(x, z, LIGHT_POS);
    }

    @Override
    public int getOceanFloorHeight(int x, int z) {
        return getSignedShort(x, z, OCEANFLOOR_HEIGHT_POS);
    }

    @Override
    public BlockState getOceanFloorBlockstate(int x, int z) {
        return this.getStateFromID(getUnsignedShort(x, z, OCEANFLOOR_BLOCKSTATE_POS));
    }

    @Override
    public int getOceanFloorBiomeTint(int x, int z) {
        return 0;
    }

    @Override
    public int getOceanFloorLight(int x, int z) {
        return getUnsignedByte(x, z, OCEANFLOOR_LIGHT_POS);
    }

    @Override
    public int getTransparentHeight(int x, int z) {
        return getSignedShort(x, z, TRANSPARENT_HEIGHT_POS);
    }

    @Override
    public BlockState getTransparentBlockstate(int x, int z) {
        return this.getStateFromID(getUnsignedShort(x, z, TRANSPARENT_BLOCKSTATE_POS));
    }

    @Override
    public int getTransparentBiomeTint(int x, int z) {
        return 0;
    }

    @Override
    public int getTransparentLight(int x, int z) {
        return getUnsignedByte(x, z, TRANSPARENT_LIGHT_POS);
    }

    @Override
    public int getFoliageHeight(int x, int z) {
        return getSignedShort(x, z, FOLIAGE_HEIGHT_POS);
    }

    @Override
    public BlockState getFoliageBlockstate(int x, int z) {
        return this.getStateFromID(getUnsignedShort(x, z, FOLIAGE_BLOCKSTATE_POS));
    }

    @Override
    public int getFoliageBiomeTint(int x, int z) {
        return 0;
    }

    @Override
    public int getFoliageLight(int x, int z) {
        return getUnsignedByte(x, z, FOLIAGE_LIGHT_POS);
    }

    @Override
    public int getBiomeID(int x, int z) {
        return getUnsignedShort(x, z, BIOMEID_POS);
    }

    private int getUnsignedByte(int x, int z, int bit) {
        return this.getData(x, z, bit) & 0xFF;
    }

    private int getUnsignedShort(int x, int z, int bit) {
        int high = this.getData(x, z, bit) & 0xFF;
        int low = this.getData(x, z, bit + 1) & 0xFF;
        return high << 8 | low;
    }

    private int getSignedShort(int x, int z, int bit) {
        return getUnsignedShort(x, z, bit) << 16 >>> 16;
    }

    private synchronized byte getData(int x, int z, int bit) {
        if (this.isCompressed) {
            this.decompress();
        }

        int index = x + z * this.width + this.width * this.height * bit;
        return this.data[index];
    }

    @Override
    public void setHeight(int x, int z, int value) {
        setShort(x, z, HEIGHT_POS, value);
    }

    @Override
    public void setBlockstate(int x, int z, BlockState blockState) {
        int id = this.getIDFromState(blockState);
        setShort(x, z, BLOCKSTATE_POS, id);
    }

    @Override
    public void setBiomeTint(int x, int z, int value) {
    }

    @Override
    public void setLight(int x, int z, int value) {
        setByte(x, z, LIGHT_POS, value);
    }

    @Override
    public void setOceanFloorHeight(int x, int z, int value) {
        setShort(x, z, OCEANFLOOR_HEIGHT_POS, value);
    }

    @Override
    public void setOceanFloorBlockstate(int x, int z, BlockState blockState) {
        int id = this.getIDFromState(blockState);
        setShort(x, z, OCEANFLOOR_BLOCKSTATE_POS, id);
    }

    @Override
    public void setOceanFloorBiomeTint(int x, int z, int value) {
    }

    @Override
    public void setOceanFloorLight(int x, int z, int value) {
        setByte(x, z, OCEANFLOOR_LIGHT_POS, value);
    }

    @Override
    public void setTransparentHeight(int x, int z, int value) {
        setShort(x, z, TRANSPARENT_HEIGHT_POS, value);
    }

    @Override
    public void setTransparentBlockstate(int x, int z, BlockState blockState) {
        int id = this.getIDFromState(blockState);
        setShort(x, z, TRANSPARENT_BLOCKSTATE_POS, id);
    }

    @Override
    public void setTransparentBiomeTint(int x, int z, int value) {
    }

    @Override
    public void setTransparentLight(int x, int z, int value) {
        setByte(x, z, TRANSPARENT_LIGHT_POS, value);
    }

    @Override
    public void setFoliageHeight(int x, int z, int value) {
        setShort(x, z, FOLIAGE_HEIGHT_POS, value);
    }

    @Override
    public void setFoliageBlockstate(int x, int z, BlockState blockState) {
        int id = this.getIDFromState(blockState);
        setShort(x, z, FOLIAGE_BLOCKSTATE_POS, id);
    }

    @Override
    public void setFoliageBiomeTint(int x, int z, int value) {
    }

    @Override
    public void setFoliageLight(int x, int z, int value) {
        setByte(x, z, FOLIAGE_LIGHT_POS, value);
    }

    @Override
    public void setBiomeID(int x, int z, int value) {
        setShort(x, z, BIOMEID_POS, value);
    }

    private synchronized void setShort(int x, int z, int bit, int value) {
        setByte(x, z, bit, (byte) (value >> 8));
        setByte(x, z, bit + 1, (byte) value);
    }

    private synchronized void setByte(int x, int z, int bit, int value) {
        if (this.isCompressed) {
            this.decompress();
        }

        int index = x + z * this.width + this.width * this.height * bit;
        this.data[index] = (byte) value;
    }

    @Override
    public void moveX(int offset) {
        synchronized (this.dataLock) {
            if (offset > 0) {
                System.arraycopy(this.data, offset * DATABYTES, this.data, 0, this.data.length - offset * DATABYTES);
            } else if (offset < 0) {
                System.arraycopy(this.data, 0, this.data, -offset * DATABYTES, this.data.length + offset * DATABYTES);
            }

        }
    }

    @Override
    public void moveZ(int offset) {
        synchronized (this.dataLock) {
            if (offset > 0) {
                System.arraycopy(this.data, offset * this.width * DATABYTES, this.data, 0, this.data.length - offset * this.width * DATABYTES);
            } else if (offset < 0) {
                System.arraycopy(this.data, 0, this.data, -offset * this.width * DATABYTES, this.data.length + offset * this.width * DATABYTES);
            }

        }
    }

    public synchronized void setData(byte[] is, BiMap<BlockState, Integer> newStateToInt, int version) {
        this.data = is;
        this.isCompressed = false;
        if (version < 2) {
            this.convertInterlacedData();
        }
        if (version < VERSION) {
            this.convertHeightData();
        }

        this.stateToInt = newStateToInt;
        this.blockStateCount = this.stateToInt.size();
    }

    private synchronized void convertInterlacedData() {
        if (this.isCompressed) {
            this.decompress();
        }

        byte[] newData = new byte[this.data.length];

        for (int x = 0; x < this.width; ++x) {
            for (int z = 0; z < this.height; ++z) {
                for (int bit = 0; bit < DATABYTES; ++bit) {
                    int oldIndex = (x + z * this.width) * DATABYTES + bit;
                    int newIndex = x + z * this.width + this.width * this.height * bit;
                    newData[newIndex] = this.data[oldIndex];
                }
            }
        }

        this.data = newData;
    }

    /**
     * convert one-byte height to two-byte
     */
    private synchronized void convertHeightData() {
        data = new HeightConverter(data, width, height).newData;
    }

    private static class HeightConverter {
        byte[] data;
        byte[] newData;
        int width, height;
        int oldIdx, newIdx;

        HeightConverter(byte[] data, int width, int height) {
            if (data.length != width * height * 18)
                throw new IllegalArgumentException("Data is not in expected format");

            this.data = data;
            this.width = width;
            this.height = height;
            this.newData = new byte[DATABYTES * width * height];

            convertSlice(1, 2); // HEIGHT
            convertSlice(2, 2); // BLOCKSTATE
            convertSlice(1, 1); // LIGHT
            convertSlice(1, 2); // OCEANFLOOR_HEIGHT
            convertSlice(2, 2); // OCEANFLOOR_BLOCKSTATE
            convertSlice(1, 1); // OCEANFLOOR_LIGHT
            convertSlice(1, 2); // TRANSPARENT_HEIGHT
            convertSlice(2, 2); // TRANSPARENT_BLOCKSTATE
            convertSlice(1, 1); // TRANSPARENT_LIGHT
            convertSlice(1, 2); // FOLIAGE_HEIGHT
            convertSlice(2, 2); // FOLIAGE_BLOCKSTATE
            convertSlice(1, 1); // FOLIAGE_LIGHT
            convertSlice(2, 2); // BIOMEID
        }

        private void convertSlice(int oldBytes, int newBytes) {
            if (oldBytes > newBytes) throw new IllegalArgumentException("Cannot convert from large size to small size");
            int sliceSize = width * height;
            // if newBytes>oldBytes, prepend zeroes
            int newIdxShifted = newIdx + newBytes - oldBytes;
            System.arraycopy(data, oldIdx * sliceSize, newData, newIdxShifted * sliceSize, oldBytes * sliceSize);
            oldIdx += oldBytes;
            newIdx += newBytes;
        }
    }

    public synchronized byte[] getData() {
        if (this.isCompressed) {
            this.decompress();
        }

        return this.data;
    }

    public synchronized void compress() {
        if (!this.isCompressed) {
            try {
                this.isCompressed = true;
                this.data = CompressionUtils.compress(this.data);
            } catch (IOException ignored) {
            }

        }
    }

    private synchronized void decompress() {
        if (this.stateToInt == null) {
            this.stateToInt = HashBiMap.create();
        }

        if (this.isCompressed) {
            try {
                this.data = CompressionUtils.decompress(this.data);
                this.isCompressed = false;
            } catch (IOException | DataFormatException ignored) {
            }

        }
    }

    public synchronized boolean isCompressed() {
        return this.isCompressed;
    }

    private BlockState getStateFromID(int id) {
        return this.stateToInt.inverse().get(id);
    }

    private synchronized int getIDFromState(BlockState blockState) {
        return getIDFromState(blockState, stateToInt);
    }

    private synchronized int getIDFromState(BlockState blockState, BiMap<BlockState, Integer> stateToInt) {
        if (blockState == null) return 0;
        Integer id = stateToInt.get(blockState);
        if (id == null) {
            while (stateToInt.containsValue(blockStateCount)) {
                ++blockStateCount;
            }
            id = blockStateCount;
            stateToInt.put(blockState, id);
        }
        return id;
    }

    public BiMap<BlockState, Integer> buildStateToInt() {
        this.stateToInt = this.createKeyFromCurrentBlocks(this.stateToInt);
        return this.stateToInt;
    }

    private BiMap<BlockState, Integer> createKeyFromCurrentBlocks(BiMap<BlockState, Integer> oldMap) {
        this.blockStateCount = 1;
        HashBiMap<BlockState, Integer> newMap = HashBiMap.create();

        for (int x = 0; x < this.width; ++x) {
            for (int z = 0; z < this.height; ++z) {
                int oldID = getUnsignedShort(x, z, BLOCKSTATE_POS);
                if (oldID != 0) {
                    BlockState blockState = oldMap.inverse().get(oldID);
                    int id = getIDFromState(blockState, newMap);
                    setShort(x, z, 1, id);
                }

                oldID = getUnsignedShort(x, z, OCEANFLOOR_BLOCKSTATE_POS);
                if (oldID != 0) {
                    BlockState blockState = oldMap.inverse().get(oldID);
                    int id = getIDFromState(blockState, newMap);
                    setShort(x, z, OCEANFLOOR_BLOCKSTATE_POS, id);
                }

                oldID = getUnsignedShort(x, z, TRANSPARENT_BLOCKSTATE_POS);
                if (oldID != 0) {
                    BlockState blockState = oldMap.inverse().get(oldID);
                    int id = getIDFromState(blockState, newMap);
                    setShort(x, z, TRANSPARENT_BLOCKSTATE_POS, id);
                }

                oldID = getUnsignedShort(x, z, FOLIAGE_BLOCKSTATE_POS);
                if (oldID != 0) {
                    BlockState blockState = oldMap.inverse().get(oldID);
                    int id = getIDFromState(blockState, newMap);
                    setShort(x, z, FOLIAGE_BLOCKSTATE_POS, id);
                }
            }
        }

        return newMap;
    }
}
