package com.sunrise.clinic.exception;

public class ValidationException extends ClinicException {
    public ValidationException(String message) {
        super(message, 400);
    }
}
