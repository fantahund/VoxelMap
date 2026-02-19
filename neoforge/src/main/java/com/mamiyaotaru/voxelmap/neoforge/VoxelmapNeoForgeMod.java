package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.PlatformResolver;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.blaze3d.validation.ValidationGpuDevice;
import net.neoforged.neoforge.client.blaze3d.validation.ValidationGpuTexture;

@Mod(value = "voxelmap", dist = Dist.CLIENT)
public class VoxelmapNeoForgeMod {

    private static IEventBus modEventBus;

    public VoxelmapNeoForgeMod(IEventBus modEventBus, ModContainer container) {
        VoxelmapNeoForgeMod.modEventBus = modEventBus;
        VoxelConstants.setModVersion(container.getModInfo().getVersion().toString());
        VoxelConstants.setEvents(new NeoForgeEvents());
        VoxelConstants.setPacketBridge(new NeoForgePacketBridge());
        registerResolvers();
        PlatformResolver.validate();
    }

    public static IEventBus getModEventBus() {
        return modEventBus;
    }

    private void registerResolvers() {
        PlatformResolver.registerResolver(PlatformResolver.ResolverType.GPU_DEVICE_TO_GL_DEVICE, input -> {
            if (input instanceof ValidationGpuDevice validationDevice) {
                return (GlDevice) validationDevice.getRealDevice();
            }
            return (GlDevice) input;
        });

        PlatformResolver.registerResolver(PlatformResolver.ResolverType.GPU_TEXTURE_TO_GL_TEXTURE, input -> {
            if (input instanceof ValidationGpuTexture validationTexture) {
                return (GlTexture) validationTexture.getRealTexture();
            }
            return (GlTexture) input;
        });
    }
}
