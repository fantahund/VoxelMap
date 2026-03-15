package com.mamiyaotaru.voxelmap.mixins;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.GuiSubworldsSelect;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MixinCamera {
    @Unique private final Minecraft minecraft = VoxelConstants.getMinecraft();
    @Unique private float yaw;

    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void move(float x, float y, float z);
    @Shadow public abstract @Nullable Entity entity();

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void afterCameraSetup(float partialTicks, CallbackInfo ci) {
        if (!(minecraft.screen instanceof GuiSubworldsSelect)) {
            yaw = entity().getViewYRot(partialTicks);
        } else {
            float frameDelta = minecraft.getDeltaTracker().getRealtimeDeltaTicks();
            float speed = 5.0F * frameDelta;
            yaw += speed * (1.0F + 0.7F * Mth.cos((yaw + 45.0) / 45.0 * Math.PI));
            setRotation(yaw, 0.0F);

            float radius = 0.475F;
            move(radius, 0.0F, 0.0F);
        }
    }
}
