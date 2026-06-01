package com.mamiyaotaru.voxelmap.options.fields;

import net.minecraft.client.resources.language.I18n;

import java.util.Optional;
import java.util.function.Function;

public class BooleanField extends CycleableField<Boolean> {
    public BooleanField(String saveKey, String key, boolean defaultValue) {
        super(saveKey, key, defaultValue);
    }

    @Override
    public Boolean cycle() {
        return set(!get()).get();
    }

    @Override
    protected Function<Boolean, String> defaultFormat() {
        return (x) -> x ? I18n.get("options.on") : I18n.get("options.off");
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
}
