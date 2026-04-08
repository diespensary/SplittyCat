package me.khromov.splittycat.common.util;

import java.util.Locale;

public final class InputNormalizer {

    private InputNormalizer() {
    }

    public static String trim(String value) {
        return value == null ? null : value.trim();
    }

    public static String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }

    public static String trimOrEmpty(String value) {
        String trimmed = trim(value);
        return trimmed == null ? "" : trimmed;
    }

    public static String lowercase(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    public static String uppercase(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
}
