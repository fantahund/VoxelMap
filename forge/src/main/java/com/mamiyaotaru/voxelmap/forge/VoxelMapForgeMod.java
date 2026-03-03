package com.mamiyaotaru.voxelmap.forge;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(VoxelConstants.MOD_ID)
public class VoxelMapForgeMod {
    private static BusGroup modBusGroup;

    public VoxelMapForgeMod(FMLJavaModLoadingContext context) {
        VoxelMapForgeMod.modBusGroup = context.getModBusGroup();
        VoxelConstants.setModVersion(context.getContainer().getModInfo().getVersion().toString());
        VoxelConstants.setEvents(new ForgeEvents());
        VoxelConstants.setPacketBridge(new ForgePacketBridge());
    }

    public static BusGroup getModBusGroup() {
        return modBusGroup;
    }
}
