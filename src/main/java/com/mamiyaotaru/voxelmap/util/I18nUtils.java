package com.mamiyaotaru.voxelmap.util;

import java.text.Collator;
import java.util.Locale;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.MinecraftClient;

public class I18nUtils {
   public static String getString(String translateMe, Object... args) {
      return I18n.translate(translateMe, args);
   }

   public static Collator getLocaleAwareCollator() {
      String mcLocale = "en_US";

      try {
         mcLocale = MinecraftClient.getInstance().getLanguageManager().getLanguage().getCode();
      } catch (NullPointerException var3) {
      }

      String[] bits = mcLocale.split("_");
      Locale locale = new Locale(bits[0], bits.length > 1 ? bits[1] : "");
      return Collator.getInstance(locale);
   }
}
