package com.sunrise.clinic.exception;

public class ConflictException extends ClinicException {
    public ConflictException(String message) {
        super(message, 409);
    }
}
