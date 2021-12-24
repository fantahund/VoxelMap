package com.mamiyaotaru.voxelmap.fabricmod.mixins;

import com.mamiyaotaru.voxelmap.fabricmod.FabricModVoxelMap;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class APIMixinNetHandlerPlayClient {
   @Inject(method = "onCustomPayload(Lnet/minecraft/network/packet/s2c/play/CustomPayloadS2CPacket;)V", at = @At("HEAD"), cancellable = true)
   private void onHandleCustomPayload(CustomPayloadS2CPacket packet, CallbackInfo ci) {
      if (FabricModVoxelMap.instance.handleCustomPayload(packet)) {
         ci.cancel();
      }

   }
}
