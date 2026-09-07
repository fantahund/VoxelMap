package com.mamiyaotaru.voxelmap.mixins;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.persistent.PersistentMap;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures accepted persistent-map updates before the client invalidates their world data. */
@Mixin(ClientChunkCache.class)
public abstract class MixinClientChunkCache {
    @Inject(method = "drop", at = @At("HEAD"))
    private void voxelmap$captureBeforeDrop(ChunkPos pos, CallbackInfo ci) {
        ClientChunkCache chunkCache = (ClientChunkCache) (Object) this;
        LevelChunk chunk = chunkCache.getChunk(pos.x(), pos.z(), ChunkStatus.FULL, false);
        if (chunk != null) {
            voxelmap$capture(chunk);
        }
    }

    @Inject(method = "updateViewRadius", at = @At("HEAD"))
    private void voxelmap$captureBeforeStorageResize(int viewRange, CallbackInfo ci) {
        PersistentMap persistentMap = VoxelConstants.getVoxelMapInstance().getPersistentMap();
        if (persistentMap != null) {
            persistentMap.capturePendingChunksBeforeStorageChange();
        }
    }

    private static void voxelmap$capture(LevelChunk chunk) {
        try {
            PersistentMap persistentMap = VoxelConstants.getVoxelMapInstance().getPersistentMap();
            if (persistentMap != null) {
                persistentMap.capturePendingChunkBeforeUnload(chunk);
            }
        } catch (RuntimeException exception) {
            VoxelConstants.getLogger().error("Failed to capture persistent-map data before unloading chunk {},{}", chunk.getPos().x(), chunk.getPos().z(), exception);
        }
    }
}
