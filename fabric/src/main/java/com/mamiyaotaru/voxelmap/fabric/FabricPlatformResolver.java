package com.mamiyaotaru.voxelmap.fabric;

import com.mamiyaotaru.voxelmap.PlatformResolver;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;

public class FabricPlatformResolver {
    public FabricPlatformResolver() {
        PlatformResolver.registerResolver(PlatformResolver.ResolverType.GPU_DEVICE_TO_GL_DEVICE, input -> (GlDevice) input);
        PlatformResolver.registerResolver(PlatformResolver.ResolverType.GPU_TEXTURE_TO_GL_TEXTURE, input -> (GlTexture) input);

        PlatformResolver.validate();
    }
}
