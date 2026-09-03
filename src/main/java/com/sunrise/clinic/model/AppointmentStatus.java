package com.sunrise.clinic.model;

public enum AppointmentStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED;

    public static AppointmentStatus from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Appointment status is required.");
        }
        return AppointmentStatus.valueOf(value.trim().toUpperCase());
    }
}
