package com.sunrise.clinic.exception;

public class ForbiddenException extends ClinicException {
    public ForbiddenException(String message) {
        super(message, 403);
    }
}
