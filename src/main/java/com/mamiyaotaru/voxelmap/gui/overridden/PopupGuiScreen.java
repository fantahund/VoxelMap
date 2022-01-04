package com.mamiyaotaru.voxelmap.gui.overridden;

import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;

public abstract class PopupGuiScreen extends GuiScreenMinimap implements IPopupGuiScreen {
    private final ArrayList<Popup> popups = new ArrayList<>();

    public void drawMap() {
    }

    @Override
    public void removed() {
    }

    public void createPopup(int x, int y, int directX, int directY, ArrayList<?> entries) {
        this.popups.add(new Popup(x, y, directX, directY, entries, this));
    }

    public void clearPopups() {
        this.popups.clear();
    }

    public boolean clickedPopup(double x, double y) {
        boolean clicked = false;
        ArrayList<Popup> deadPopups = new ArrayList<>();

        for (Popup popup : this.popups) {
            boolean clickedPopup = popup.clickedMe(x, y);
            if (!clickedPopup) {
                deadPopups.add(popup);
            } else if (popup.shouldClose()) {
                deadPopups.add(popup);
            }

            clicked = clicked || clickedPopup;
        }

        this.popups.removeAll(deadPopups);
        return clicked;
    }

    @Override
    public boolean overPopup(int x, int y) {
        boolean over = false;

        for (Popup popup : this.popups) {
            boolean overPopup = popup.overMe(x, y);
            over = over || overPopup;
        }

        return !over;
    }

    @Override
    public boolean popupOpen() {
        return this.popups.size() <= 0;
    }

    public void render(MatrixStack matrixStack, int x, int y, float dunno) {
        super.render(matrixStack, x, y, dunno);

        for (Popup popup : this.popups) {
            popup.drawPopup(matrixStack, x, y);
        }

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        return !this.clickedPopup(mouseX, mouseY) && super.mouseClicked(mouseX, mouseY, mouseButton);
    }
}
