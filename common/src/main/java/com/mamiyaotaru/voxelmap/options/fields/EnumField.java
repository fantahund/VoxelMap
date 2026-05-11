package com.mamiyaotaru.voxelmap.options.fields;

import com.mamiyaotaru.voxelmap.gui.widgets.IOptionWidget;
import com.mamiyaotaru.voxelmap.options.enums.IOptionEnum;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.language.I18n;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class EnumField<T extends Enum<T>> extends OptionField<T> {
    private final Class<T> enumClass;
    private final T[] allValues;

    public EnumField(String saveKey, String key, T defaultValue) {
        super(saveKey, key, defaultValue);
        enumClass = defaultValue.getDeclaringClass();
        allValues = enumClass.getEnumConstants();
    }

    private int getIndex(T value) {
        for (int i = 0; i < allValues.length; i++) {
            if (value == allValues[i]) {
                return i;
            }
        }
        return -1;
    }

    public T cycle() {
        int i = getIndex(get());
        if (i < 0) {
            return get();
        }
        i = (i + 1) % allValues.length;
        return set(allValues[i]).get();
    }

    @Override
    protected Function<T, String> defaultFormat() {
        return (x) -> x instanceof IOptionEnum e ? I18n.get(e.getKey()) : String.valueOf(x);
    }

    @Override
    protected Optional<T> validate(T value) {
        return Optional.of(value);
    }

    @Override
    protected Optional<T> parse(Object value) {
        if (enumClass.isInstance(value)) {
            return Optional.of(enumClass.cast(value));
        }
        if (value instanceof String string) {
            try {
                return Optional.of(Enum.valueOf(enumClass, string));
            } catch (Exception ignored) {
            }
        }
        return Optional.empty();
    }

    @Override
    public AbstractWidget createWidget(int x, int y, int w, int h, Consumer<T> consumer) {
        return new EnumFieldButton<>(this, x, y, w, h, consumer);
    }

    public static class EnumFieldButton<T extends Enum<T>> extends Button.Plain implements IOptionWidget {
        private final EnumField<T> field;
        private final Consumer<T> consumer;

        protected EnumFieldButton(EnumField<T> field, int x, int y, int w, int h, Consumer<T> consumer) {
            super(x, y, w, h, field.getMessage(), (e) -> ((EnumFieldButton<?>) e).onUpdate(), DEFAULT_NARRATION);
            this.field = field;
            this.consumer = consumer;
        }

        @Override
        public void refresh() {
            refreshWidget(this, field);
        }

        @Override
        public void onUpdate() {
            field.cycle();
            consumer.accept(field.get());

            refresh();
        }
    }
}
