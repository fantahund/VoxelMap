package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelContants;
import net.minecraft.client.resource.language.I18n;

import java.text.Collator;
import java.util.Locale;

public class I18nUtils {
    public static String getString(String translateMe, Object... args) {
        return I18n.translate(translateMe, args);
    }

    public static Collator getLocaleAwareCollator() {
        String mcLocale = "en_US";

        try {
            mcLocale = VoxelContants.getMinecraft().getLanguageManager().getLanguage().getCode();
        } catch (NullPointerException ignored) {}

        String[] bits = mcLocale.split("_");
        Locale locale = new Locale(bits[0], bits.length > 1 ? bits[1] : "");
        return Collator.getInstance(locale);
    }
}
