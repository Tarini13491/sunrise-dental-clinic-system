package com.sunrise.clinic.model;

public enum Role {
    ADMIN,
    STAFF;

    public static Role from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Role is required.");
        }
        return Role.valueOf(value.trim().toUpperCase());
    }
}
