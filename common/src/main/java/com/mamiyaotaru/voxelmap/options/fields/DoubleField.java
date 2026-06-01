package com.mamiyaotaru.voxelmap.options.fields;

import java.util.Optional;

public class DoubleField extends SliderableField<Double> {
    public DoubleField(String saveKey, String key, double defaultValue, double min, double max) {
        this(saveKey, key, defaultValue, min, max, 0.0);
    }

    public DoubleField(String saveKey, String key, double defaultValue, double min, double max, double step) {
        super(saveKey, key, defaultValue, min, max, step);
    }

    @Override
    protected Double fromDouble(double value) {
        return value;
    }

    @Override
    protected Optional<Double> parse(Object value) {
        if (value instanceof Double doubleVal) {
            return Optional.of(doubleVal);
        }
        if (value instanceof String string) {
            try {
                return Optional.of(Double.parseDouble(string));
            } catch (Exception ignored) {
            }
        }
        return Optional.empty();
    }
}
