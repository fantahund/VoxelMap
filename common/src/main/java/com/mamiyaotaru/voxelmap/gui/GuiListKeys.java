package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiListMinimap;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class GuiListKeys extends GuiListMinimap<GuiListKeys.RowItem> {
    private final MapSettingsManager options;
    private final GuiMinimapControls parentGui;
    private final ArrayList<RowItem> rowItems = new ArrayList<>();
    private KeyMapping keyForEdit;
    private final HashMap<KeyMapping, Component> duplicateKeys = new HashMap<>();

    public GuiListKeys(GuiMinimapControls parentGui, int x, int y, int width, int height) {
        super(x, y, width, height, 20);

        this.parentGui = parentGui;
        options = VoxelConstants.getVoxelMapInstance().getMapOptions();

        for (int i = 0; i < options.keyBindings.length; ++i) {
            int ii = i;

            Button editButton = new Button.Builder(Component.empty(), button -> startEditing(ii)).bounds(0, 0, 75, 20).build();
            Button resetButton = new Button.Builder(Component.translatable("controls.reset"), button -> resetKeyMapping(ii)).bounds(0, 0, 50, 20).build();

            rowItems.add(new RowItem(parentGui, editButton, resetButton, options.keyBindings[i]));
        }
        rowItems.sort(Comparator.comparing(entry -> entry.keyMapping));
        rowItems.forEach(this::addEntry);

        checkDuplicateKeys();
    }

    public boolean isEditing() {
        return keyForEdit != null;
    }

    private void startEditing(int index) {
        keyForEdit = options.keyBindings[index];
    }

    private void resetKeyMapping(int index) {
        KeyMapping key = options.keyBindings[index];
        options.setKeyBinding(key, key.getDefaultKey());
        checkDuplicateKeys();
        KeyMapping.resetMapping();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        if (isEditing()) {
            options.setKeyBinding(keyForEdit, InputConstants.Type.MOUSE.getOrCreate(mouseButtonEvent.button()));
            keyForEdit = null;
            checkDuplicateKeys();
            KeyMapping.resetMapping();

            return true;
        } else {
            return super.mouseClicked(mouseButtonEvent, doubleClick);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (isEditing()) {
            if (keyEvent.key() != GLFW.GLFW_KEY_ESCAPE) {
                options.setKeyBinding(keyForEdit, InputConstants.getKey(keyEvent));
            } else if (keyForEdit.same(options.keyBindMenu)) {
                options.setKeyBinding(keyForEdit, InputConstants.UNKNOWN);
            }
            keyForEdit = null;
            checkDuplicateKeys();
            KeyMapping.resetMapping();

            return true;
        } else {
            return super.keyPressed(keyEvent);
        }
    }

    private boolean isDuplicated(KeyMapping key) {
        return duplicateKeys.containsKey(key);
    }

    private Component getDuplicateKeys(KeyMapping key) {
        return duplicateKeys.get(key);
    }

    private void checkDuplicateKeys() {
        duplicateKeys.clear();
        for (KeyMapping key : options.keyBindings) {
            if (key.isUnbound()) continue;

            MutableComponent keyNames = null;
            for (KeyMapping compare : minecraft.options.keyMappings) {
                if (key != compare && key.same(compare)) {
                    if (keyNames == null) {
                        keyNames = Component.empty();
                    } else {
                        keyNames.append(",");
                    }
                    keyNames.append(Component.translatable(compare.getName()));
                }
            }

            if (keyNames != null) {
                duplicateKeys.put(key, Component.translatable("controls.keybinds.duplicateKeybinds", keyNames));
            }
        }
    }

    @Override
    public int getRowWidth() {
        return 340;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    public class RowItem extends GuiListMinimap.Entry<RowItem> {
        private final GuiMinimapControls parentGui;
        private final Button button;
        private final Button buttonReset;
        private final KeyMapping keyMapping;

        protected RowItem(GuiMinimapControls parentGui, Button button, Button buttonReset, KeyMapping keyMapping) {
            super(GuiListKeys.this);

            this.parentGui = parentGui;
            this.keyMapping = keyMapping;

            addWidget(this.button = button);
            addWidget(this.buttonReset = buttonReset);
        }

        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            super.renderContent(guiGraphics, mouseX, mouseY, hovered, tickDelta);

            guiGraphics.drawString(parentGui.getFont(), Component.translatable(keyMapping.getName()), getX() + 5, getY() + 5, 0xFFFFFFFF);

            Tooltip tooltip = null;

            MutableComponent keyText = keyMapping.getTranslatedKeyMessage().copy();
            if (isEditing() && keyForEdit == this.keyMapping) {
                keyText = Component.empty().append("§e> §r").append(keyText).append("§e <");
            } else if (isDuplicated(keyMapping)) {
                keyText = Component.empty().append("§e[ §r").append(keyText).append("§e ]");
                tooltip = Tooltip.create(getDuplicateKeys(keyMapping));
            }

            button.setMessage(keyText);
            button.setTooltip(tooltip);
            button.setX(getX() + getWidth() - 135);
            button.setY(getY());

            buttonReset.active = !keyMapping.isDefault();
            buttonReset.setX(getX() + getWidth() - 55);
            buttonReset.setY(getY());
        }
    }

}