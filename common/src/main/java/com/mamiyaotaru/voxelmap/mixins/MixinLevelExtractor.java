package com.mamiyaotaru.voxelmap.mixins;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.WorldUpdateListener;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Covers the section-dirty calls that do not go through ClientLevel:
 * ClientChunkCache and ClientPacketListener call LevelExtractor.setSectionDirty(III)
 * directly when chunks are loaded or re-sent by the server. Sodium only overwrites
 * the private setSectionDirty(IIIZ) overload, so this public overload keeps firing
 * with and without Sodium installed.
 */
@Mixin(LevelExtractor.class)
public abstract class MixinLevelExtractor {
    @Inject(method = "setSectionDirty(III)V", at = @At("HEAD"))
    private void voxelmap$onSectionDirty(int sectionX, int sectionY, int sectionZ, CallbackInfo ci) {
        WorldUpdateListener listener = VoxelConstants.getVoxelMapInstance().getWorldUpdateListener();
        if (listener != null) {
            listener.notifyObservers(sectionX, sectionZ);
        }
    }
}
