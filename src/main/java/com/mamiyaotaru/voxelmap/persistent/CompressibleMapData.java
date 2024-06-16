package com.mamiyaotaru.voxelmap.persistent;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mamiyaotaru.voxelmap.interfaces.AbstractMapData;
import com.mamiyaotaru.voxelmap.util.CompressionUtils;
import net.minecraft.block.BlockState;

import java.util.zip.DataFormatException;

public class CompressibleMapData extends AbstractMapData {
    public final static int LAYERS = 18;

    private static final int HEIGHTPOS = 0;
    private static final int BLOCKSTATEPOS = 1;
    private static final int LIGHTPOS = 3;

    private static final int OCEANFLOORHEIGHTPOS = 4;
    private static final int OCEANFLOORBLOCKSTATEPOS = 5;
    private static final int OCEANFLOORLIGHTPOS = 7;

    private static final int TRANSPARENTHEIGHTPOS = 8;
    private static final int TRANSPARENTBLOCKSTATEPOS = 9;
    private static final int TRANSPARENTLIGHTPOS = 11;

    private static final int FOLIAGEHEIGHTPOS = 12;
    private static final int FOLIAGEBLOCKSTATEPOS = 13;
    private static final int FOLIAGELIGHTPOS = 15;

    private static final int BIOMEIDPOS = 16;

    private final static int REGION_SIZE = 256;
    private final static byte[] compressedEmptyData = CompressionUtils.compress(new byte[REGION_SIZE * REGION_SIZE * LAYERS]);

    private byte[] data;
    private boolean isCompressed;
    private BiMap<BlockState, Integer> stateToInt;
    int count = 1;

    public CompressibleMapData() {
        this.width = REGION_SIZE;
        this.height = REGION_SIZE;
        this.data = compressedEmptyData;
        this.isCompressed = true;
    }

    @Override
    public int getHeight(int x, int z) {
        return this.getData(x, z, HEIGHTPOS) & 0xFF;
    }

    @Override
    public BlockState getBlockstate(int x, int z) {
        int id = (this.getData(x, z, BLOCKSTATEPOS) & 255) << 8 | this.getData(x, z, BLOCKSTATEPOS + 1) & 255;
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
        return this.getData(x, z, OCEANFLOORHEIGHTPOS) & 0xFF;
    }

    @Override
    public BlockState getOceanFloorBlockstate(int x, int z) {
        int id = (this.getData(x, z, OCEANFLOORBLOCKSTATEPOS) & 255) << 8 | this.getData(x, z, OCEANFLOORBLOCKSTATEPOS + 1) & 255;
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
        return this.getData(x, z, TRANSPARENTHEIGHTPOS) & 0xFF;
    }

    @Override
    public BlockState getTransparentBlockstate(int x, int z) {
        int id = (this.getData(x, z, TRANSPARENTBLOCKSTATEPOS) & 255) << 8 | this.getData(x, z, TRANSPARENTBLOCKSTATEPOS + 1) & 255;
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
        return this.getData(x, z, FOLIAGEHEIGHTPOS) & 0xFF;
    }

    @Override
    public BlockState getFoliageBlockstate(int x, int z) {
        int id = (this.getData(x, z, FOLIAGEBLOCKSTATEPOS) & 255) << 8 | this.getData(x, z, FOLIAGEBLOCKSTATEPOS + 1) & 255;
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
    public int getBiomeID(int x, int z) {
        return (this.getData(x, z, BIOMEIDPOS) & 0xFF) << 8 | this.getData(x, z, BIOMEIDPOS + 1) & 0xFF;
    }

    private synchronized byte getData(int x, int z, int bit) {
        if (this.isCompressed) {
            this.decompress();
        }

        int index = x + z * this.width + this.width * this.height * bit;
        return this.data[index];
    }

    @Override
    public void setHeight(int x, int z, int height) {
        this.setData(x, z, HEIGHTPOS, (byte) height);
    }

    @Override
    public void setBlockstate(int x, int z, BlockState state) {
        int id = this.getIDFromState(state);
        this.setData(x, z, BLOCKSTATEPOS, (byte) (id >> 8));
        this.setData(x, z, BLOCKSTATEPOS + 1, (byte) id);
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
        this.setData(x, z, OCEANFLOORHEIGHTPOS, (byte) height);
    }

    @Override
    public void setOceanFloorBlockstate(int x, int z, BlockState state) {
        int id = this.getIDFromState(state);
        this.setData(x, z, OCEANFLOORBLOCKSTATEPOS, (byte) (id >> 8));
        this.setData(x, z, OCEANFLOORBLOCKSTATEPOS + 1, (byte) id);
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
        this.setData(x, z, TRANSPARENTHEIGHTPOS, (byte) height);
    }

    @Override
    public void setTransparentBlockstate(int x, int z, BlockState state) {
        int id = this.getIDFromState(state);
        this.setData(x, z, TRANSPARENTBLOCKSTATEPOS, (byte) (id >> 8));
        this.setData(x, z, TRANSPARENTBLOCKSTATEPOS + 1, (byte) id);
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
        this.setData(x, z, FOLIAGEHEIGHTPOS, (byte) height);
    }

    @Override
    public void setFoliageBlockstate(int x, int z, BlockState state) {
        int id = this.getIDFromState(state);
        this.setData(x, z, FOLIAGEBLOCKSTATEPOS, (byte) (id >> 8));
        this.setData(x, z, FOLIAGEBLOCKSTATEPOS + 1, (byte) id);
    }

    @Override
    public void setFoliageBiomeTint(int x, int z, int tint) {
    }

    @Override
    public void setFoliageLight(int x, int z, int light) {
        this.setData(x, z, FOLIAGELIGHTPOS, (byte) light);
    }

    @Override
    public void setBiomeID(int x, int z, int id) {
        this.setData(x, z, BIOMEIDPOS, (byte) (id >> 8));
        this.setData(x, z, BIOMEIDPOS + 1, (byte) id);
    }

    private synchronized void setData(int x, int z, int bit, byte value) {
        if (this.isCompressed) {
            this.decompress();
        }

        int index = x + z * this.width + this.width * this.height * bit;
        this.data[index] = value;
    }

    @Override
    public void moveX(int x) {
        synchronized (this.dataLock) {
            if (x > 0) {
                System.arraycopy(this.data, x * LAYERS, this.data, 0, this.data.length - x * LAYERS);
            } else if (x < 0) {
                System.arraycopy(this.data, 0, this.data, -x * LAYERS, this.data.length + x * LAYERS);
            }

        }
    }

    @Override
    public void moveZ(int z) {
        synchronized (this.dataLock) {
            if (z > 0) {
                System.arraycopy(this.data, z * this.width * LAYERS, this.data, 0, this.data.length - z * this.width * LAYERS);
            } else if (z < 0) {
                System.arraycopy(this.data, 0, this.data, -z * this.width * LAYERS, this.data.length + z * this.width * LAYERS);
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
                for (int layer = 0; layer < LAYERS; ++layer) {
                    int oldIndex = (x + z * this.width) * LAYERS + layer;
                    int newIndex = x + z * this.width + this.width * this.height * layer;
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
            this.isCompressed = true;
            this.data = CompressionUtils.compress(this.data);
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
            } catch (DataFormatException ignored) {
            }
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
                int oldID = (this.getData(x, z, BLOCKSTATEPOS) & 255) << 8 | this.getData(x, z, BLOCKSTATEPOS + 1) & 255;
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

                    this.setData(x, z, BLOCKSTATEPOS, (byte) (id >> 8));
                    this.setData(x, z, BLOCKSTATEPOS + 1, (byte) id.intValue());
                }

                oldID = (this.getData(x, z, OCEANFLOORBLOCKSTATEPOS) & 255) << 8 | this.getData(x, z, OCEANFLOORBLOCKSTATEPOS + 1) & 255;
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

                    this.setData(x, z, OCEANFLOORBLOCKSTATEPOS, (byte) (id >> 8));
                    this.setData(x, z, OCEANFLOORBLOCKSTATEPOS + 1, (byte) id.intValue());
                }

                oldID = (this.getData(x, z, TRANSPARENTBLOCKSTATEPOS) & 255) << 8 | this.getData(x, z, TRANSPARENTBLOCKSTATEPOS + 1) & 255;
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

                    this.setData(x, z, TRANSPARENTBLOCKSTATEPOS, (byte) (id >> 8));
                    this.setData(x, z, TRANSPARENTBLOCKSTATEPOS + 1, (byte) id.intValue());
                }

                oldID = (this.getData(x, z, FOLIAGEBLOCKSTATEPOS) & 255) << 8 | this.getData(x, z, FOLIAGEBLOCKSTATEPOS + 1) & 255;
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

                    this.setData(x, z, FOLIAGEBLOCKSTATEPOS, (byte) (id >> 8));
                    this.setData(x, z, FOLIAGEBLOCKSTATEPOS + 1, (byte) id.intValue());
                }
            }
        }

        return newMap;
    }

    public int getExpectedDataLength(int version) {
        return getWidth() * getHeight() * CompressibleMapData.LAYERS;
    }
}
