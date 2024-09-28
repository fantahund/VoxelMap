package com.mamiyaotaru.voxelmap.mixins;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class APIMixinNetHandlerPlayClient {
    /*@Inject(method = "onCustomPayload", at = @At("HEAD"), cancellable = true)
    private void onHandleCustomPayload(class_8710 arg, CallbackInfo ci) {
        if (FabricModVoxelMap.instance.handleCustomPayload(arg)) {
            ci.cancel();
        }
    }*/

    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    public void onSendChatMessage(String string, CallbackInfo cir) {
        if (!VoxelConstants.onSendChatMessage(string)) {
            cir.cancel();
        }
    }
}
