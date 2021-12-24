package com.mamiyaotaru.voxelmap.fabricmod.mixins;

import com.mamiyaotaru.voxelmap.fabricmod.Share;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldRenderer.class)
public class ONMixinWorldRenderer {
   @Redirect(
      method = {"renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Matrix4f;FLjava/lang/Runnable;)V"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/util/math/MatrixStack;multiply(Lnet/minecraft/util/math/Quaternion;)V"
)
   )
   private void onRotate(MatrixStack matrixStack, Quaternion quat) {
      if (Share.isOldNorth()) {
         if (quat.equals(Vec3f.POSITIVE_X.getDegreesQuaternion(90.0F))) {
            return;
         }

         if (quat.equals(Vec3f.POSITIVE_Y.getDegreesQuaternion(-90.0F))) {
            return;
         }
      }

      matrixStack.multiply(quat);
   }
}
