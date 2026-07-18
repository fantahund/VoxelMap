package com.mamiyaotaru.voxelmap.mixins;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.WorldUpdateListener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Notifies VoxelMap of block changes at the ClientLevel entry points. These are
 * invoked by the packet handlers before the render pipeline takes over, so they
 * fire regardless of whether the vanilla LevelExtractor/SectionUpdateTracker or a
 * replacement render pipeline (e.g. Sodium, which overwrites the LevelExtractor
 * methods feeding the tracker) handles the update.
 */
@Mixin(ClientLevel.class)
public abstract class MixinClientLevel {
    @Inject(method = "sendBlockUpdated", at = @At("RETURN"))
    private void voxelmap$onBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
        voxelmap$notifyBlockArea(pos);
    }

    @Inject(method = "setBlocksDirty", at = @At("RETURN"))
    private void voxelmap$onBlocksDirty(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {
        voxelmap$notifyBlockArea(pos);
    }

    @Inject(method = "setSectionDirtyWithNeighbors", at = @At("RETURN"))
    private void voxelmap$onSectionDirtyWithNeighbors(int sectionX, int sectionY, int sectionZ, CallbackInfo ci) {
        WorldUpdateListener listener = VoxelConstants.getVoxelMapInstance().getWorldUpdateListener();
        if (listener != null) {
            for (int x = sectionX - 1; x <= sectionX + 1; ++x) {
                for (int z = sectionZ - 1; z <= sectionZ + 1; ++z) {
                    listener.notifyObservers(x, z);
                }
            }
        }
    }

    @Inject(method = "setSectionRangeDirty", at = @At("RETURN"))
    private void voxelmap$onSectionRangeDirty(int minSectionX, int minSectionY, int minSectionZ, int maxSectionX, int maxSectionY, int maxSectionZ, CallbackInfo ci) {
        WorldUpdateListener listener = VoxelConstants.getVoxelMapInstance().getWorldUpdateListener();
        if (listener != null) {
            for (int x = minSectionX; x <= maxSectionX; ++x) {
                for (int z = minSectionZ; z <= maxSectionZ; ++z) {
                    listener.notifyObservers(x, z);
                }
            }
        }
    }

    @Unique
    private static void voxelmap$notifyBlockArea(BlockPos pos) {
        WorldUpdateListener listener = VoxelConstants.getVoxelMapInstance().getWorldUpdateListener();
        if (listener == null) {
            return;
        }

        // include neighboring sections when the block sits on a section border, so
        // border pixels (e.g. slope shading) are repainted like they were with the
        // old SectionUpdateTracker hook
        int minSectionX = SectionPos.blockToSectionCoord(pos.getX() - 1);
        int maxSectionX = SectionPos.blockToSectionCoord(pos.getX() + 1);
        int minSectionZ = SectionPos.blockToSectionCoord(pos.getZ() - 1);
        int maxSectionZ = SectionPos.blockToSectionCoord(pos.getZ() + 1);

        for (int x = minSectionX; x <= maxSectionX; ++x) {
            for (int z = minSectionZ; z <= maxSectionZ; ++z) {
                listener.notifyObservers(x, z);
            }
        }
    }
}
