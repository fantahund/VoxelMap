package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.interfaces.AbstractMapData;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

public class BiomeMapData extends AbstractMapData {
    private Biome[] data;

    public BiomeMapData(int width, int height) {
        this.width = width;
        this.height = height;
        this.data = new Biome[width * height];
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
    public Biome getBiome(int x, int z) {
        return this.getData(x, z, 0);
    }

    private Biome getData(int x, int z, int bit) {
        int index = (x + z * this.width) + bit;
        return this.data[index];
    }

    @Override
    public void setHeight(int x, int z, int height) {
    }

    @Override
    public void setBlockstate(int x, int z, BlockState state) {
    }

    @Override
    public void setBiomeTint(int x, int z, int tint) {
    }

    @Override
    public void setLight(int x, int z, int light) {
    }

    @Override
    public void setOceanFloorHeight(int x, int z, int height) {
    }

    @Override
    public void setOceanFloorBlockstate(int x, int z, BlockState state) {
    }

    @Override
    public void setOceanFloorBiomeTint(int x, int z, int tint) {
    }

    @Override
    public void setOceanFloorLight(int x, int z, int light) {
    }

    @Override
    public void setTransparentHeight(int x, int z, int height) {
    }

    @Override
    public void setTransparentBlockstate(int x, int z, BlockState state) {
    }

    @Override
    public void setTransparentBiomeTint(int x, int z, int tint) {
    }

    @Override
    public void setTransparentLight(int x, int z, int light) {
    }

    @Override
    public void setFoliageHeight(int x, int z, int height) {
    }

    @Override
    public void setFoliageBlockstate(int x, int z, BlockState state) {
    }

    @Override
    public void setFoliageBiomeTint(int x, int z, int tint) {
    }

    @Override
    public void setFoliageLight(int x, int z, int light) {
    }

    @Override
    public void setBiome(int x, int z, Biome id) {
        this.setData(x, z, 0, id);
    }

    private void setData(int x, int z, int bit, Biome value) {
        int index = (x + z * this.width) + bit;
        this.data[index] = value;
    }

    @Override
    public void moveX(int x) {
        synchronized (this.dataLock) {
            if (x > 0) {
                System.arraycopy(this.data, x, this.data, 0, this.data.length - x);
            } else if (x < 0) {
                System.arraycopy(this.data, 0, this.data, -x, this.data.length + x);
            }

        }
    }

    @Override
    public void moveZ(int z) {
        synchronized (this.dataLock) {
            if (z > 0) {
                System.arraycopy(this.data, z * this.width, this.data, 0, this.data.length - z * this.width);
            } else if (z < 0) {
                System.arraycopy(this.data, 0, this.data, -z * this.width, this.data.length + z * this.width);
            }

        }
    }
}
