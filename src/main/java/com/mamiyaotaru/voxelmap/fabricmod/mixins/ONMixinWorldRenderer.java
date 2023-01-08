package com.mamiyaotaru.voxelmap.fabricmod.mixins;

import com.mamiyaotaru.voxelmap.fabricmod.Share;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldRenderer.class)
public class ONMixinWorldRenderer {
    @Redirect(method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;multiply(Lorg/joml/Quaternionf;)V"))
    private void onRotate(MatrixStack matrixStack, Quaternionf quat) {
        if (Share.isOldNorth()) {
            if (quat.equals(RotationAxis.POSITIVE_X.rotationDegrees(90.0F))) return;
            if (quat.equals(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F))) return;
        }

        matrixStack.multiply(quat);
    }
}
