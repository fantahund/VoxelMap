package com.mamiyaotaru.voxelmap.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod("voxelmap")
public class VoxelmapNeoForgeMod {

    private static IEventBus modEventBus;

    public VoxelmapNeoForgeMod(IEventBus modEventBus, ModContainer container) {
        VoxelmapNeoForgeMod.modEventBus = modEventBus;
        modEventBus.addListener(NeoForgePayloads::register);

        if (FMLEnvironment.getDist().isClient()) {
            ClientInit.init(container);
        }
        if (FMLEnvironment.getDist().isDedicatedServer()) {
            new VoxelmapNeoForgeServer().init();
        }
    }

    public static IEventBus getModEventBus() {
        return modEventBus;
    }

    private static final class ClientInit {
        private ClientInit() {
        }

        private static void init(ModContainer container) {
            NeoForgeClientBootstrap.init(container);
        }
    }
}
