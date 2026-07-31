package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.PackRegistrar;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class FabricPackRegistrar implements PackRegistrar {
    @Override
    public void registerPack(Identifier location, Component displayName) {
        FabricLoader.getInstance().getModContainer(VoxelConstants.MOD_ID).ifPresent(container -> {
            ResourceLoader.registerBuiltinPack(location, container, displayName, PackActivationType.NORMAL);
        });
    }
}
