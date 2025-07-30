package com.mamiyaotaru.voxelmap.gui.overridden;

public interface IPopupGuiScreen {
    boolean overPopup(int mouseX, int mouseY);

    boolean popupOpen();

    void popupAction(Popup popup, int action);

    boolean mouseClicked(double mouseX, double mouseY, int button, boolean doubleClick);
}