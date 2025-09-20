package com.mamiyaotaru.voxelmap.mixins;

import com.mamiyaotaru.voxelmap.persistent.GuiPersistentMap;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class MixinKeyboard {

    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At(value = "RETURN"))
    public void onKey(long l, int action, KeyEvent keyEvent, CallbackInfo ci) {
        if (this.minecraft.screen instanceof GuiPersistentMap guiPersistentMap && guiPersistentMap.passEvents) {
            InputConstants.Key key2 = InputConstants.getKey(keyEvent);
            if (action == 0) { //TODO 1.21.9
                KeyMapping.set(key2, false);
            } else {
                KeyMapping.set(key2, true);
                KeyMapping.click(key2);
            }
        }
    }

}
