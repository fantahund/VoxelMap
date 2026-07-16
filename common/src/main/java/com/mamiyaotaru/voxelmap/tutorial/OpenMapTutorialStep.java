package com.mamiyaotaru.voxelmap.tutorial;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.client.tutorial.TutorialStepInstance;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.network.chat.Component;

public final class OpenMapTutorialStep implements TutorialStepInstance {
    private static final int DISPLAY_DELAY_TICKS = 5 * 20;
    // private static final int DISPLAY_TIME_MILLISECONDS = 60 * 1000;

    private final Tutorial tutorial;
    private TutorialToast toast;
    private int ticks;

    public OpenMapTutorialStep(Tutorial tutorial) {
        this.tutorial = tutorial;
        ModTutorialSignals.reset();
    }

    @Override
    public void tick() {
        Minecraft minecraft = tutorial.getMinecraft();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        ticks++;
        if (ticks == DISPLAY_DELAY_TICKS) {
            showToast(minecraft);
        }

        if (ModTutorialSignals.consumeMenuOpened()) {
            complete();
        }
    }

    private void showToast(Minecraft minecraft) {
        MapSettingsManager options = VoxelConstants.getVoxelMapInstance().getMapOptions();
        Component keyName = options.keyBindMenu.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.YELLOW);
        toast = new TutorialToast(
                minecraft.font,
                TutorialToast.Icons.RECIPE_BOOK,
                Component.translatable("tutorial.voxelmap.open_menu.title"),
                Component.translatable("tutorial.voxelmap.open_menu.description", keyName),
                false);
        minecraft.gui.toastManager().addToast(toast);
    }

    private void complete() {
        VoxelConstants.getVoxelMapInstance().getMapOptions().completeWelcomeTutorial();
        tutorial.setStep(TutorialSteps.NONE);
    }

    @Override
    public void clear() {
        ModTutorialSignals.reset();
        if (toast != null) {
            toast.hide();
            toast = null;
        }
    }
}
