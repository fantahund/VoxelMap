package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = "voxelmap", dist = Dist.CLIENT)
public class VoxelmapNeoForgeMod {

    private static IEventBus modEventBus;

    public VoxelmapNeoForgeMod(IEventBus modEventBus, ModContainer container) {
        VoxelmapNeoForgeMod.modEventBus = modEventBus;
        VoxelConstants.setEvents(new ForgeEvents());
    }

    public static IEventBus getModEventBus() {
        return modEventBus;
    }
}
