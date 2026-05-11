package com.mamiyaotaru.voxelmap.options.fields;

import com.mamiyaotaru.voxelmap.gui.widgets.GuiRangedSlider;
import com.mamiyaotaru.voxelmap.gui.widgets.IOptionWidget;
import net.minecraft.client.gui.components.AbstractWidget;

import java.util.Optional;
import java.util.function.Consumer;

public abstract class NumberField<T extends Number> extends OptionField<T> {
    private final double min;
    private final double max;
    private final double step;

    public NumberField(String saveKey, String key, T defaultValue, T min, T max, T step) {
        super(saveKey, key, defaultValue);
        this.min = min.doubleValue();
        this.max = max.doubleValue();
        this.step = step.doubleValue();
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    protected abstract T fromDouble(double value);

    @Override
    protected Optional<T> validate(T value) {
        double val = value.doubleValue();
        return val >= min && val <= max ? Optional.of(value) : Optional.empty();
    }

    @Override
    public AbstractWidget createWidget(int x, int y, int w, int h, Consumer<T> consumer) {
        return new NumberFieldSlider<>(this, x, y, w, h, consumer);
    }

    public static class NumberFieldSlider<T extends Number> extends GuiRangedSlider implements IOptionWidget {
        private final NumberField<T> field;
        private final Consumer<T> consumer;

        public NumberFieldSlider(NumberField<T> field, int x, int y, int w, int h, Consumer<T> consumer) {
            super(x, y, w, h, field.getMessage(), field.get().doubleValue(), field.getMin(), field.getMax(), (e) -> ((NumberFieldSlider<?>) e).onUpdate());
            this.field = field;
            this.consumer = consumer;
        }

        @Override
        public void refresh() {
            if (!isFocused()) {
                setUnscaled(field.get().doubleValue());
            }
            refreshWidget(this, field);
        }

        @Override
        public void onUpdate() {
            double val = getUnscaled();
            double step = field.getStep();
            boolean isMax = val <= field.getMin() || val >= field.getMax();
            if (step > 0.0 && !isMax) {
                val = Math.round(val / step) * step;
            }

            field.set(field.fromDouble(val));
            consumer.accept(field.get());

            refresh();
        }
    }
}
