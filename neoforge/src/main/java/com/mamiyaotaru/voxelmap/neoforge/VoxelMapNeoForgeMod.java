package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = VoxelConstants.MOD_ID, dist = Dist.CLIENT)
public class VoxelMapNeoForgeMod {

    private static IEventBus modEventBus;

    public VoxelMapNeoForgeMod(IEventBus modEventBus, ModContainer container) {
        VoxelMapNeoForgeMod.modEventBus = modEventBus;
        VoxelConstants.setModVersion(container.getModInfo().getVersion().toString());
        VoxelConstants.setEvents(new NeoForgeEvents());
        VoxelConstants.setPacketBridge(new NeoForgePacketBridge());
        VoxelConstants.setModApiBride(new NeoForgeModApiBridge());
    }

    public static IEventBus getModEventBus() {
        return modEventBus;
    }
}
