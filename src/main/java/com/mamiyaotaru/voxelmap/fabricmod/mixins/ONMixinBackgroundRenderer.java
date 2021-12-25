package com.mamiyaotaru.voxelmap.fabricmod.mixins;

import com.mamiyaotaru.voxelmap.fabricmod.Share;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BackgroundRenderer.class)
public class ONMixinBackgroundRenderer {
    @Redirect(method = {"render(Lnet/minecraft/client/render/Camera;FLnet/minecraft/client/world/ClientWorld;IF)V"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3f;dot(Lnet/minecraft/util/math/Vec3f;)F"))
    private static float onDotProduct(Vec3f vec3d, Vec3f arg) {
        if (Share.isOldNorth()) {
            arg = new Vec3f(0.0F, 0.0F, -arg.getX());
        }

        return vec3d.dot(arg);
    }
}
