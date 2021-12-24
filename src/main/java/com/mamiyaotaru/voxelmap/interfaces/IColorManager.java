package com.mamiyaotaru.voxelmap.interfaces;

import com.mamiyaotaru.voxelmap.util.MutableBlockPos;
import java.awt.image.BufferedImage;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraft.resource.ResourceManager;

public interface IColorManager {
   void onResourceManagerReload(ResourceManager var1);

   BufferedImage getColorPicker();

   BufferedImage getBlockImage(BlockState var1, ItemStack var2, World var3, float var4, float var5);

   boolean checkForChanges();

   int getBlockColorWithDefaultTint(MutableBlockPos var1, int var2);

   int getBlockColor(MutableBlockPos var1, int var2, int var3);

   void setSkyColor(int var1);

   int getAirColor();

   int getBiomeTint(AbstractMapData var1, World var2, BlockState var3, int var4, MutableBlockPos var5, MutableBlockPos var6, int var7, int var8);
}
