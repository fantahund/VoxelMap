package com.mamiyaotaru.voxelmap.gui.settings;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.GuiMinimapOptions;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;

public final class SettingsListWidget extends AbstractSelectionList<SettingsListWidget.Entry> {
    private static final int OPTION_HEIGHT = 28;
    private static final int HEADER_HEIGHT = 24;
    private final GuiMinimapOptions screen;
    private KeyMapping editingKey;

    public SettingsListWidget(GuiMinimapOptions screen, int x, int y, int width, int height, SettingsCategory category) {
        super(VoxelConstants.getMinecraft(), width, height, y, OPTION_HEIGHT);
        this.screen = screen;
        this.centerListVertically = false;
        updateSizeAndPosition(width, height, x, y);
        populate(category);
    }

    private void populate(SettingsCategory category) {
        for (SettingsGroup group : category.groups()) {
            addEntry(new GroupEntry(group.title()), HEADER_HEIGHT);
            for (SettingsOption<?> option : group.options()) {
                addEntry(new OptionEntry(option), OPTION_HEIGHT);
            }
        }

        if (category.specialView() == SettingsCategory.SpecialView.KEY_BINDINGS) {
            addEntry(new GroupEntry(Component.translatable("options.voxelmap.group.keyBindings")), HEADER_HEIGHT);
            KeyMapping[] mappings = VoxelConstants.getVoxelMapInstance().getMapOptions().keyBindings.clone();
            Arrays.sort(mappings);
            for (KeyMapping mapping : mappings) {
                addEntry(new KeyEntry(mapping), OPTION_HEIGHT);
            }
            addEntry(new HelpEntry(Component.translatable("options.voxelmap.controls.unbindHelp")), HEADER_HEIGHT);
        }
    }

    @Override
    public int getRowWidth() {
        return Math.max(120, getWidth() - 10);
    }

    @Override
    protected int scrollBarX() {
        return getX() + getWidth() - 6;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (editingKey != null) {
            MapSettingsManager map = VoxelConstants.getVoxelMapInstance().getMapOptions();
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                if (!editingKey.same(map.keyBindMenu))
                    map.setKeyBinding(editingKey, InputConstants.UNKNOWN);
            } else {
                map.setKeyBinding(editingKey, InputConstants.getKey(event));
            }
            editingKey = null;
            KeyMapping.resetMapping();
            return true;
        }
        return super.keyPressed(event);
    }

    public boolean isEditingKey() {
        return editingKey != null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (editingKey != null) {
            VoxelConstants.getVoxelMapInstance().getMapOptions().setKeyBinding(editingKey, InputConstants.Type.MOUSE.getOrCreate(event.button()));
            editingKey = null;
            KeyMapping.resetMapping();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    public void commitPendingText() {
        for (Entry entry : children()) {
            if (entry instanceof OptionEntry optionEntry && optionEntry.control instanceof CommitEditBox editBox)
                editBox.commit();
        }
    }

    public abstract static class Entry extends AbstractSelectionList.Entry<Entry> {
    }

    private final class GroupEntry extends Entry {
        private final Component title;

        private GroupEntry(Component title) {
            this.title = title;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight() - 2, 0xAA202020);
            graphics.text(screen.getFont(), title.copy().withStyle(ChatFormatting.BOLD), getX() + 8, getY() + 7, 0xFFFFFFFF);
        }
    }

    private final class HelpEntry extends Entry {
        private final Component text;

        private HelpEntry(Component text) {
            this.text = text;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            graphics.text(screen.getFont(), text, getX() + 10, getY() + 7, 0xFFA0A0A0);
        }
    }

    private final class OptionEntry extends Entry {
        private final SettingsOption<?> option;
        private final AbstractWidget control;

        private OptionEntry(SettingsOption<?> option) {
            this.option = option;
            this.control = createControl(option);
        }

        private AbstractWidget createControl(SettingsOption<?> option) {
            return switch (option.controlType()) {
                case TOGGLE -> Button.builder(Component.empty(), button -> {
                    setOption(option, !(Boolean) option.value());
                }).bounds(0, 0, 110, 20).build();
                case CHOICE -> Button.builder(Component.empty(), button -> screen.cycleChoice(option)).bounds(0, 0, 140, 20).build();
                case SLIDER -> new OptionSlider(castDouble(option));
                case TEXT -> new CommitEditBox(castString(option));
                case ACTION -> Button.builder(Component.empty(), button -> {
                    setOption(option, null);
                }).bounds(0, 0, 140, 20).build();
            };
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            boolean enabled = option.enabled();
            int indent = option.indentation() * 12;
            int controlWidth = Math.clamp(getWidth() / 2, 105, 190);
            control.setRectangle(controlWidth, 20, getX() + getWidth() - controlWidth - 8, getY() + 4);
            control.active = enabled;

            if (control instanceof Button)
                control.setMessage(formatOption(option));
            if (control instanceof OptionSlider slider && !slider.isFocused())
                slider.sync();
            if (control instanceof CommitEditBox editBox)
                editBox.syncIfIdle();

            Component tooltip = enabled || option.disabledReason().getString().isEmpty()
                    ? option.tooltip()
                    : Component.empty().append(option.tooltip()).append("\n").append(option.disabledReason().copy().withStyle(ChatFormatting.YELLOW));
            control.setTooltip(tooltip.getString().isEmpty() ? null : Tooltip.create(tooltip));

            int labelColor = enabled ? 0xFFFFFFFF : 0xFF808080;
            int labelMaxWidth = control.getX() - getX() - 20 - indent;
            String label = screen.getFont().plainSubstrByWidth(option.name().getString(), Math.max(20, labelMaxWidth));
            graphics.text(screen.getFont(), label, getX() + 10 + indent, getY() + 10, labelColor);
            control.extractRenderState(graphics, mouseX, mouseY, delta);
        }

        @Override
        public void visitWidgets(Consumer<AbstractWidget> consumer) {
            consumer.accept(control);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return control.mouseClicked(event, doubleClick);
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            return control.mouseReleased(event);
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
            return control.mouseDragged(event, dx, dy);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            return control.keyPressed(event);
        }

        @Override
        public boolean charTyped(CharacterEvent event) {
            return control.charTyped(event);
        }
    }

    private final class OptionSlider extends AbstractSliderButton {
        private final SettingsOption<Double> option;

        private OptionSlider(SettingsOption<Double> option) {
            super(0, 0, 140, 20, Component.empty(), normalize(option, option.value()));
            this.option = option;
            updateMessage();
        }

        private void sync() {
            value = normalize(option, option.value());
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(option.format(denormalize(option, value)));
        }

        @Override
        protected void applyValue() {
            option.set(denormalize(option, value));
            updateMessage();
        }
    }

    private final class CommitEditBox extends EditBox {
        private final SettingsOption<String> option;
        private boolean dirty;
        private boolean syncing;

        private CommitEditBox(SettingsOption<String> option) {
            super(screen.getFont(), 0, 0, 140, 20, option.name());
            this.option = option;
            setMaxLength(256);
            setFromOption();
            setHint(Component.translatable("options.voxelmap.value.enterText"));
            setResponder(ignored -> {
                if (!syncing)
                    dirty = true;
            });
        }

        private void syncIfIdle() {
            if (!isFocused() && !dirty && !getValue().equals(option.value()))
                setFromOption();
        }

        private void setFromOption() {
            syncing = true;
            setValue(option.value());
            syncing = false;
        }

        private void commit() {
            if (!dirty)
                return;
            option.set(getValue());
            dirty = false;
            setFromOption();
        }

        @Override
        public void setFocused(boolean focused) {
            if (!focused && isFocused())
                commit();
            super.setFocused(focused);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                commit();
                setFocused(false);
                return true;
            }
            return super.keyPressed(event);
        }
    }

    private final class KeyEntry extends Entry {
        private final KeyMapping mapping;
        private final Button bindButton;
        private final Button resetButton;

        private KeyEntry(KeyMapping mapping) {
            this.mapping = mapping;
            bindButton = Button.builder(Component.empty(), button -> editingKey = mapping).bounds(0, 0, 100, 20).build();
            resetButton = Button.builder(Component.translatable("controls.reset"), button -> {
                VoxelConstants.getVoxelMapInstance().getMapOptions().setKeyBinding(mapping, mapping.getDefaultKey());
                KeyMapping.resetMapping();
            }).bounds(0, 0, 55, 20).build();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            int resetWidth = Math.min(70, Math.max(48, getWidth() / 6));
            int bindWidth = Math.min(150, Math.max(80, getWidth() / 3));
            resetButton.setRectangle(resetWidth, 20, getX() + getWidth() - resetWidth - 8, getY() + 4);
            bindButton.setRectangle(bindWidth, 20, resetButton.getX() - bindWidth - 4, getY() + 4);
            resetButton.active = !mapping.isDefault();

            MutableComponent keyText = mapping.getTranslatedKeyMessage().copy();
            if (editingKey == mapping)
                keyText = Component.literal("> ").withStyle(ChatFormatting.YELLOW).append(keyText).append(" <");
            Component duplicate = duplicateTooltip(mapping);
            if (duplicate != null && editingKey != mapping) {
                keyText = Component.literal("[ ").withStyle(ChatFormatting.YELLOW).append(keyText).append(" ]");
                bindButton.setTooltip(Tooltip.create(duplicate));
            } else
                bindButton.setTooltip(null);

            bindButton.setMessage(keyText);
            int labelWidth = bindButton.getX() - getX() - 16;
            String label = screen.getFont().plainSubstrByWidth(Component.translatable(mapping.getName()).getString(), Math.max(20, labelWidth));
            graphics.text(screen.getFont(), label, getX() + 10, getY() + 10, 0xFFFFFFFF);
            bindButton.extractRenderState(graphics, mouseX, mouseY, delta);
            resetButton.extractRenderState(graphics, mouseX, mouseY, delta);
        }

        @Override
        public void visitWidgets(Consumer<AbstractWidget> consumer) {
            consumer.accept(bindButton);
            consumer.accept(resetButton);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return bindButton.mouseClicked(event, doubleClick) || resetButton.mouseClicked(event, doubleClick);
        }
    }

    private Component duplicateTooltip(KeyMapping mapping) {
        List<KeyMapping> duplicates = Arrays.stream(VoxelConstants.getMinecraft().options.keyMappings)
                .filter(other -> other != mapping && !mapping.isUnbound() && mapping.same(other)).toList();
        if (duplicates.isEmpty())
            return null;
        MutableComponent names = Component.empty();
        for (int i = 0; i < duplicates.size(); i++) {
            if (i > 0)
                names.append(", ");
            names.append(Component.translatable(duplicates.get(i).getName()));
        }
        return Component.translatable("controls.keybinds.duplicateKeybinds", names);
    }

    @SuppressWarnings("unchecked")
    private static void setOption(SettingsOption<?> option, Object value) {
        ((SettingsOption<Object>) option).set(value);
    }

    @SuppressWarnings("unchecked")
    private static Component formatOption(SettingsOption<?> option) {
        SettingsOption<Object> typed = (SettingsOption<Object>) option;
        return typed.format(typed.value());
    }

    @SuppressWarnings("unchecked")
    private static SettingsOption<Double> castDouble(SettingsOption<?> option) {
        return (SettingsOption<Double>) option;
    }

    @SuppressWarnings("unchecked")
    private static SettingsOption<String> castString(SettingsOption<?> option) {
        return (SettingsOption<String>) option;
    }

    private static double normalize(SettingsOption<Double> option, double value) {
        return (value - option.minimum()) / (option.maximum() - option.minimum());
    }

    private static double denormalize(SettingsOption<Double> option, double normalized) {
        double raw = option.minimum() + normalized * (option.maximum() - option.minimum());
        double stepped = Math.round(raw / option.step()) * option.step();
        return Math.clamp(stepped, option.minimum(), option.maximum());
    }
}
