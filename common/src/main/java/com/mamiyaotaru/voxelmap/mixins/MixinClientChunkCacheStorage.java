package com.mamiyaotaru.voxelmap.mixins;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.persistent.PersistentMap;
import java.util.concurrent.atomic.AtomicReferenceArray;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Handles slot collisions before ClientChunkCache replaces the old chunk reference. */
@Mixin(targets = "net.minecraft.client.multiplayer.ClientChunkCache$Storage")
public abstract class MixinClientChunkCacheStorage {
    @Shadow @Final private AtomicReferenceArray<LevelChunk> chunks;

    @Inject(method = "replace", at = @At("HEAD"))
    private void voxelmap$captureBeforeReplace(int index, LevelChunk newChunk, CallbackInfo ci) {
        LevelChunk oldChunk = this.chunks.get(index);
        if (oldChunk != null && oldChunk != newChunk) {
            try {
                PersistentMap persistentMap = VoxelConstants.getVoxelMapInstance().getPersistentMap();
                if (persistentMap != null) {
                    persistentMap.capturePendingChunkBeforeUnload(oldChunk);
                }
            } catch (RuntimeException exception) {
                VoxelConstants.getLogger().error(
                        "Failed to capture persistent-map data before replacing chunk {},{}",
                        oldChunk.getPos().x(),
                        oldChunk.getPos().z(),
                        exception);
            }
        }
    }
}
