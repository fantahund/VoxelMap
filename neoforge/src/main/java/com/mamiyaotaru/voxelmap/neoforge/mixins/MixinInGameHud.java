package com.mamiyaotaru.voxelmap.neoforge.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.mamiyaotaru.voxelmap.Map;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Gui.class)
public class MixinInGameHud {

    // this method: private void renderScoreboardSidebar(DrawContext context, ScoreboardObjective objective)
    // this lambda: context.draw(() -> {
    // this variable: int m = context.getScaledWindowHeight() / 2 + l / 3;
    //
    // 3566: net/minecraft/client/gui/hud/InGameHud.method_55440([Lnet/minecraft/client/gui/hud/InGameHud$SidebarEntry;Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/text/Text;I)V,
    // 6542: private synthetic method_55440([Lnet/minecraft/client/gui/hud/InGameHud$SidebarEntry;Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/text/Text;I)V
    //
    // entriesHeight is: int l = k * this.getTextRenderer().fontHeight;

    //FIXME @Brokkonat
    /*@ModifyVariable(method = "method_55440([Lnet/minecraft/client/gui/Gui$1DisplayEntry;Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/network/chat/Component;I)V", at = @At("STORE"), ordinal = 4)
    private int injected(int bottomX, @Local(ordinal = 3) int entriesHeight) {
        return VoxelConstants.moveScoreboard(bottomX, entriesHeight);
    }*/
}
