package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(value = "voxelmap")
public class VoxelmapNeoForgeMod {

    private static IEventBus modEventBus;

    public VoxelmapNeoForgeMod() {
        VoxelmapNeoForgeMod.modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        VoxelConstants.setEvents(new ForgeEvents());
        VoxelConstants.setPacketBridge(new NeoForgePacketBridge());
    }

    public static IEventBus getModEventBus() {
        return modEventBus;
    }
}
