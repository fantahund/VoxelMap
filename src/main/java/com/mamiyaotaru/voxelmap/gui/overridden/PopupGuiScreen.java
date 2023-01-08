package com.mamiyaotaru.voxelmap.gui.overridden;

import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;

public class PopupGuiScreen extends GuiScreenMinimap implements IPopupGuiScreen {
    private final ArrayList<Popup> popups = new ArrayList<>();

    @Override
    public void removed() {}

    public void createPopup(int x, int y, int directX, int directY, ArrayList<Popup.PopupEntry> entries) {
        this.popups.add(new Popup(x, y, directX, directY, entries, this));
    }

    public void clearPopups() { this.popups.clear(); }

    public boolean clickedPopup(double x, double y) {
        ArrayList<Popup> deadPopups = new ArrayList<>();
        boolean clicked = false;

        for (Popup popup : popups) {
            boolean clickedPopup = popup.clickedMe(x, y);
            if (!(clickedPopup) || popup.shouldClose()) deadPopups.add(popup);

            clicked = clicked || clickedPopup;
        }

        popups.removeAll(deadPopups);

        return clicked;
    }

    @Override
    public boolean overPopup(int mouseX, int mouseY) {
        boolean over = false;

        for (Popup popup : popups) {
            boolean overPopup = popup.overMe(mouseX, mouseY);
            over = over || overPopup;
        }

        return (!(over));
    }

    @Override
    public boolean popupOpen() { return this.popups.isEmpty(); }

    @Override
    public void popupAction(Popup popup, int action) {}

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
        popups.forEach(popup -> popup.drawPopup(matrices, mouseX, mouseY));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) { return (!(clickedPopup(mouseX, mouseY) && super.mouseClicked(mouseX, mouseY, button))); }
}