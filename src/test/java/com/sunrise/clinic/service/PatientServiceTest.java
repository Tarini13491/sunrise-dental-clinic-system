package com.sunrise.clinic.service;

import com.sunrise.clinic.exception.ConflictException;
import com.sunrise.clinic.exception.ForbiddenException;
import com.sunrise.clinic.exception.ValidationException;
import com.sunrise.clinic.model.Patient;
import com.sunrise.clinic.model.Role;
import com.sunrise.clinic.model.SessionUser;
import com.sunrise.clinic.support.InMemoryPatientDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientServiceTest {
    private PatientService patientService;
    private final SessionUser staff = new SessionUser(2, "nimal", "Nimal Perera", Role.STAFF);
    private final SessionUser admin = new SessionUser(1, "admin", "Clinic Administrator", Role.ADMIN);

    @BeforeEach
    void setUp() {
        patientService = new PatientService(new InMemoryPatientDao());
    }

    @Test
    void staffCanRegisterAndUpdatePatients() {
        Patient created = patientService.register(staff, "Ayesha Fernando", 29, "12 Flower Road, Colombo", "0771234567");
        assertEquals("PAT-0001", created.getPatientCode());
        Patient updated = patientService.update(staff, created.getPatientId(), "Ayesha Fernando", 30, "12 Flower Road, Colombo", "0771234567");
        assertEquals(30, updated.getAge());
    }

    @Test
    void adminCannotRegisterPatients() {
        assertThrows(ForbiddenException.class, () -> patientService.register(
                admin, "Ayesha Fernando", 29, "12 Flower Road, Colombo", "0771234567"));
    }

    @Test
    void rejectsInvalidPatientDataAndDuplicateContacts() {
        assertThrows(ValidationException.class, () -> patientService.register(staff, "Ayesha", 0, "12 Flower Road, Colombo", "0771234567"));
        patientService.register(staff, "Ayesha Fernando", 29, "12 Flower Road, Colombo", "0771234567");
        ConflictException error = assertThrows(ConflictException.class, () -> patientService.register(
                staff, "Nuwan Jayasuriya", 40, "88 Marine Drive, Colombo", "0771234567"));
        assertTrue(error.getMessage().contains("contact number"));
    }
}
