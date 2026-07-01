package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.gui.widgets.IOptionWidget;
import com.mamiyaotaru.voxelmap.options.fields.OptionField;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

public abstract class GuiOptionsScreenMinimap extends GuiScreenMinimap {
    protected final VoxelMap voxelMap = VoxelConstants.getVoxelMapInstance();
    private final ArrayList<AbstractWidget> optionWidgets = new ArrayList<>();

    protected GuiOptionsScreenMinimap(Screen parentGui, Component title) {
        super(parentGui, title);
    }

    public void rebuildOptionWidgets() {
        clearOptionWidgets();
        setupOptionWidgets();
        refreshOptionWidgets();
    }

    protected abstract void setupOptionWidgets();

    private void refreshOptionWidgets() {
        voxelMap.getOptionsManager().updateOptionsActive();

        for (AbstractWidget widget : optionWidgets) {
            if (widget instanceof IOptionWidget widget2) {
                widget2.refresh();
            }
        }
    }

    protected void clearOptionWidgets() {
        for (AbstractWidget widget : optionWidgets) {
            removeWidget(widget);
        }
        optionWidgets.clear();
    }

    protected void addOptionWidget(AbstractWidget widget) {
        addRenderableWidget(widget);
        optionWidgets.add(widget);
    }

    protected void removeOptionWidget(AbstractWidget widget) {
        removeWidget(widget);
        optionWidgets.remove(widget);
    }

    protected AbstractWidget createOptionWidget(OptionField<?> option, int x, int y, int width, int height) {
        return option.createWidget(x, y, width, height, (e) -> refreshOptionWidgets());
    }
}
