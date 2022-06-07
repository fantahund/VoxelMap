package com.mamiyaotaru.voxelmap.fabricmod.mixins;

import com.mamiyaotaru.voxelmap.fabricmod.FabricModVoxelMap;
import net.minecraft.client.gui.hud.ChatHudListener;
import net.minecraft.network.message.MessageSender;
import net.minecraft.network.message.MessageType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHudListener.class)
public class APIMixinChatListenerHud {
    @Inject(method = "onChatMessage", at = @At("HEAD"), cancellable = true)
    public void postSay(MessageType type, Text textComponent, MessageSender uuid, CallbackInfo ci) {
        if (!FabricModVoxelMap.instance.onChat(textComponent)) {
            ci.cancel();
        }

    }
}
