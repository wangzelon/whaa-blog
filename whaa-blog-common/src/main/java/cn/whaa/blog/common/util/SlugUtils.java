package cn.whaa.blog.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtils {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern MULTIPLE_DASH = Pattern.compile("-{2,}");

    private SlugUtils() {
    }

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "untitled";
        }
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");
        normalized = WHITESPACE.matcher(normalized).replaceAll("-");
        normalized = NON_LATIN.matcher(normalized).replaceAll("");
        normalized = MULTIPLE_DASH.matcher(normalized).replaceAll("-");
        normalized = normalized.replaceAll("^-|-$", "");
        if (normalized.isEmpty()) {
            return "post-" + System.currentTimeMillis();
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}
