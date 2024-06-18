package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.interfaces.AbstractMapData;
import net.minecraft.block.BlockState;
import net.minecraft.world.biome.Biome;
import java.util.Arrays;

public class FullMapData extends AbstractMapData {
    private final static int LAYERS = 17;

    private int[] data;
    private Biome[] biomes;

    public FullMapData(int width, int height) {
        this.width = width;
        this.height = height;
        this.data = new int[width * height * LAYERS];
        this.biomes = new Biome[width * height];
        Arrays.fill(this.data, 0);
    }

    public void blank() {
        Arrays.fill(this.data, 0);
    }

    @Override
    public int getHeight(int x, int z) {
        return this.getData(x, z, 0);
    }

    public int getBlockstateID(int x, int z) {
        return this.getData(x, z, 1);
    }

    @Override
    public BlockState getBlockstate(int x, int z) {
        return this.getStateFromID(this.getData(x, z, 1));
    }

    @Override
    public int getBiomeTint(int x, int z) {
        return this.getData(x, z, 2);
    }

    @Override
    public int getLight(int x, int z) {
        return this.getData(x, z, 3);
    }

    @Override
    public int getOceanFloorHeight(int x, int z) {
        return this.getData(x, z, 4);
    }

    public int getOceanFloorBlockstateID(int x, int z) {
        return this.getData(x, z, 5);
    }

    @Override
    public BlockState getOceanFloorBlockstate(int x, int z) {
        return this.getStateFromID(this.getData(x, z, 5));
    }

    @Override
    public int getOceanFloorBiomeTint(int x, int z) {
        return this.getData(x, z, 6);
    }

    @Override
    public int getOceanFloorLight(int x, int z) {
        return this.getData(x, z, 7);
    }

    @Override
    public int getTransparentHeight(int x, int z) {
        return this.getData(x, z, 8);
    }

    public int getTransparentBlockstateID(int x, int z) {
        return this.getData(x, z, 9);
    }

    @Override
    public BlockState getTransparentBlockstate(int x, int z) {
        return this.getStateFromID(this.getData(x, z, 9));
    }

    @Override
    public int getTransparentBiomeTint(int x, int z) {
        return this.getData(x, z, 10);
    }

    @Override
    public int getTransparentLight(int x, int z) {
        return this.getData(x, z, 11);
    }

    @Override
    public int getFoliageHeight(int x, int z) {
        return this.getData(x, z, 12);
    }

    public int getFoliageBlockstateID(int x, int z) {
        return this.getData(x, z, 13);
    }

    @Override
    public BlockState getFoliageBlockstate(int x, int z) {
        return this.getStateFromID(this.getData(x, z, 13));
    }

    @Override
    public int getFoliageBiomeTint(int x, int z) {
        return this.getData(x, z, 14);
    }

    @Override
    public int getFoliageLight(int x, int z) {
        return this.getData(x, z, 15);
    }

    @Override
    public Biome getBiome(int x, int z) {
        return this.biomes[x + z * this.width];
    }

    private int getData(int x, int z, int bit) {
        int index = (x + z * this.width) * LAYERS + bit;
        return this.data[index];
    }

    @Override
    public void setHeight(int x, int z, int height) {
        this.setData(x, z, 0, height);
    }

    public void setBlockstateID(int x, int z, int id) {
        this.setData(x, z, 1, id);
    }

    @Override
    public void setBlockstate(int x, int z, BlockState state) {
        this.setData(x, z, 1, this.getIDFromState(state));
    }

    @Override
    public void setBiomeTint(int x, int z, int tint) {
        this.setData(x, z, 2, tint);
    }

    @Override
    public void setLight(int x, int z, int light) {
        this.setData(x, z, 3, light);
    }

    @Override
    public void setOceanFloorHeight(int x, int z, int height) {
        this.setData(x, z, 4, height);
    }

    public void setOceanFloorBlockstateID(int x, int z, int id) {
        this.setData(x, z, 5, id);
    }

    @Override
    public void setOceanFloorBlockstate(int x, int z, BlockState state) {
        this.setData(x, z, 5, this.getIDFromState(state));
    }

    @Override
    public void setOceanFloorBiomeTint(int x, int z, int tint) {
        this.setData(x, z, 6, tint);
    }

    @Override
    public void setOceanFloorLight(int x, int z, int light) {
        this.setData(x, z, 7, light);
    }

    @Override
    public void setTransparentHeight(int x, int z, int height) {
        this.setData(x, z, 8, height);
    }

    public void setTransparentBlockstateID(int x, int z, int id) {
        this.setData(x, z, 9, id);
    }

    @Override
    public void setTransparentBlockstate(int x, int z, BlockState state) {
        this.setData(x, z, 9, this.getIDFromState(state));
    }

    @Override
    public void setTransparentBiomeTint(int x, int z, int tint) {
        this.setData(x, z, 10, tint);
    }

    @Override
    public void setTransparentLight(int x, int z, int light) {
        this.setData(x, z, 11, light);
    }

    @Override
    public void setFoliageHeight(int x, int z, int height) {
        this.setData(x, z, 12, height);
    }

    public void setFoliageBlockstateID(int x, int z, int id) {
        this.setData(x, z, 13, id);
    }

    @Override
    public void setFoliageBlockstate(int x, int z, BlockState state) {
        this.setData(x, z, 13, this.getIDFromState(state));
    }

    @Override
    public void setFoliageBiomeTint(int x, int z, int tint) {
        this.setData(x, z, 14, tint);
    }

    @Override
    public void setFoliageLight(int x, int z, int light) {
        this.setData(x, z, 15, light);
    }

    @Override
    public void setBiome(int x, int z, Biome biome) {
        this.biomes[x + z * this.width] = biome;
    }

    private void setData(int x, int z, int bit, int value) {
        int index = (x + z * this.width) * LAYERS + bit;
        this.data[index] = value;
    }

    @Override
    public void moveX(int x) {
        synchronized (this.dataLock) {
            if (x > 0) {
                System.arraycopy(this.data, x * LAYERS, this.data, 0, this.data.length - x * LAYERS);
                System.arraycopy(this.biomes, x, this.biomes, 0, this.biomes.length - x);
            } else if (x < 0) {
                System.arraycopy(this.data, 0, this.data, -x * LAYERS, this.data.length + x * LAYERS);
                System.arraycopy(this.biomes, 0, this.biomes, -x, this.biomes.length + x);
            }

        }
    }

    @Override
    public void moveZ(int z) {
        synchronized (this.dataLock) {
            if (z > 0) {
                System.arraycopy(this.data, z * this.width * LAYERS, this.data, 0, this.data.length - z * this.width * LAYERS);
                System.arraycopy(this.biomes, z * this.width, this.biomes, 0, this.biomes.length - z * this.width);
            } else if (z < 0) {
                System.arraycopy(this.data, 0, this.data, -z * this.width * LAYERS, this.data.length + z * this.width * LAYERS);
                System.arraycopy(this.biomes, 0, this.biomes, -z * this.width, this.biomes.length + z * this.width);
            }

        }
    }

    private int getIDFromState(BlockState blockState) {
        return BlockRepository.getStateId(blockState);
    }

    private BlockState getStateFromID(int id) {
        return BlockRepository.getStateById(id);
    }
}
