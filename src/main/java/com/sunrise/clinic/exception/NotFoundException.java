package com.sunrise.clinic.exception;

public class NotFoundException extends ClinicException {
    public NotFoundException(String message) {
        super(message, 404);
    }
}
