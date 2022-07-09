package com.mamiyaotaru.voxelmap.fabricmod.mixins;

import com.mamiyaotaru.voxelmap.fabricmod.FabricModVoxelMap;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class APIMixinClientPlayerEntity extends AbstractClientPlayerEntity {
    public APIMixinClientPlayerEntity() {
        super( null, null, null);
    }

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    public void onSendChatMessage(String message, Text preview, CallbackInfo ci) {
        if (!FabricModVoxelMap.instance.onSendChatMessage(message)) {
            ci.cancel();
        }
    }
}
