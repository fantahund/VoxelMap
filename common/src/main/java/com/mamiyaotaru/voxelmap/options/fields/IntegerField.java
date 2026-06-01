package com.mamiyaotaru.voxelmap.options.fields;

import java.util.Optional;

public class IntegerField extends SliderableField<Integer> {
    public IntegerField(String saveKey, String key, int defaultValue, int min, int max) {
        this(saveKey, key, defaultValue, min, max, 0);
    }

    public IntegerField(String saveKey, String key, int defaultValue, int min, int max, int step) {
        super(saveKey, key, defaultValue, min, max, step);
    }

    @Override
    protected Integer fromDouble(double value) {
        return (int) Math.round(value);
    }

    @Override
    protected Optional<Integer> parse(Object value) {
        if (value instanceof Integer intVal) {
            return Optional.of(intVal);
        }
        if (value instanceof String string) {
            try {
                return Optional.of(Integer.parseInt(string));
            } catch (Exception ignored) {
            }
        }
        return Optional.empty();
    }
}
