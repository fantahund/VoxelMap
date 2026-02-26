package com.mamiyaotaru.voxelmap.mixins;

import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Gui.class)
public class MixinInGameHud {

    // this method: private void displayScoreboardSidebar(GuiGraphics guiGraphics, Objective objective)
    // this variable: int o = guiGraphics.guiHeight() / 2 + n / 3;
    //
    // entriesHeight is: int n = m * 9;

    // FIXME 26.1
//    @ModifyVariable(method = "displayScoreboardSidebar", at = @At("STORE"), ordinal = 6)
//    private int injected(int bottomX, @Local(ordinal = 5) int entriesHeight) {
//        return VoxelConstants.moveScoreboard(bottomX, entriesHeight);
//    }
}
