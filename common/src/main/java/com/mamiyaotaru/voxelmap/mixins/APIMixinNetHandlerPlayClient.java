package com.mamiyaotaru.voxelmap.mixins;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class APIMixinNetHandlerPlayClient {
    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    public void onSendCommand(String command, CallbackInfo ci) {
        if (voxelmap$parseCommand(command)) {
            ci.cancel();
        }
    }

    @Inject(method = "sendUnattendedCommand", at = @At("HEAD"), cancellable = true)
    public void onUnsignedCommand(String command, Screen screenAfterCommand, CallbackInfo ci) {
        if (voxelmap$parseCommand(command)) {
            ci.cancel();
        }
    }


    @Unique
    private boolean voxelmap$parseCommand(String command) {
        VoxelConstants.getLogger().info("Command: " + command);
        return !VoxelConstants.onSendChatMessage(command);
    }
}
