package com.sunrise.clinic.exception;

public class ClinicException extends RuntimeException {
    private final int statusCode;

    public ClinicException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
