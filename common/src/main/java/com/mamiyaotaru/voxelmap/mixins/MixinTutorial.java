package com.mamiyaotaru.voxelmap.mixins;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.tutorial.OpenMapTutorialStep;
import net.minecraft.client.Minecraft;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.client.tutorial.TutorialStepInstance;
import net.minecraft.client.tutorial.TutorialSteps;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Tutorial.class)
public abstract class MixinTutorial {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private TutorialStepInstance instance;

    @Inject(method = "start", at = @At("TAIL"))
    private void voxelmap$startOpenMapTutorial(CallbackInfo ci) {
        voxelmap$installOpenMapTutorial();
    }

    @Inject(method = "setStep", at = @At("TAIL"))
    private void voxelmap$startOpenMapTutorialAfterVanilla(TutorialSteps step, CallbackInfo ci) {
        if (step == TutorialSteps.NONE) {
            voxelmap$installOpenMapTutorial();
        }
    }

    private void voxelmap$installOpenMapTutorial() {
        if (minecraft.options.tutorialStep != TutorialSteps.NONE
                || !VoxelConstants.getVoxelMapInstance().getMapOptions().isWelcomeTutorialPending()
                || instance instanceof OpenMapTutorialStep) {
            return;
        }

        if (instance != null) {
            instance.clear();
        }
        instance = new OpenMapTutorialStep((Tutorial) (Object) this);
    }
}
