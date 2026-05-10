package com.mamiyaotaru.voxelmap.options.fields;

import java.util.Optional;

public class FloatField extends NumberField<Float> {
    public FloatField(String saveKey, String key, float defaultValue, float min, float max) {
        this(saveKey, key, defaultValue, min, max, 0.0F);
    }

    public FloatField(String saveKey, String key, float defaultValue, float min, float max, float step) {
        super(saveKey, key, defaultValue, min, max, step);
    }

    @Override
    protected Float fromDouble(double value) {
        return (float) value;
    }

    @Override
    protected Optional<Float> parse(Object value) {
        if (value instanceof Float floatVal) {
            return Optional.of(floatVal);
        }
        if (value instanceof String string) {
            try {
                return Optional.of(Float.parseFloat(string));
            } catch (Exception ignored) {
            }
        }
        return Optional.empty();
    }
}
