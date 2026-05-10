package com.mamiyaotaru.voxelmap.options.fields;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class OptionField<T> {
    private final String saveKey;
    private final String key;
    private final T defaultValue;
    private T value;
    private Function<T, String> format;
    private Function<T, Tooltip> tooltip;
    private Consumer<T> listener;
    private boolean isActive;
    private boolean isAllowed;

    public OptionField(String saveKey, String key, T defaultValue) {
        this.saveKey = saveKey;
        this.key = key;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.format = defaultFormat();
        this.tooltip = (x) -> null;
        this.listener = (x) -> {};
        this.isActive = true;
        this.isAllowed = true;
    }

    protected Function<T, String> defaultFormat() {
        return String::valueOf;
    }

    public String getSaveKey() {
        return saveKey;
    }

    public String getKey() {
        return key;
    }

    public Component getMessage() {
        return Component.translatable("options.generic_value", I18n.get(getKey()), getValueString());
    }

    public T getDefault() {
        return defaultValue;
    }

    public T get() {
        return value;
    }

    public OptionField<T> set(Object value) {
        T parsed = parse(value).orElseGet(() -> {
            VoxelConstants.getLogger().warn("Failed to parse value {} for {}", value, saveKey);
            return defaultValue;
        });

        T validated = validate(parsed).orElseGet(() -> {
            VoxelConstants.getLogger().warn("Invalid option value {} for {}", parsed, saveKey);
            return defaultValue;
        });

        if (!VoxelConstants.getVoxelMapInstance().isRunning()) {
            this.value = validated;
        } else {
            if (this.value != validated) {
                this.value = validated;
                listener.accept(this.value);
            }
        }

        return this;
    }

    public OptionField<T> reset() {
        set(defaultValue);
        return this;
    }

    protected abstract Optional<T> validate(T value);

    protected abstract Optional<T> parse(Object value);

    public abstract AbstractWidget createWidget(int x, int y, int w, int h, Consumer<T> consumer);

    // Attribute Getters

    public String getValueString() {
        return format.apply(value);
    }

    public Tooltip getTooltip() {
        return tooltip.apply(value);
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isAllowed() {
        return isAllowed;
    }

    // Attribute Setters

    public OptionField<T> withFormat(Function<T, String> format) {
        this.format = format;
        return this;
    }

    public OptionField<T> withTooltip(Function<T, Tooltip> tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public OptionField<T> withListener(Consumer<T> listener) {
        this.listener = listener;
        return this;
    }

    public OptionField<T> setActive(boolean isActive) {
        this.isActive = isActive;
        return this;
    }

    public OptionField<T> setAllowed(boolean isAllowed) {
        this.isAllowed = isAllowed;
        return this;
    }
}
