package com.mamiyaotaru.voxelmap.fabricmod.mixins;

import com.mamiyaotaru.voxelmap.persistent.GuiPersistentMap;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class MixinKeyboard {

    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "onKey", at = @At(value = "RETURN"))
    public void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (this.client.currentScreen instanceof GuiPersistentMap guiPersistentMap && guiPersistentMap.passEvents) {
            InputUtil.Key key2 = InputUtil.fromKeyCode(key, scancode);
            if (action == 0) {
                KeyBinding.setKeyPressed(key2, false);
            } else {
                KeyBinding.setKeyPressed(key2, true);
                KeyBinding.onKeyPressed(key2);
            }
        }
    }

}
