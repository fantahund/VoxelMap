package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.VoxelmapSettingsChannelHandler;
import com.mamiyaotaru.voxelmap.VoxelmapWorldIdChannelHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = "voxelmap", dist = Dist.CLIENT)
public class VoxelmapNeoForgeMod {

    public VoxelmapNeoForgeMod(IEventBus bus, ModContainer modContainer) {
        new VoxelmapSettingsChannelHandler();
        new VoxelmapWorldIdChannelHandler();
    }
}
