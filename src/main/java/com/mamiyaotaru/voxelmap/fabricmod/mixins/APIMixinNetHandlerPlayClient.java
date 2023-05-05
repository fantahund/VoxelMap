package com.mamiyaotaru.voxelmap.fabricmod.mixins;

import com.mamiyaotaru.voxelmap.fabricmod.FabricModVoxelMap;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayNetworkHandler.class)
public class APIMixinNetHandlerPlayClient {
    @Inject(method = "onCustomPayload(Lnet/minecraft/network/packet/s2c/play/CustomPayloadS2CPacket;)V", at = @At("HEAD"), cancellable = true)
    private void onHandleCustomPayload(CustomPayloadS2CPacket packet, CallbackInfo ci) {
        if (FabricModVoxelMap.instance.handleCustomPayload(packet)) {
            ci.cancel();
        }
    }

    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    public void onSendChatMessage(String command, CallbackInfoReturnable<Boolean> cir) {
        if (!FabricModVoxelMap.instance.onSendChatMessage(command)) {
            cir.cancel();
        }
    }
}
