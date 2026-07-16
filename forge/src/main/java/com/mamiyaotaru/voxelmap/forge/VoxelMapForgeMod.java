package com.mamiyaotaru.voxelmap.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(VoxelMapForgeMod.MOD_ID)
public class VoxelMapForgeMod {
    public static final String MOD_ID = "voxelmap";
    private static BusGroup modBusGroup;

    public VoxelMapForgeMod(FMLJavaModLoadingContext context) {
        VoxelMapForgeMod.modBusGroup = context.getModBusGroup();
        ForgeSettingsPacketHandler.register();
        ForgeWorldIdPacketHandler.register();
        new VoxelmapForgeServer().init();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ForgeClientBootstrap.init(context);
        }
    }

    public static BusGroup getModBusGroup() {
        return modBusGroup;
    }
}
