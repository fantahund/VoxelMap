package com.mamiyaotaru.voxelmap.persistent;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.interfaces.AbstractMapData;
import com.mamiyaotaru.voxelmap.util.CompressionUtils;
import net.minecraft.block.BlockState;

import java.io.IOException;
import java.util.Arrays;
import java.util.zip.DataFormatException;

public class CompressibleMapData extends AbstractMapData {
    private byte[] data;
    private boolean isCompressed;
    private BiMap<BlockState, Integer> stateToInt = null;
    int count = 1;
    private static byte[] compressedEmptyData = new byte[1179648];

    public CompressibleMapData(int width, int height) {
        this.width = width;
        this.height = height;
        this.data = compressedEmptyData;
        this.isCompressed = true;
    }

    @Override
    public int getHeight(int x, int z) {
        return this.getData(x, z, 0) & 0xFF;
    }

    @Override
    public BlockState getBlockstate(int x, int z) {
        int id = (this.getData(x, z, 1) & 255) << 8 | this.getData(x, z, 2) & 255;
        return this.getStateFromID(id);
    }

    @Override
    public int getBiomeTint(int x, int z) {
        return 0;
    }

    @Override
    public int getLight(int x, int z) {
        return this.getData(x, z, 3) & 0xFF;
    }

    @Override
    public int getOceanFloorHeight(int x, int z) {
        return this.getData(x, z, 4) & 0xFF;
    }

    @Override
    public BlockState getOceanFloorBlockstate(int x, int z) {
        int id = (this.getData(x, z, 5) & 255) << 8 | this.getData(x, z, 6) & 255;
        return this.getStateFromID(id);
    }

    @Override
    public int getOceanFloorBiomeTint(int x, int z) {
        return 0;
    }

    @Override
    public int getOceanFloorLight(int x, int z) {
        return this.getData(x, z, 7) & 0xFF;
    }

    @Override
    public int getTransparentHeight(int x, int z) {
        return this.getData(x, z, 8) & 0xFF;
    }

    @Override
    public BlockState getTransparentBlockstate(int x, int z) {
        int id = (this.getData(x, z, 9) & 255) << 8 | this.getData(x, z, 10) & 255;
        return this.getStateFromID(id);
    }

    @Override
    public int getTransparentBiomeTint(int x, int z) {
        return 0;
    }

    @Override
    public int getTransparentLight(int x, int z) {
        return this.getData(x, z, 11) & 0xFF;
    }

    @Override
    public int getFoliageHeight(int x, int z) {
        return this.getData(x, z, 12) & 0xFF;
    }

    @Override
    public BlockState getFoliageBlockstate(int x, int z) {
        int id = (this.getData(x, z, 13) & 255) << 8 | this.getData(x, z, 14) & 255;
        return this.getStateFromID(id);
    }

    @Override
    public int getFoliageBiomeTint(int x, int z) {
        return 0;
    }

    @Override
    public int getFoliageLight(int x, int z) {
        return this.getData(x, z, 15) & 0xFF;
    }

    @Override
    public int getBiomeID(int x, int z) {
        return (this.getData(x, z, 16) & 0xFF) << 8 | this.getData(x, z, 17) & 0xFF;
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
        this.setData(x, z, 0, (byte) value);
    }

    @Override
    public void setBlockstate(int x, int z, BlockState blockState) {
        int id = this.getIDFromState(blockState);
        this.setData(x, z, 1, (byte) (id >> 8));
        this.setData(x, z, 2, (byte) id);
    }

    @Override
    public void setBiomeTint(int x, int z, int value) {
    }

    @Override
    public void setLight(int x, int z, int value) {
        this.setData(x, z, 3, (byte) value);
    }

    @Override
    public void setOceanFloorHeight(int x, int z, int value) {
        this.setData(x, z, 4, (byte) value);
    }

    @Override
    public void setOceanFloorBlockstate(int x, int z, BlockState blockState) {
        int id = this.getIDFromState(blockState);
        this.setData(x, z, 5, (byte) (id >> 8));
        this.setData(x, z, 6, (byte) id);
    }

    @Override
    public void setOceanFloorBiomeTint(int x, int z, int value) {
    }

    @Override
    public void setOceanFloorLight(int x, int z, int value) {
        this.setData(x, z, 7, (byte) value);
    }

    @Override
    public void setTransparentHeight(int x, int z, int value) {
        this.setData(x, z, 8, (byte) value);
    }

    @Override
    public void setTransparentBlockstate(int x, int z, BlockState blockState) {
        int id = this.getIDFromState(blockState);
        this.setData(x, z, 9, (byte) (id >> 8));
        this.setData(x, z, 10, (byte) id);
    }

    @Override
    public void setTransparentBiomeTint(int x, int z, int value) {
    }

    @Override
    public void setTransparentLight(int x, int z, int value) {
        this.setData(x, z, 11, (byte) value);
    }

    @Override
    public void setFoliageHeight(int x, int z, int value) {
        this.setData(x, z, 12, (byte) value);
    }

    @Override
    public void setFoliageBlockstate(int x, int z, BlockState blockState) {
        int id = this.getIDFromState(blockState);
        this.setData(x, z, 13, (byte) (id >> 8));
        this.setData(x, z, 14, (byte) id);
    }

    @Override
    public void setFoliageBiomeTint(int x, int z, int value) {
    }

    @Override
    public void setFoliageLight(int x, int z, int value) {
        this.setData(x, z, 15, (byte) value);
    }

    @Override
    public void setBiomeID(int x, int z, int value) {
        this.setData(x, z, 16, (byte) (value >> 8));
        this.setData(x, z, 17, (byte) value);
    }

    private synchronized void setData(int x, int z, int bit, byte value) {
        if (this.isCompressed) {
            this.decompress();
        }

        int index = x + z * this.width + this.width * this.height * bit;
        this.data[index] = value;
    }

    @Override
    public void moveX(int offset) {
        synchronized (this.dataLock) {
            if (offset > 0) {
                System.arraycopy(this.data, offset * 18, this.data, 0, this.data.length - offset * 18);
            } else if (offset < 0) {
                System.arraycopy(this.data, 0, this.data, -offset * 18, this.data.length + offset * 18);
            }

        }
    }

    @Override
    public void moveZ(int offset) {
        synchronized (this.dataLock) {
            if (offset > 0) {
                System.arraycopy(this.data, offset * this.width * 18, this.data, 0, this.data.length - offset * this.width * 18);
            } else if (offset < 0) {
                System.arraycopy(this.data, 0, this.data, -offset * this.width * 18, this.data.length + offset * this.width * 18);
            }

        }
    }

    public synchronized void setData(byte[] is, BiMap<BlockState, Integer> newStateToInt, int version) {
        this.data = is;
        this.isCompressed = false;
        if (version < 2) {
            this.convertData();
        }

        this.stateToInt = newStateToInt;
        this.count = this.stateToInt.size();
    }

    private synchronized void convertData() {
        if (this.isCompressed) {
            this.decompress();
        }

        byte[] newData = new byte[this.data.length];

        for (int x = 0; x < this.width; ++x) {
            for (int z = 0; z < this.height; ++z) {
                for (int bit = 0; bit < 18; ++bit) {
                    int oldIndex = (x + z * this.width) * 18 + bit;
                    int newIndex = x + z * this.width + this.width * this.height * bit;
                    newData[newIndex] = this.data[oldIndex];
                }
            }
        }

        this.data = newData;
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
            } catch (IOException ignored) {}

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
            } catch (IOException | DataFormatException ignored) {}

        }
    }

    public synchronized boolean isCompressed() {
        return this.isCompressed;
    }

    private synchronized int getIDFromState(BlockState blockState) {
        Integer id = this.stateToInt.get(blockState);
        if (id == null && blockState != null) {
            while (this.stateToInt.inverse().containsKey(this.count)) {
                ++this.count;
            }

            id = this.count;
            this.stateToInt.put(blockState, id);
        }

        return id;
    }

    private BlockState getStateFromID(int id) {
        return this.stateToInt.inverse().get(id);
    }

    public BiMap<BlockState, Integer> getStateToInt() {
        this.stateToInt = this.createKeyFromCurrentBlocks(this.stateToInt);
        return this.stateToInt;
    }

    private BiMap<BlockState, Integer> createKeyFromCurrentBlocks(BiMap<BlockState, Integer> oldMap) {
        this.count = 1;
        BiMap<BlockState, Integer> newMap = HashBiMap.create();

        for (int x = 0; x < this.width; ++x) {
            for (int z = 0; z < this.height; ++z) {
                int oldID = (this.getData(x, z, 1) & 255) << 8 | this.getData(x, z, 2) & 255;
                if (oldID != 0) {
                    BlockState blockState = oldMap.inverse().get(oldID);
                    Integer id = newMap.get(blockState);
                    if (id == null && blockState != null) {
                        while (newMap.inverse().containsKey(this.count)) {
                            ++this.count;
                        }

                        id = this.count;
                        newMap.put(blockState, id);
                    }

                    this.setData(x, z, 1, (byte) (id >> 8));
                    this.setData(x, z, 2, (byte) id.intValue());
                }

                oldID = (this.getData(x, z, 5) & 255) << 8 | this.getData(x, z, 6) & 255;
                if (oldID != 0) {
                    BlockState blockState = oldMap.inverse().get(oldID);
                    Integer id = newMap.get(blockState);
                    if (id == null && blockState != null) {
                        while (newMap.inverse().containsKey(this.count)) {
                            ++this.count;
                        }

                        id = this.count;
                        newMap.put(blockState, id);
                    }

                    this.setData(x, z, 5, (byte) (id >> 8));
                    this.setData(x, z, 6, (byte) id.intValue());
                }

                oldID = (this.getData(x, z, 9) & 255) << 8 | this.getData(x, z, 10) & 255;
                if (oldID != 0) {
                    BlockState blockState = oldMap.inverse().get(oldID);
                    Integer id = newMap.get(blockState);
                    if (id == null && blockState != null) {
                        while (newMap.inverse().containsKey(this.count)) {
                            ++this.count;
                        }

                        id = this.count;
                        newMap.put(blockState, id);
                    }

                    this.setData(x, z, 9, (byte) (id >> 8));
                    this.setData(x, z, 10, (byte) id.intValue());
                }

                oldID = (this.getData(x, z, 13) & 255) << 8 | this.getData(x, z, 14) & 255;
                if (oldID != 0) {
                    BlockState blockState = oldMap.inverse().get(oldID);
                    Integer id = newMap.get(blockState);
                    if (id == null && blockState != null) {
                        while (newMap.inverse().containsKey(this.count)) {
                            ++this.count;
                        }

                        id = this.count;
                        newMap.put(blockState, id);
                    }

                    this.setData(x, z, 13, (byte) (id >> 8));
                    this.setData(x, z, 14, (byte) id.intValue());
                }
            }
        }

        return newMap;
    }

    static {
        Arrays.fill(compressedEmptyData, (byte) 0);

        try {
            compressedEmptyData = CompressionUtils.compress(compressedEmptyData);
        } catch (IOException var1) {
            VoxelMap.getLogger().error(var1);
        }

    }
}
