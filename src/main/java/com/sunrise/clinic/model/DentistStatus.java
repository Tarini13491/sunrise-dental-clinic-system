package com.sunrise.clinic.model;

public enum DentistStatus {
    ACTIVE,
    INACTIVE;

    public static DentistStatus from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Dentist status is required.");
        }
        return DentistStatus.valueOf(value.trim().toUpperCase());
    }
}
