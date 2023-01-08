package com.mamiyaotaru.voxelmap.fabricmod.mixins;

import com.mamiyaotaru.voxelmap.VoxelMap;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class MixinChatHud {

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
    private void addMessage(Text message, CallbackInfo ci) {
        VoxelMap.checkPermissionMessages(message);
    }
}
