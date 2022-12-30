package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.interfaces.AbstractMapData;
import net.minecraft.block.BlockState;

import java.util.Arrays;

public class BiomeMapData extends AbstractMapData {
    private int[] data;

    public BiomeMapData(int width, int height) {
        this.width = width;
        this.height = height;
        this.data = new int[width * height];
        Arrays.fill(this.data, 0);
    }

    @Override
    public int getHeight(int x, int z) {
        return 0;
    }

    @Override
    public BlockState getBlockstate(int x, int z) {
        return null;
    }

    @Override
    public int getBiomeTint(int x, int z) {
        return 0;
    }

    @Override
    public int getLight(int x, int z) {
        return 0;
    }

    @Override
    public int getOceanFloorHeight(int x, int z) {
        return 0;
    }

    @Override
    public BlockState getOceanFloorBlockstate(int x, int z) {
        return null;
    }

    @Override
    public int getOceanFloorBiomeTint(int x, int z) {
        return 0;
    }

    @Override
    public int getOceanFloorLight(int x, int z) {
        return 0;
    }

    @Override
    public int getTransparentHeight(int x, int z) {
        return 0;
    }

    @Override
    public BlockState getTransparentBlockstate(int x, int z) {
        return null;
    }

    @Override
    public int getTransparentBiomeTint(int x, int z) {
        return 0;
    }

    @Override
    public int getTransparentLight(int x, int z) {
        return 0;
    }

    @Override
    public int getFoliageHeight(int x, int z) {
        return 0;
    }

    @Override
    public BlockState getFoliageBlockstate(int x, int z) {
        return null;
    }

    @Override
    public int getFoliageBiomeTint(int x, int z) {
        return 0;
    }

    @Override
    public int getFoliageLight(int x, int z) {
        return 0;
    }

    @Override
    public int getBiomeID(int x, int z) {
        return this.getData(x, z, 0);
    }

    private int getData(int x, int z, int bit) {
        int index = (x + z * this.width) + bit;
        return this.data[index];
    }

    @Override
    public void setHeight(int x, int z, int value) {
    }

    @Override
    public void setBlockstate(int x, int z, BlockState blockState) {
    }

    @Override
    public void setBiomeTint(int x, int z, int value) {
    }

    @Override
    public void setLight(int x, int z, int value) {
    }

    @Override
    public void setOceanFloorHeight(int x, int z, int value) {
    }

    @Override
    public void setOceanFloorBlockstate(int x, int z, BlockState blockState) {
    }

    @Override
    public void setOceanFloorBiomeTint(int x, int z, int value) {
    }

    @Override
    public void setOceanFloorLight(int x, int z, int value) {
    }

    @Override
    public void setTransparentHeight(int x, int z, int value) {
    }

    @Override
    public void setTransparentBlockstate(int x, int z, BlockState blockState) {
    }

    @Override
    public void setTransparentBiomeTint(int x, int z, int value) {
    }

    @Override
    public void setTransparentLight(int x, int z, int value) {
    }

    @Override
    public void setFoliageHeight(int x, int z, int value) {
    }

    @Override
    public void setFoliageBlockstate(int x, int z, BlockState blockState) {
    }

    @Override
    public void setFoliageBiomeTint(int x, int z, int value) {
    }

    @Override
    public void setFoliageLight(int x, int z, int value) {
    }

    @Override
    public void setBiomeID(int x, int z, int value) {
        this.setData(x, z, 0, value);
    }

    private void setData(int x, int z, int bit, int value) {
        int index = (x + z * this.width) + bit;
        this.data[index] = value;
    }

    @Override
    public void moveX(int offset) {
        synchronized (this.dataLock) {
            if (offset > 0) {
                System.arraycopy(this.data, offset, this.data, 0, this.data.length - offset);
            } else if (offset < 0) {
                System.arraycopy(this.data, 0, this.data, -offset, this.data.length + offset);
            }

        }
    }

    @Override
    public void moveZ(int offset) {
        synchronized (this.dataLock) {
            if (offset > 0) {
                System.arraycopy(this.data, offset * this.width, this.data, 0, this.data.length - offset * this.width);
            } else if (offset < 0) {
                System.arraycopy(this.data, 0, this.data, -offset * this.width, this.data.length + offset * this.width);
            }

        }
    }

    public void setData(int[] is) {
        this.data = is;
    }

    public int[] getData() {
        return this.data;
    }
}
