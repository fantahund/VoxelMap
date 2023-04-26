package com.mamiyaotaru.voxelmap.fabricmod.mixins;

import com.mamiyaotaru.voxelmap.persistent.GuiPersistentMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MixinMouse {

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    private boolean leftButtonClicked;

    @Shadow
    private boolean middleButtonClicked;

    @Shadow
    private boolean rightButtonClicked;

    @Shadow
    private double x;

    @Shadow
    private double y;

    @Inject(method = "onMouseButton", at = @At(value = "RETURN"))
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        boolean bl = action == 1;
        boolean[] bls = new boolean[]{false};

        if (this.client.currentScreen instanceof GuiPersistentMap guiPersistentMap) {
            double d = this.x * (double) this.client.getWindow().getScaledWidth() / (double) this.client.getWindow().getWidth();
            double e = this.y * (double) this.client.getWindow().getScaledHeight() / (double) this.client.getWindow().getHeight();
            bls[0] = guiPersistentMap.mouseClicked(d, e, button);
            if (!bls[0] && (guiPersistentMap.passEvents && this.client.getOverlay() == null)) {
                if (button == 0) {
                    this.leftButtonClicked = bl;
                } else if (button == 2) {
                    this.middleButtonClicked = bl;
                } else if (button == 1) {
                    this.rightButtonClicked = bl;
                }
            }
        }
    }
}
