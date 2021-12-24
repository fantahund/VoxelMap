package com.mamiyaotaru.voxelmap.fabricmod.mixins;

import com.mamiyaotaru.voxelmap.fabricmod.FabricModVoxelMap;
import java.util.UUID;
import net.minecraft.network.MessageType;
import net.minecraft.text.Text;
import net.minecraft.client.gui.hud.ChatHudListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHudListener.class)
public class APIMixinChatListenerHud {
   @Inject(method = "onChatMessage(Lnet/minecraft/network/MessageType;Lnet/minecraft/text/Text;Ljava/util/UUID;)V", at = @At("HEAD"), cancellable = true)
   public void postSay(MessageType type, Text textComponent, UUID uuid, CallbackInfo ci) {
      if (!FabricModVoxelMap.instance.onChat(textComponent)) {
         ci.cancel();
      }

   }
}
