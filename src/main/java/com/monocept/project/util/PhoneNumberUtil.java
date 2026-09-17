package com.monocept.project.util;

import java.util.List;

/**
 * Single place that decides how an Indian mobile number is stored.
 *
 * Registration used to store "+919876543210" while My Profile and
 * Update Staff stored "9876543210". Because the duplicate checks compare
 * raw strings, the same phone could end up on two accounts. Every write
 * path now normalizes to +91XXXXXXXXXX, and duplicate checks look up both
 * shapes so rows saved before this fix are still caught.
 */
public final class PhoneNumberUtil {

    private static final String PREFIX = "+91";

    private PhoneNumberUtil() {
    }

    /**
     * Returns "+91" followed by the last 10 digits when the input looks like
     * an Indian mobile number (10 digits, optionally prefixed by 91 / +91 / 0).
     * Anything else is returned trimmed and unchanged so bean validation can
     * reject it with its normal message.
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) {
            digits = digits.substring(2);
        } else if (digits.length() == 11 && digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        if (digits.length() == 10) {
            return PREFIX + digits;
        }
        return raw.trim();
    }

    /** Both storage shapes of the same number: "+91XXXXXXXXXX" and "XXXXXXXXXX". */
    public static List<String> variants(String raw) {
        String normalized = normalize(raw);
        if (normalized != null && normalized.startsWith(PREFIX) && normalized.length() == 13) {
            return List.of(normalized, normalized.substring(3));
        }
        return normalized == null ? List.of() : List.of(normalized);
    }

    /** True when both values refer to the same phone number. */
    public static boolean sameNumber(String a, String b) {
        String left = normalize(a);
        String right = normalize(b);
        return left != null && left.equals(right);
    }
}