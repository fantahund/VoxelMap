package com.mamiyaotaru.voxelmap.mixins;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPacketListener.class)
public abstract class APIMixinNetHandlerPlayClient {
    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    public void onSendCommand(String string, CallbackInfo cir) {
        if (voxelmap$parseCommand(string)) {
            cir.cancel();
        }
    }

    @Inject(method = "sendUnattendedCommand", at = @At("HEAD"), cancellable = true)
    public void onUnsignedCommand(String string, boolean bl, CallbackInfo ci) {
        if (voxelmap$parseCommand(string)) {
            ci.cancel();
        }
    }


    @Unique
    private boolean voxelmap$parseCommand(String command) {
        VoxelConstants.getLogger().info("Command: " + command);
        return !VoxelConstants.onSendChatMessage(command);
    }
}
