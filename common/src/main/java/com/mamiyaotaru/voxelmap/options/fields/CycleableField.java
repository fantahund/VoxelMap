package com.mamiyaotaru.voxelmap.options.fields;

import com.mamiyaotaru.voxelmap.gui.widgets.IOptionWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;

import java.util.Optional;
import java.util.function.Consumer;

public abstract class CycleableField<T> extends OptionField<T> {
    public CycleableField(String saveKey, String key, T defaultValue) {
        super(saveKey, key, defaultValue);
    }

    public abstract T cycle();

    @Override
    protected Optional<T> validate(T value) {
        return Optional.of(value);
    }

    @Override
    public AbstractWidget createWidget(int x, int y, int w, int h, Consumer<T> consumer) {
        return new ButtonWidget<>(this, x, y, w, h, consumer);
    }

    public static class ButtonWidget<T> extends Button.Plain implements IOptionWidget {
        private final CycleableField<T> field;
        private final Consumer<T> consumer;

        protected ButtonWidget(CycleableField<T> field, int x, int y, int w, int h, Consumer<T> consumer) {
            super(x, y, w, h, field.getMessage(), (e) -> ((ButtonWidget<?>) e).onUpdate(), DEFAULT_NARRATION);
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
