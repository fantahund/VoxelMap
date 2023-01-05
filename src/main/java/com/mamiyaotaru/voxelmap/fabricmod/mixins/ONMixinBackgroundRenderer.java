package com.mamiyaotaru.voxelmap.fabricmod.mixins;

import com.mamiyaotaru.voxelmap.fabricmod.Share;
import net.minecraft.client.render.BackgroundRenderer;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BackgroundRenderer.class)
public class ONMixinBackgroundRenderer {
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lorg/joml/Vector3f;dot(Lorg/joml/Vector3fc;)F"))
    private static float onDotProduct(Vector3f vec3d, Vector3fc arg) {
        if (Share.isOldNorth()) {
            arg = new Vector3f(0.0F, 0.0F, -arg.x());
        }

        return vec3d.dot(arg);
    }
}
