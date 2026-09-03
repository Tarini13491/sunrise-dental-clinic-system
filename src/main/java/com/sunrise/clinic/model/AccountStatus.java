package com.sunrise.clinic.model;

public enum AccountStatus {
    ACTIVE,
    BLOCKED;

    public static AccountStatus from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Account status is required.");
        }
        return AccountStatus.valueOf(value.trim().toUpperCase());
    }
}
