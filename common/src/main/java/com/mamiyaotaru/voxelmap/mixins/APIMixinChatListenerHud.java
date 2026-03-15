package com.mamiyaotaru.voxelmap.mixins;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public class APIMixinChatListenerHud {
    @ModifyVariable(method = "addMessage", at = @At("HEAD"), ordinal = 0)
    public Component modifyChat(Component message) {
        return VoxelConstants.getModifiedChatMessage(message);
    }
}
