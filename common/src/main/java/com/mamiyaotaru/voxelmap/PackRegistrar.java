package com.mamiyaotaru.voxelmap;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public interface PackRegistrar {
    public void registerPack(Identifier location, Component displayName);
}
