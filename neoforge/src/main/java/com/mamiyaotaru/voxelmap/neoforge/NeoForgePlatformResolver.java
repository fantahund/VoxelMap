package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.PlatformResolver;
import com.mojang.blaze3d.opengl.GlTexture;
import net.neoforged.neoforge.client.blaze3d.validation.ValidationGpuTexture;

public class NeoForgePlatformResolver {
    public NeoForgePlatformResolver() {
        PlatformResolver.registerResolver(PlatformResolver.ResolverType.GPU_TEXTURE_TO_GL_TEXTURE, input -> {
            if (input instanceof ValidationGpuTexture validationTexture) {
                return (GlTexture) validationTexture.getRealTexture();
            }
            return (GlTexture) input;
        });

        PlatformResolver.validate();
    }
}
