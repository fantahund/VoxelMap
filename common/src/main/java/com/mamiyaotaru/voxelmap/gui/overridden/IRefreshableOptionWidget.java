package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.options.fields.OptionField;
import net.minecraft.client.gui.components.AbstractWidget;

public interface IRefreshableOptionWidget {
    void refresh();

    void onUpdate();

    default void refreshWidget(AbstractWidget widget, OptionField<?> field) {
        widget.setMessage(field.getMessage());
        widget.setTooltip(field.getTooltip());
        widget.active = field.isActive() && field.isAllowed();
    }
}
