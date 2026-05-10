package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionsScreenMinimap;
import com.mamiyaotaru.voxelmap.options.fields.OptionField;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GuiWaypointsOptions extends GuiOptionsScreenMinimap {
    private final OptionField<?>[] relevantOptions;

    public GuiWaypointsOptions(Screen parentGui) {
        super(parentGui, Component.translatable("options.minimap.waypoints.title"));

        relevantOptions = new OptionField[] {
                waypointOptions.maxDistance,
                waypointOptions.signScale,
                waypointOptions.deathpoints,
                waypointOptions.unitConversion,
                waypointOptions.labelStyle,
                waypointOptions.highlightFocused
        };
    }

    @Override
    public void init() {
        addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), button -> onClose()).bounds(getWidth() / 2 - 100, getHeight() - 26, 200, 20).build());
        rebuildOptionWidgets();
    }

    @Override
    protected void setupOptionWidgets() {
        int i = 0;
        for (OptionField<?> option : relevantOptions) {
            int x = getWidth() / 2 - 155 + i % 2 * 160;
            int y = getHeight() / 6 + 24 * (i >> 1);
            AbstractWidget widget = createOptionWidget(option, x, y, 150, 20);

            addOptionWidget(widget);

            ++i;
        }
    }
}
