package com.mamiyaotaru.voxelmap.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class MixinInGameHud {
    @Inject(method = "renderBossOverlay", at = @At("TAIL"))
    private void injectRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        // Flush previous render layers before drawing the VoxelMap overlay.
        GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
        gameRenderer.guiRenderer.render(gameRenderer.fogRenderer.getBuffer(FogRenderer.FogMode.NONE));

        VoxelConstants.renderOverlay(guiGraphics);
    }

    // this method: private void displayScoreboardSidebar(GuiGraphics guiGraphics, Objective objective)
    // this variable: int o = guiGraphics.guiHeight() / 2 + n / 3;
    //
    // entriesHeight is: int n = m * 9;

    @ModifyVariable(method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V", at = @At("STORE"), ordinal = 6)
    private int injected(int bottomX, @Local(ordinal = 5) int entriesHeight) {
        return VoxelConstants.moveScoreboard(bottomX, entriesHeight);
    }
}
