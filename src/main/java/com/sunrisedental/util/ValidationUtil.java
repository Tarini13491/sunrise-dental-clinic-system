package com.sunrisedental.util;

import java.util.regex.Pattern;

public final class ValidationUtil {

    private static final Pattern PHONE = Pattern.compile("^[0-9+][0-9 ]{8,14}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private ValidationUtil() {
    }

    public static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            return field + " is required.";
        }
        return null;
    }

    public static String phone(String value) {
        if (value == null || value.isBlank()) {
            return "Contact number is required.";
        }
        String compact = value.replace("-", "").trim();
        if (!PHONE.matcher(compact).matches()) {
            return "Enter a valid contact number (e.g. 0771234567).";
        }
        return null;
    }

    public static String emailOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!EMAIL.matcher(value).matches()) {
            return "Enter a valid email address.";
        }
        return null;
    }

    public static String firstError(String... errors) {
        for (String error : errors) {
            if (error != null) {
                return error;
            }
        }
        return null;
    }
}
