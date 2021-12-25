package com.mamiyaotaru.voxelmap.interfaces;

import net.minecraft.block.BlockState;

public interface IMapData {
    int DATABITS = 17;
    int BYTESPERDATUM = 4;

    int getWidth();

    int getHeight();

    int getHeight(int var1, int var2);

    BlockState getBlockstate(int var1, int var2);

    int getBiomeTint(int var1, int var2);

    int getLight(int var1, int var2);

    int getOceanFloorHeight(int var1, int var2);

    BlockState getOceanFloorBlockstate(int var1, int var2);

    int getOceanFloorBiomeTint(int var1, int var2);

    int getOceanFloorLight(int var1, int var2);

    int getTransparentHeight(int var1, int var2);

    BlockState getTransparentBlockstate(int var1, int var2);

    int getTransparentBiomeTint(int var1, int var2);

    int getTransparentLight(int var1, int var2);

    int getFoliageHeight(int var1, int var2);

    BlockState getFoliageBlockstate(int var1, int var2);

    int getFoliageBiomeTint(int var1, int var2);

    int getFoliageLight(int var1, int var2);

    int getBiomeID(int var1, int var2);

    void setHeight(int var1, int var2, int var3);

    void setBlockstate(int var1, int var2, BlockState var3);

    void setBiomeTint(int var1, int var2, int var3);

    void setLight(int var1, int var2, int var3);

    void setOceanFloorHeight(int var1, int var2, int var3);

    void setOceanFloorBlockstate(int var1, int var2, BlockState var3);

    void setOceanFloorBiomeTint(int var1, int var2, int var3);

    void setOceanFloorLight(int var1, int var2, int var3);

    void setTransparentHeight(int var1, int var2, int var3);

    void setTransparentBlockstate(int var1, int var2, BlockState var3);

    void setTransparentBiomeTint(int var1, int var2, int var3);

    void setTransparentLight(int var1, int var2, int var3);

    void setFoliageHeight(int var1, int var2, int var3);

    void setFoliageBlockstate(int var1, int var2, BlockState var3);

    void setFoliageBiomeTint(int var1, int var2, int var3);

    void setFoliageLight(int var1, int var2, int var3);

    void setBiomeID(int var1, int var2, int var3);

    void moveX(int var1);

    void moveZ(int var1);
}
