package com.mamiyaotaru.voxelmap.interfaces;

import net.minecraft.server.packs.resources.ResourceManager;

public interface IReloadListener {
    public void onResourceManagerReload(ResourceManager resourceManager);
}
