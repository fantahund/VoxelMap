package com.mamiyaotaru.voxelmap.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

public final class TextUtils {
    private static final Pattern CODE_SCRUBBING_PATTERN = Pattern.compile("(§.)");

    private TextUtils() {}

    public static String scrubCodes(String string) { return CODE_SCRUBBING_PATTERN.matcher(string).replaceAll(""); }

    @NotNull
    public static String scrubName(String input) {
        if (input == null) {
            return "";
        }

        return input
                .replace(",", "~comma~")
                .replace(":", "~colon~");
    }

    @NotNull
    public static String scrubNameRegex(String input) {
        if (input == null) {
            return "";
        }

        return input
                .replace(',', '﹐')
                .replace('[', '⟦')
                .replace(']', '⟧');
    }

    @NotNull
    public static String scrubNameFile(String input) {
        if (input == null) {
            return "";
        }

        return input
                .replace("<", "~less~")
                .replace(">", "~greater~")
                .replace(":", "~colon~")
                .replace("\"", "~quote~")
                .replace("/", "~slash~")
                .replace("\\", "~backslash~")
                .replace("|", "~pipe~")
                .replace("?", "~question~")
                .replace("*", "~star~");
    }

    @NotNull
    public static String descrubName(String input) {
        if (input == null) {
            return "";
        }

        return input
                .replace("~less~", "<")
                .replace("~greater~", ">")
                .replace("~colon~", ":")
                .replace("~quote~", "\"")
                .replace("~slash~", "/")
                .replace("~backslash~", "\\")
                .replace("~pipe~", "|")
                .replace("~question~", "?")
                .replace("~star~", "*")
                .replace("~comma~", ",")
                .replace("~colon~", ":")
                .replace('﹐', ',')
                .replace('⟦', '[')
                .replace('⟧', ']');
    }

    @NotNull
    public static String prettify(String input) {
        if (input == null) {
            return "";
        }

        return Arrays
                .stream(input.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    @NotNull
    public static String asFormattedString(Component text2) {
        StringBuilder stringBuilder = new StringBuilder();
        String lastStyleString = "";

        for (Component text : stream(text2)) {
            String contentString = text.getString();

            if (contentString.isEmpty()) {
                continue;
            }

            String styleString = asString(text.getStyle());

            if (styleString.equals(lastStyleString)) {
                stringBuilder.append(styleString);
                continue;
            }

            if (!(lastStyleString.isEmpty())) {
                stringBuilder.append(ChatFormatting.RESET);
            }

            stringBuilder.append(styleString);
            lastStyleString = styleString;

            stringBuilder.append(contentString);
        }

        if (!(lastStyleString.isEmpty())) {
            stringBuilder.append(ChatFormatting.RESET);
        }
        return stringBuilder.toString();
    }

    @NotNull
    private static List<Component> stream(Component text) {
        List<Component> stream = new ArrayList<>();
        stream.add(text);

        for (Component sibling : text.getSiblings()) {
            stream.addAll(stream(sibling));
        }

        return stream;
    }

    @NotNull
    private static String asString(Style style) {
        if (style == null || style.isEmpty()) {
            return "";
        }

        StringBuilder stringBuilder = new StringBuilder();

        if (style.getColor() != null) {
            ChatFormatting colorFormat = ChatFormatting.getByName(style.getColor().serialize());

            if (colorFormat != null) {
                stringBuilder.append(colorFormat);
            }
        }

        if (style.isBold()) {
            stringBuilder.append(ChatFormatting.BOLD);
        }
        if (style.isItalic()) {
            stringBuilder.append(ChatFormatting.ITALIC);
        }
        if (style.isUnderlined()) {
            stringBuilder.append(ChatFormatting.UNDERLINE);
        }
        if (style.isObfuscated()) {
            stringBuilder.append(ChatFormatting.OBFUSCATED);
        }
        if (style.isStrikethrough()) {
            stringBuilder.append(ChatFormatting.STRIKETHROUGH);
        }
        return stringBuilder.toString();
    }
}