package com.sunrise.clinic.exception;

public class UnauthorizedException extends ClinicException {
    public UnauthorizedException(String message) {
        super(message, 401);
    }
}
