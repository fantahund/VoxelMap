package com.mamiyaotaru.voxelmap.fabricmod.mixins;

import com.mamiyaotaru.voxelmap.fabricmod.FabricModVoxelMap;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayNetworkHandler.class)
public class APIMixinNetHandlerPlayClient {
    /*@Inject(method = "onCustomPayload", at = @At("HEAD"), cancellable = true)
    private void onHandleCustomPayload(class_8710 arg, CallbackInfo ci) {
        if (FabricModVoxelMap.instance.handleCustomPayload(arg)) {
            ci.cancel();
        }
    }*/

    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    public void onSendChatMessage(String command, CallbackInfoReturnable<Boolean> cir) {
        if (!FabricModVoxelMap.instance.onSendChatMessage(command)) {
            cir.cancel();
        }
    }
}
