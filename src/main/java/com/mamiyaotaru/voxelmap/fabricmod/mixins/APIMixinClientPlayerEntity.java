package com.mamiyaotaru.voxelmap.fabricmod.mixins;

import com.mamiyaotaru.voxelmap.fabricmod.FabricModVoxelMap;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class APIMixinClientPlayerEntity extends AbstractClientPlayerEntity {
    public APIMixinClientPlayerEntity() {
        super( null, null, null);
    }

    @Inject(method = "sendCommand(Ljava/lang/String;)Z", at = @At("HEAD"))
    public void onSendChatMessage(String command, CallbackInfoReturnable<Boolean> cir) {
        if (!FabricModVoxelMap.instance.onSendChatMessage(command)) {
            cir.cancel();
        }
    }
}
