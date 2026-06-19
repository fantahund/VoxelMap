package com.mamiyaotaru.voxelmap.options.fields;

import com.mamiyaotaru.voxelmap.options.enums.IOptionEnum;
import net.minecraft.client.resources.language.I18n;

import java.util.Optional;
import java.util.function.Function;

public class EnumField<T extends Enum<T>> extends CycleableField<T> {
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

    @Override
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
}
