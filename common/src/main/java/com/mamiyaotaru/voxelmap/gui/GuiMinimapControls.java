package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.widgets.GuiListMinimap;
import com.mamiyaotaru.voxelmap.options.containers.MapOptions;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class GuiMinimapControls extends GuiScreenMinimap {
    private final MapOptions options;
    private KeyList keymapList;

    public GuiMinimapControls(Screen parentGui) {
        super(parentGui, Component.translatable("key.category.voxelmap.controls.title"));
        options = VoxelConstants.getVoxelMapInstance().getMapOptions();
    }

    public void init() {
        addRenderableWidget(keymapList = new KeyList(0, 40, getWidth(), getHeight() - 114));
        addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), button -> onClose()).bounds(getWidth() / 2 - 100, getHeight() - 26, 200, 20).build());
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keymapList.isEditing()) {
            return keymapList.keyPressed(keyEvent);
        }

        return super.keyPressed(keyEvent);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        guiGraphics.drawCenteredString(getFont(), I18n.get("controls.minimap.unbind1"), getWidth() / 2, getHeight() - 62, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(getFont(), "§e" + I18n.get("controls.minimap.unbind2"), getWidth() / 2, getHeight() - 46, 0xFFFFFFFF);
    }

    class KeyList extends GuiListMinimap<KeyList.Entry> {
        private final ArrayList<Entry> keys = new ArrayList<>();
        private final HashMap<KeyMapping, Component> duplicateKeys = new HashMap<>();

        private KeyMapping keyForEdit;

        public KeyList(int x, int y, int width, int height) {
            super(x, y, width, height, 20);

            for (int i = 0; i < options.keyBindings.length; i++) {
                int ii = i;

                Button editButton = new Button.Builder(Component.empty(), button -> startEditing(ii)).bounds(0, 0, 75, 20).build();
                Button resetButton = new Button.Builder(Component.translatable("controls.reset"), button -> resetKeyMapping(ii)).bounds(0, 0, 50, 20).build();

                keys.add(new Entry(options.keyBindings[i], editButton, resetButton));
            }
            keys.sort(Comparator.comparing(entry -> entry.keyMapping));
            keys.forEach(this::addEntry);

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
            key.setKey(key.getDefaultKey());
            checkDuplicateKeys();
            KeyMapping.resetMapping();
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
            if (isEditing()) {
                keyForEdit.setKey(InputConstants.Type.MOUSE.getOrCreate(mouseButtonEvent.button()));
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
                    keyForEdit.setKey(InputConstants.getKey(keyEvent));
                } else if (!keyForEdit.same(options.keyBindMenu)) {
                    keyForEdit.setKey(InputConstants.UNKNOWN);
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
                            keyNames.append(", ");
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
        protected void renderSelection(GuiGraphics guiGraphics, Entry entry, int color) {
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }

        public class Entry extends GuiListMinimap.Entry<Entry> {
            private final KeyMapping keyMapping;
            private final Button button;
            private final Button buttonReset;

            public Entry(KeyMapping keyMapping, Button button, Button buttonReset) {
                super(KeyList.this);

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
                if (isEditing() && keyForEdit == keyMapping) {
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
}