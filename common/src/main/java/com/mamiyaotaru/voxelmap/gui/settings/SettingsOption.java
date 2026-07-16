package com.mamiyaotaru.voxelmap.gui.settings;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;

public final class SettingsOption<T> {
    public enum ControlType {
        TOGGLE,
        CHOICE,
        SLIDER,
        TEXT,
        ACTION
    }

    public record Choice<T>(T value, Component label) {}

    private final String id;
    private final Component name;
    private final Component tooltip;
    private final ControlType controlType;
    private final Supplier<T> getter;
    private final Consumer<T> setter;
    private final List<Choice<T>> choices;
    private final double minimum;
    private final double maximum;
    private final double step;
    private final Function<T, Component> formatter;
    private final BooleanSupplier enabled;
    private final Supplier<Component> disabledReason;
    private final int indentation;

    private SettingsOption(
            String id,
            Component name,
            Component tooltip,
            ControlType controlType,
            Supplier<T> getter,
            Consumer<T> setter,
            List<Choice<T>> choices,
            double minimum,
            double maximum,
            double step,
            Function<T, Component> formatter,
            BooleanSupplier enabled,
            Supplier<Component> disabledReason,
            int indentation) {
        this.id = id;
        this.name = name;
        this.tooltip = tooltip;
        this.controlType = controlType;
        this.getter = getter;
        this.setter = setter;
        this.choices = choices;
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = step;
        this.formatter = formatter;
        this.enabled = enabled;
        this.disabledReason = disabledReason;
        this.indentation = indentation;
    }

    public static SettingsOption<Boolean> toggle(String id, String nameKey, String tooltipKey, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return toggle(id, nameKey, tooltipKey, getter, setter, () -> true, Component::empty, 0);
    }

    public static SettingsOption<Boolean> toggle(
            String id,
            String nameKey,
            String tooltipKey,
            Supplier<Boolean> getter,
            Consumer<Boolean> setter,
            BooleanSupplier enabled,
            Supplier<Component> disabledReason,
            int indentation) {
        return new SettingsOption<>(id, Component.translatable(nameKey), tooltip(tooltipKey), ControlType.TOGGLE, getter, setter,
                List.of(), 0.0, 1.0, 1.0, value -> Component.translatable(value ? "options.on" : "options.off"), enabled, disabledReason, indentation);
    }

    public static <T> SettingsOption<T> choice(
            String id,
            String nameKey,
            String tooltipKey,
            Supplier<T> getter,
            Consumer<T> setter,
            List<Choice<T>> choices) {
        return choice(id, nameKey, tooltipKey, getter, setter, choices, () -> true, Component::empty, 0);
    }

    public static <T> SettingsOption<T> choice(
            String id,
            String nameKey,
            String tooltipKey,
            Supplier<T> getter,
            Consumer<T> setter,
            List<Choice<T>> choices,
            BooleanSupplier enabled,
            Supplier<Component> disabledReason,
            int indentation) {
        return new SettingsOption<>(id, Component.translatable(nameKey), tooltip(tooltipKey), ControlType.CHOICE, getter, setter,
                List.copyOf(choices), 0.0, 0.0, 0.0, value -> choices.stream().filter(choice -> choice.value().equals(value)).map(Choice::label).findFirst().orElse(Component.literal("?")), enabled, disabledReason, indentation);
    }

    public static SettingsOption<Double> slider(
            String id,
            String nameKey,
            String tooltipKey,
            Supplier<Double> getter,
            Consumer<Double> setter,
            double minimum,
            double maximum,
            double step,
            Function<Double, Component> formatter,
            BooleanSupplier enabled,
            Supplier<Component> disabledReason,
            int indentation) {
        return new SettingsOption<>(id, Component.translatable(nameKey), tooltip(tooltipKey), ControlType.SLIDER, getter, setter,
                List.of(), minimum, maximum, step, formatter, enabled, disabledReason, indentation);
    }

    public static SettingsOption<String> text(
            String id,
            String nameKey,
            String tooltipKey,
            Supplier<String> getter,
            Consumer<String> setter,
            BooleanSupplier enabled,
            Supplier<Component> disabledReason) {
        return new SettingsOption<>(id, Component.translatable(nameKey), tooltip(tooltipKey), ControlType.TEXT, getter, setter,
                List.of(), 0.0, 0.0, 0.0, Component::literal, enabled, disabledReason, 0);
    }

    public static SettingsOption<Void> action(String id, String nameKey, String tooltipKey, Component actionLabel, Runnable action) {
        return action(id, nameKey, tooltipKey, actionLabel, action, () -> true, Component::empty);
    }

    public static SettingsOption<Void> action(
            String id,
            String nameKey,
            String tooltipKey,
            Component actionLabel,
            Runnable action,
            BooleanSupplier enabled,
            Supplier<Component> disabledReason) {
        return new SettingsOption<>(id, Component.translatable(nameKey), tooltip(tooltipKey), ControlType.ACTION, () -> null, ignored -> action.run(),
                List.of(), 0.0, 0.0, 0.0, ignored -> actionLabel, enabled, disabledReason, 0);
    }

    private static Component tooltip(String key) {
        return key == null || key.isEmpty() ? Component.empty() : Component.translatable(key);
    }

    public String id() {
        return id;
    }

    public Component name() {
        return name;
    }

    public Component tooltip() {
        return tooltip;
    }

    public ControlType controlType() {
        return controlType;
    }

    public T value() {
        return getter.get();
    }

    public void set(T value) {
        setter.accept(value);
    }

    public List<Choice<T>> choices() {
        return choices;
    }

    public double minimum() {
        return minimum;
    }

    public double maximum() {
        return maximum;
    }

    public double step() {
        return step;
    }

    public Component format(T value) {
        return formatter.apply(value);
    }

    public boolean enabled() {
        return enabled.getAsBoolean();
    }

    public Component disabledReason() {
        return disabledReason.get();
    }

    public int indentation() {
        return indentation;
    }
}
