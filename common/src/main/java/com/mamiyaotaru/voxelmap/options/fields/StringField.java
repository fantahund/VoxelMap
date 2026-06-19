package com.mamiyaotaru.voxelmap.options.fields;

import com.mamiyaotaru.voxelmap.gui.widgets.GuiButtonText;
import com.mamiyaotaru.voxelmap.gui.widgets.IOptionWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;

import java.util.Optional;
import java.util.function.Consumer;

public class StringField extends OptionField<String> {
    public static final String PATTERN_NONE = ".*";
    public static final String PATTERN_NOT_EMPTY = ".*\\S.*";

    private final String pattern;

    public StringField(String saveKey, String key, String defaultValue, String pattern) {
        super(saveKey, key, defaultValue);
        this.pattern = pattern;
    }

    public String getPattern() {
        return pattern;
    }

    @Override
    protected Optional<String> validate(String value) {
        return value.matches(pattern) ? Optional.of(value) : Optional.empty();
    }

    @Override
    protected Optional<String> parse(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof String string) {
            return Optional.of(string);
        }
        return Optional.of(value.toString());
    }

    @Override
    public AbstractWidget createWidget(int x, int y, int w, int h, Consumer<String> consumer) {
        return new StringFieldTextButton(this, x, y, w, h, consumer);
    }

    public static class StringFieldTextButton extends GuiButtonText implements IOptionWidget {
        private final StringField field;
        private final Consumer<String> consumer;

        public StringFieldTextButton(StringField field, int x, int y, int w, int h, Consumer<String> consumer) {
            super(Minecraft.getInstance().font, x, y, w, h, field.getMessage(), (e) -> ((StringFieldTextButton) e).onUpdate());
            this.field = field;
            this.consumer = consumer;
        }

        @Override
        public void refresh() {
            if (!isFocused()) {
                setText(field.get());
            }
            refreshWidget(this, field);
        }

        @Override
        public void onUpdate() {
            field.set(getText());
            consumer.accept(field.get());

            refresh();
        }
    }
}
