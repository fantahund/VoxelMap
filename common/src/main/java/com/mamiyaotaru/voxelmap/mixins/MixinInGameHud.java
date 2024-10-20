package com.mamiyaotaru.voxelmap.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Gui.class)
public class MixinInGameHud {

    // this method: private void displayScoreboardSidebar(GuiGraphics guiGraphics, Objective objective)
    // this variable: int o = guiGraphics.guiHeight() / 2 + n / 3;
    //
    // entriesHeight is: int n = m * 9;

    @ModifyVariable(method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V", at = @At("STORE"), ordinal = 6)
    private int injected(int bottomX, @Local(ordinal = 5) int entriesHeight) {
        return VoxelConstants.moveScoreboard(bottomX, entriesHeight);
    }
}
