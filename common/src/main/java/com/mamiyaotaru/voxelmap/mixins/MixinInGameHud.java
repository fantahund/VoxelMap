package com.mamiyaotaru.voxelmap.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.mamiyaotaru.voxelmap.Map;
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

    /*@ModifyVariable(method = "method_55440([Lnet/minecraft/client/gui/Gui$1DisplayEntry;Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/network/chat/Component;I)V", at = @At("STORE"), ordinal = 4)
    private int injected(int bottomX, @Local(ordinal = 3) int entriesHeight) {
        double unscaledHeight = Map.getMinTablistOffset(); // / scaleFactor;
        if (VoxelMap.mapOptions.hide || !VoxelMap.mapOptions.minimapAllowed || VoxelMap.mapOptions.mapCorner != 1 || !VoxelMap.mapOptions.moveScoreBoardDown || !Double.isFinite(unscaledHeight)) {
            return bottomX;
        }
        double scaleFactor = Minecraft.getInstance().getWindow().getGuiScale(); // 1x 2x 3x, ...
        double mapHeightScaled = unscaledHeight * 1.37 / scaleFactor; // * 1.37 because unscaledHeight is just the map without the text around it

        int fontHeight = ((Gui) (Object) this).getFont().lineHeight; // height of the title line
        float statusIconOffset = Map.getStatusIconOffset();
        int statusIconOffsetInt = Float.isFinite(statusIconOffset) ? (int) statusIconOffset : 0;
        int minBottom = (int) (mapHeightScaled + entriesHeight + fontHeight + statusIconOffsetInt);

        return Math.max(bottomX, (int) minBottom);
    }*/
}
