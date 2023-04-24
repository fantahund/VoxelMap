package com.mamiyaotaru.voxelmap.fabricmod.mixins;

import com.mamiyaotaru.voxelmap.fabricmod.FabricModVoxelMap;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class APIMixinInGameHud {
    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;F)V", at = @At("RETURN"))
    private void onRenderGameOverlay(DrawContext drawContext, float tickDelta, CallbackInfo ci) {
        FabricModVoxelMap.instance.renderOverlay(drawContext);
    }
}
