package com.monocept.project.util;

/**
 * Masks personal data before it is written to the application log
 * (LOG-RUL-003: sensitive customer data should not be logged unnecessarily).
 */
public final class LogMaskUtil {

    private LogMaskUtil() {
    }

    /** "john.doe@gmail.com" -> "jo***@gmail.com" */
    public static String email(String email) {
        if (email == null || email.isBlank()) {
            return "<empty>";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = email.substring(0, at);
        String visible = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return visible + "***" + email.substring(at);
    }

    /** "+919876543210" -> "******3210" */
    public static String phone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "***";
        }
        return "******" + phone.substring(phone.length() - 4);
    }
}