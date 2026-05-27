package com.mamiyaotaru.voxelmap.mixins;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraft.client.SectionUpdateTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionUpdateTracker.class)
public abstract class MixinSectionUpdateTracker {
    @Inject(method = "setDirty(IIIZ)V", at = @At("RETURN"))
    public void postScheduleChunkRender(int sectionX, int sectionY, int sectionZ, boolean playerChanged, CallbackInfo ci) {
        if (VoxelConstants.getVoxelMapInstance().getWorldUpdateListener() != null) {
            VoxelConstants.getVoxelMapInstance().getWorldUpdateListener().notifyObservers(sectionX, sectionZ);
        }
    }
}
