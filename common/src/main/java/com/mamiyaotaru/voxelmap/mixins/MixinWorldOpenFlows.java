package com.mamiyaotaru.voxelmap.mixins;

import com.mamiyaotaru.voxelmap.gui.WorldLoadMigrationHook;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldOpenFlows.class)
public class MixinWorldOpenFlows {
    @Inject(method = "openWorld(Ljava/lang/String;Ljava/lang/Runnable;)V", at = @At("HEAD"), cancellable = true)
    private void voxelmap$onOpenWorld(String levelId, Runnable onFail, CallbackInfo ci) {
        if (WorldLoadMigrationHook.interceptWorldLoad((WorldOpenFlows) (Object) this, levelId, onFail)) {
            ci.cancel();
        }
    }
}
