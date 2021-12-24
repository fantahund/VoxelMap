package com.mamiyaotaru.voxelmap.fabricmod.mixins;

import com.mamiyaotaru.voxelmap.fabricmod.FabricModVoxelMap;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class APIMixinClientPlayerEntity extends AbstractClientPlayerEntity {
   public APIMixinClientPlayerEntity() {
      super((ClientWorld)null, (GameProfile)null);
   }

   @Inject(method = "sendChatMessage(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
   public void onSendChatMessage(String message, CallbackInfo ci) {
      if (!FabricModVoxelMap.instance.onSendChatMessage(message)) {
         ci.cancel();
      }

   }
}
