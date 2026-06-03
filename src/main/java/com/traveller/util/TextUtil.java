package com.traveller.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Turns config strings into Adventure components, supporting both the legacy
 * &-codes (&a, &c ...) and &#RRGGBB hex colors.
 */
public final class TextUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final String VALID_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx";

    private static final LegacyComponentSerializer SERIALIZER =
            LegacyComponentSerializer.builder().character('\u00a7').hexColors().build();

    private TextUtil() {
    }

    public static Component component(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        String translated = translate(input);
        // Stop Minecraft's default italic styling leaking into our messages.
        return SERIALIZER.deserialize(translated)
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    private static String translate(String input) {
        // First convert &#RRGGBB into the section-sign hex form Adventure expects.
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("\u00a7x");
            for (char c : hex.toCharArray()) {
                replacement.append('\u00a7').append(c);
            }
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);

        // Then convert the remaining &-codes.
        String result = sb.toString();
        char[] chars = result.toCharArray();
        StringBuilder out = new StringBuilder(result.length());
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '&' && i + 1 < chars.length && VALID_CODES.indexOf(chars[i + 1]) > -1) {
                out.append('\u00a7').append(chars[i + 1]);
                i++;
            } else {
                out.append(chars[i]);
            }
        }
        return out.toString();
    }
}
