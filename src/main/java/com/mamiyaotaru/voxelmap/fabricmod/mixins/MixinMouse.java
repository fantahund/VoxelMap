package com.mamiyaotaru.voxelmap.fabricmod.mixins;

import com.mamiyaotaru.voxelmap.persistent.GuiPersistentMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouse {


    @Shadow @Final private Minecraft minecraft;

    @Shadow private double xpos;

    @Shadow private double ypos;

    @Shadow private boolean isLeftPressed;

    @Inject(method = "onPress", at = @At(value = "RETURN"))
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (this.minecraft.screen instanceof GuiPersistentMap guiPersistentMap && button == 0) {
            double d = this.xpos * (double) this.minecraft.getWindow().getGuiScaledWidth() / (double) this.minecraft.getWindow().getScreenWidth();
            double e = this.ypos * (double) this.minecraft.getWindow().getGuiScaledHeight() / (double) this.minecraft.getWindow().getScreenHeight();
            if (!guiPersistentMap.mouseClicked(d, e, button) && (guiPersistentMap.passEvents && this.minecraft.getOverlay() == null)) this.isLeftPressed = action == 1;
        }
    }

}
