package com.mamiyaotaru.voxelmap.options.fields;

import com.mamiyaotaru.voxelmap.gui.overridden.IRefreshableOptionWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.language.I18n;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class BooleanField extends OptionField<Boolean> {
    public BooleanField(String saveKey, String key, boolean defaultValue) {
        super(saveKey, key, defaultValue);
    }

    public Boolean toggle() {
        return set(!get()).get();
    }

    @Override
    protected Function<Boolean, String> defaultFormat() {
        return (x) -> x ? I18n.get("options.on") : I18n.get("options.off");
    }

    @Override
    protected Optional<Boolean> validate(Boolean value) {
        return Optional.of(value);
    }

    @Override
    protected Optional<Boolean> parse(Object value) {
        if (value instanceof Boolean booleanVal) {
            return Optional.of(booleanVal);
        }
        if (value instanceof String string) {
            try {
                return Optional.of(Boolean.parseBoolean(string));
            } catch (Exception ignored) {
            }
        }
        return Optional.empty();
    }

    @Override
    public AbstractWidget createWidget(int x, int y, int w, int h, Consumer<Boolean> consumer) {
        return new BooleanFieldButton(this, x, y, w, h, consumer);
    }

    public static class BooleanFieldButton extends Button.Plain implements IRefreshableOptionWidget {
        private final BooleanField field;
        private final Consumer<Boolean> consumer;

        protected BooleanFieldButton(BooleanField field, int x, int y, int w, int h, Consumer<Boolean> consumer) {
            super(x, y, w, h, field.getMessage(), (e) -> ((BooleanFieldButton) e).onUpdate(), DEFAULT_NARRATION);
            this.field = field;
            this.consumer = consumer;
        }

        @Override
        public void refresh() {
            refreshWidget(this, field);
        }

        @Override
        public void onUpdate() {
            field.toggle();
            consumer.accept(field.get());

            refresh();
        }
    }
}
