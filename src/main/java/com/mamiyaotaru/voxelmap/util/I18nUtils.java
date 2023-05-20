package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraft.client.resource.language.I18n;

import java.text.Collator;
import java.util.Locale;

public final class I18nUtils {
    private I18nUtils() {}

    public static String getString(String translateMe, Object... args) { return I18n.translate(translateMe, args); }

    public static Collator getLocaleAwareCollator() {
        String mcLocale = "en_US";

        try {
            mcLocale = VoxelConstants.getMinecraft().getLanguageManager().getLanguage().getCode();
        } catch (NullPointerException ignored) {}

        String[] bits = mcLocale.split("_");
        return Collator.getInstance(new Locale(bits[0], bits.length > 1 ? bits[1] : ""));
    }
}