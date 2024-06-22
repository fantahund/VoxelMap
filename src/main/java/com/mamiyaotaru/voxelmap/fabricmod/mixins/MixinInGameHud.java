package com.mamiyaotaru.voxelmap.fabricmod.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.mamiyaotaru.voxelmap.Map;
import com.mamiyaotaru.voxelmap.VoxelMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(InGameHud.class)
public class MixinInGameHud {

    // this method: private void renderScoreboardSidebar(DrawContext context, ScoreboardObjective objective)
    // this lambda: context.draw(() -> {
    // this variable: int m = context.getScaledWindowHeight() / 2 + l / 3;
    //
    // 3566: net/minecraft/client/gui/hud/InGameHud.method_55440([Lnet/minecraft/client/gui/hud/InGameHud$SidebarEntry;Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/text/Text;I)V,
    // 6542: private synthetic method_55440([Lnet/minecraft/client/gui/hud/InGameHud$SidebarEntry;Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/text/Text;I)V
    //
    // entriesHeight is: int l = k * this.getTextRenderer().fontHeight;

    @ModifyVariable(method = "method_55440([Lnet/minecraft/client/gui/hud/InGameHud$SidebarEntry;Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/text/Text;I)V", at = @At("STORE"), ordinal = 4)
    private int injected(int bottomX, @Local(ordinal = 3) int entriesHeight) {
        if (VoxelMap.mapOptions.moveScoreBoardDown) {
            double unscaledHeight = Map.getMinTablistOffset(); // / scaleFactor;
            if (VoxelMap.mapOptions.hide || VoxelMap.mapOptions.mapCorner != 1 || !Double.isFinite(unscaledHeight)) {
                return bottomX;
            }
            double scaleFactor = MinecraftClient.getInstance().getWindow().getScaleFactor(); // 1x 2x 3x, ...
            double mapHeightScaled = unscaledHeight * 1.37 / scaleFactor; // * 1.37 weil unscaledHeight nur die map selbst ist ohne text aussen und abstand oben

            int fontHeight = ((InGameHud) (Object) this).getTextRenderer().fontHeight; // height of the title line
            float statusIconOffset = Map.getStatusIconOffset();
            int statusIconOffsetInt = Float.isFinite(statusIconOffset) ? (int) statusIconOffset : 0;
            int minBottom = (int) (mapHeightScaled + entriesHeight + fontHeight + statusIconOffsetInt);

            return Math.max(bottomX, (int) minBottom);
        }
        return bottomX;
    }
}
