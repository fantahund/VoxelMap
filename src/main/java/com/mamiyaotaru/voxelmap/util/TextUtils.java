package com.mamiyaotaru.voxelmap.util;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class TextUtils {
    public static String scrubCodes(String string) {
        return string.replaceAll("(§.)", "");
    }

    public static String scrubName(String input) {
        input = input.replace(",", "~comma~");
        return input.replace(":", "~colon~");
    }

    public static String scrubNameRegex(String input) {
        input = input.replace(",", "﹐");
        input = input.replace("[", "⟦");
        return input.replace("]", "⟧");
    }

    public static String scrubNameFile(String input) {
        input = input.replace("<", "~less~");
        input = input.replace(">", "~greater~");
        input = input.replace(":", "~colon~");
        input = input.replace("\"", "~quote~");
        input = input.replace("/", "~slash~");
        input = input.replace("\\", "~backslash~");
        input = input.replace("|", "~pipe~");
        input = input.replace("?", "~question~");
        return input.replace("*", "~star~");
    }

    public static String descrubName(String input) {
        input = input.replace("~less~", "<");
        input = input.replace("~greater~", ">");
        input = input.replace("~colon~", ":");
        input = input.replace("~quote~", "\"");
        input = input.replace("~slash~", "/");
        input = input.replace("~backslash~", "\\");
        input = input.replace("~pipe~", "|");
        input = input.replace("~question~", "?");
        input = input.replace("~star~", "*");
        input = input.replace("~comma~", ",");
        input = input.replace("~colon~", ":");
        input = input.replace("﹐", ",");
        input = input.replace("⟦", "[");
        return input.replace("⟧", "]");
    }

    public static String prettify(String input) {
        String[] words = input.split("_");

        for (int t = 0; t < words.length; ++t) {
            words[t] = words[t].substring(0, 1).toUpperCase() + words[t].substring(1).toLowerCase();
        }

        return String.join(" ", words);
    }

    public static String asFormattedString(Text text2) {
        StringBuilder stringBuilder = new StringBuilder();
        String lastStyleString = "";

        for (Text text : TextUtils.stream(text2)) {
            String contentString = text.getString();
            if (!contentString.isEmpty()) {
                String styleString = asString(text.getStyle());
                if (!styleString.equals(lastStyleString)) {
                    if (!lastStyleString.isEmpty()) {
                        stringBuilder.append(Formatting.RESET);
                    }

                    stringBuilder.append(styleString);
                    lastStyleString = styleString;
                }

                stringBuilder.append(contentString);
            }
        }

        if (!lastStyleString.isEmpty()) {
            stringBuilder.append(Formatting.RESET);
        }

        return stringBuilder.toString();
    }

    private static List<Text> stream(Text text) {
        List<Text> stream = new ArrayList<>();
        stream.add(text);

        for (@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter") Text sibling : text.getSiblings()) {
            stream.addAll(stream(sibling));
        }

        return stream;
    }

    private static String asString(Style style) {
        if (style.isEmpty()) {
            return "";
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            if (style.getColor() != null) {
                Formatting colorFormat = Formatting.byName(style.getColor().getName());
                if (colorFormat != null) {
                    stringBuilder.append(colorFormat);
                }
            }

            if (style.isBold()) {
                stringBuilder.append(Formatting.BOLD);
            }

            if (style.isItalic()) {
                stringBuilder.append(Formatting.ITALIC);
            }

            if (style.isUnderlined()) {
                stringBuilder.append(Formatting.UNDERLINE);
            }

            if (style.isObfuscated()) {
                stringBuilder.append(Formatting.OBFUSCATED);
            }

            if (style.isStrikethrough()) {
                stringBuilder.append(Formatting.STRIKETHROUGH);
            }

            return stringBuilder.toString();
        }
    }
}
