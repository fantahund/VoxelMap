package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(value = VoxelConstants.MOD_ID)
public class VoxelMapNeoForgeMod {

    private static IEventBus modEventBus;

    public VoxelMapNeoForgeMod(IEventBus modEventBus, ModContainer container) {
        VoxelMapNeoForgeMod.modEventBus = modEventBus;
        new VoxelMapNeoForgeServer().init();

        if (FMLEnvironment.getDist().isClient()) {
            VoxelConstants.setModVersion(container.getModInfo().getVersion().toString());
            VoxelConstants.setEvents(new NeoForgeEvents());
            VoxelConstants.setPacketBridge(new NeoForgePacketBridge());
            VoxelConstants.setModApiBride(new NeoForgeModApiBridge());
        }
    }

    public static IEventBus getModEventBus() {
        return modEventBus;
    }
}
