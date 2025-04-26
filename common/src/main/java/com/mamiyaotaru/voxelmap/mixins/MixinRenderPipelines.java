package com.mamiyaotaru.voxelmap.mixins;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderPipelines.class)
public class MixinRenderPipelines {
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void voxelmap_onRegisterPipelines(CallbackInfo ci) {
        VoxelConstants.registerIrisPipelines();
    }
}
