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
        super( null, null);
    }

    @Inject(method = "sendMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
    public void onSendChatMessage(Text message, CallbackInfo cir) {
        //if (!FabricModVoxelMap.instance.onSendChatMessage(command)) {
        //    cir.cancel();
        //}
        FabricModVoxelMap.instance.onSendChatMessage(message.getString()); // TODO make better fix
    }
}
