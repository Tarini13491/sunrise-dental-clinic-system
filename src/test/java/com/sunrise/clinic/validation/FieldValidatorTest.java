package com.sunrise.clinic.validation;

import com.sunrise.clinic.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldValidatorTest {
    @Test
    void rejectsEmptyRequiredFields() {
        ValidationException error = assertThrows(ValidationException.class, () -> FieldValidator.required("  ", "Username"));
        assertEquals("Username is required.", error.getMessage());
    }

    @Test
    void acceptsValidUsernameAndRejectsInvalidUsername() {
        assertEquals("front_desk", FieldValidator.username("front_desk"));
        assertThrows(ValidationException.class, () -> FieldValidator.username("ab"));
        assertThrows(ValidationException.class, () -> FieldValidator.username("1staff"));
    }

    @Test
    void enforcesPasswordRules() {
        assertEquals("Clinic123", FieldValidator.password("Clinic123"));
        assertThrows(ValidationException.class, () -> FieldValidator.password("short1"));
        assertThrows(ValidationException.class, () -> FieldValidator.password("NoDigitsHere"));
    }

    @Test
    void validatesPersonNameAgeContactAndAddress() {
        assertEquals("Nimal Perera", FieldValidator.personName("Nimal Perera", "Patient name"));
        assertEquals(34, FieldValidator.age(34));
        assertEquals("0771234567", FieldValidator.contactNumber("0771234567"));
        assertEquals("42 Galle Road, Colombo", FieldValidator.address("42 Galle Road, Colombo"));
        assertThrows(ValidationException.class, () -> FieldValidator.age(0));
        assertThrows(ValidationException.class, () -> FieldValidator.contactNumber("771234567"));
        assertThrows(ValidationException.class, () -> FieldValidator.address("Col"));
    }

    @Test
    void validatesAppointmentNumberAndSchedule() {
        assertEquals("APT-20260901-0001", FieldValidator.appointmentNumber("apt-20260901-0001"));
        assertThrows(ValidationException.class, () -> FieldValidator.appointmentNumber("A1"));
        assertTrue(FieldValidator.isValidAppointmentNumberFormat("APT-20260901-0001"));
        LocalDate past = LocalDate.now().minusDays(1);
        assertThrows(ValidationException.class, () -> FieldValidator.appointmentDate(past.toString()));
        LocalDate sunday = LocalDate.now().plusDays(1);
        while (sunday.getDayOfWeek() != DayOfWeek.SUNDAY) {
            sunday = sunday.plusDays(1);
        }
        LocalDate closed = sunday;
        assertThrows(ValidationException.class, () -> FieldValidator.appointmentDate(closed.toString()));
        assertThrows(ValidationException.class, () -> FieldValidator.appointmentTime("07:30"));
        assertThrows(ValidationException.class, () -> FieldValidator.appointmentTime("09:15"));
        assertEquals("09:00", FieldValidator.appointmentTime("09:00").toString());
    }
}
