package com.monocept.project.util;

public final class TextNormalizationUtil {

    private TextNormalizationUtil() {
    }

    public static String trimAndCollapseSpaces(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    public static String toTitleCase(String value) {
        String cleaned = trimAndCollapseSpaces(value);
        if (cleaned == null || cleaned.isEmpty()) {
            return cleaned;
        }
        String[] words = cleaned.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
            }
        }
        return result.toString().trim();
    }

    public static String normalizeForComparison(String value) {
        String cleaned = trimAndCollapseSpaces(value);
        return cleaned == null ? null : cleaned.toLowerCase();
    }
}