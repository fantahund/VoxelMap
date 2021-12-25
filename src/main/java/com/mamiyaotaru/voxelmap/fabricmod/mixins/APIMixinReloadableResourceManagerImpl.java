package com.mamiyaotaru.voxelmap.fabricmod.mixins;

import com.mamiyaotaru.voxelmap.VoxelMap;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceReload;
import net.minecraft.resource.ZipResourcePack;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ReloadableResourceManagerImpl.class)
public abstract class APIMixinReloadableResourceManagerImpl {
    @Shadow
    public abstract void addPack(ResourcePack pack);

    private ZipResourcePack zipPack;

    @Inject(method = "reload(Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;Ljava/util/concurrent/CompletableFuture;Ljava/util/List;)Lnet/minecraft/resource/ResourceReload;", at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/ReloadableResourceManagerImpl;clear()V", shift = Shift.AFTER))
    public void reload(Executor prepareExecutor, Executor applyExecutor, CompletableFuture<Unit> initialStage, List<ResourcePack> packs, CallbackInfoReturnable<ResourceReload> cir) {
        if (this.zipPack == null) {
            CodeSource src = VoxelMap.class.getProtectionDomain().getCodeSource();
            if (src != null) {
                URL jar = src.getLocation();

                try {
                    File pack = new File(jar.toURI());
                    this.zipPack = new ZipResourcePack(pack);
                } catch (URISyntaxException ignored) {
                }
            }
        }

        if (this.zipPack != null) {
            this.addPack(this.zipPack);
        }

    }
}
