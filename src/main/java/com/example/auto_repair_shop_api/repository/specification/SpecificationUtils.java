package com.example.auto_repair_shop_api.repository.specification;

import java.util.Locale;

/**
 * Shared helpers for Specifications that build LIKE predicates from user input.
 */
public final class SpecificationUtils {

    /** Escape character used with every LIKE built from user input. */
    public static final char LIKE_ESCAPE = '\\';

    private SpecificationUtils() {
    }

    /** Escapes \, % and _ so user input is matched literally inside a LIKE pattern. */
    public static String escapeLike(String value) {
        // Escape the escape character first, then the LIKE wildcards.
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /** "%value%" trimmed, lower-cased and escaped, ready for cb.like(cb.lower(...), pattern, LIKE_ESCAPE). */
    public static String containsPattern(String value) {
        return "%" + escapeLike(value.trim().toLowerCase(Locale.ROOT)) + "%";
    }
}
