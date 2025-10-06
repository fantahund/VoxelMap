package com.mamiyaotaru.voxelmap.gui.overridden;

import net.minecraft.client.input.MouseButtonEvent;

public interface IPopupGuiScreen {
    boolean overPopup(int mouseX, int mouseY);

    boolean popupOpen();

    void popupAction(Popup popup, int action);

    boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick);
}