package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(VoxelConstants.MOD_ID)
public class VoxelMapNeoForgeMod {

    private static IEventBus modEventBus;

    public VoxelMapNeoForgeMod(IEventBus modEventBus, ModContainer container) {
        VoxelMapNeoForgeMod.modEventBus = modEventBus;
        modEventBus.addListener(NeoForgePacketHandler::initCommon);

        if (FMLEnvironment.getDist().isClient()) {
            VoxelMapNeoForgeClientMod.init(container);
        }
        if (FMLEnvironment.getDist().isDedicatedServer()) {
            new VoxelMapNeoForgeServerMod().init();
        }
    }

    public static IEventBus getModEventBus() {
        return modEventBus;
    }
}
