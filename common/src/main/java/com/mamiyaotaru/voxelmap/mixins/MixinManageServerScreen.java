package com.mamiyaotaru.voxelmap.mixins;

import com.mamiyaotaru.voxelmap.gui.GuiServerAliases;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screens.ManageServerScreen")
public abstract class MixinManageServerScreen extends Screen {
    @Shadow
    @Final
    private ServerData serverData;

    private MixinManageServerScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void voxelmap$addAliasButton(CallbackInfo ci) {
        int buttonWidth = 120;
        int x = this.width - buttonWidth - 8;
        int y = this.height - 28;
        this.addRenderableWidget(new Button.Builder(Component.translatable("voxelmap.alias.editButton"),
                button -> this.minecraft.gui.setScreen(new GuiServerAliases(this, this.serverData.ip)))
                .bounds(x, y, buttonWidth, 20).build());
    }
}
