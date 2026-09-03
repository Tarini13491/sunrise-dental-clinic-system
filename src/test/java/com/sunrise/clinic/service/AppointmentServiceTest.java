package com.sunrise.clinic.service;

import com.sunrise.clinic.exception.ConflictException;
import com.sunrise.clinic.exception.ForbiddenException;
import com.sunrise.clinic.exception.NotFoundException;
import com.sunrise.clinic.exception.ValidationException;
import com.sunrise.clinic.model.AppointmentRecord;
import com.sunrise.clinic.model.AppointmentStatus;
import com.sunrise.clinic.model.Patient;
import com.sunrise.clinic.model.Role;
import com.sunrise.clinic.model.SessionUser;
import com.sunrise.clinic.support.InMemoryAppointmentDao;
import com.sunrise.clinic.support.InMemoryDentistDao;
import com.sunrise.clinic.support.InMemoryPatientDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppointmentServiceTest {
    private AppointmentService appointmentService;
    private Patient patient;
    private final SessionUser staff = new SessionUser(2, "nimal", "Nimal Perera", Role.STAFF);
    private final SessionUser admin = new SessionUser(1, "admin", "Clinic Administrator", Role.ADMIN);

    @BeforeEach
    void setUp() {
        InMemoryPatientDao patients = new InMemoryPatientDao();
        InMemoryDentistDao dentists = new InMemoryDentistDao();
        appointmentService = new AppointmentService(new InMemoryAppointmentDao(patients), patients, dentists);
        PatientService patientService = new PatientService(patients);
        patient = patientService.register(staff, "Ayesha Fernando", 29, "12 Flower Road, Colombo", "0771234567");
        new DentistService(dentists).register(admin, "Dr Anika Perera");
    }

    @Test
    void staffCanCreateAndSearchAppointments() {
        AppointmentRecord created = appointmentService.register(staff, patient.getPatientId(), "Dr Anika Perera", "Tooth Filling", nextOpenDay(), "09:00");
        assertTrue(created.getAppointmentNumber().startsWith("APT-"));
        AppointmentRecord found = appointmentService.searchByNumber(staff, created.getAppointmentNumber());
        assertEquals("Ayesha Fernando", found.getPatientName());
        assertEquals("Dr Anika Perera", found.getDentistName());
        assertEquals("Tooth Filling", found.getTreatmentType());
    }

    @Test
    void adminCannotCreateAppointmentsButCanSearchThem() {
        AppointmentRecord created = appointmentService.register(staff, patient.getPatientId(), "Dr Anika Perera", "Tooth Filling", nextOpenDay(), "09:00");
        assertThrows(ForbiddenException.class, () -> appointmentService.register(
                admin, patient.getPatientId(), "Dr Anika Perera", "Tooth Filling", nextOpenDay(), "10:00"));
        AppointmentRecord found = appointmentService.searchByNumber(admin, created.getAppointmentNumber());
        assertEquals(created.getAppointmentNumber(), found.getAppointmentNumber());
    }

    @Test
    void preventsDoubleBookingTheSameDentistSlot() {
        String date = nextOpenDay();
        appointmentService.register(staff, patient.getPatientId(), "Dr Anika Perera", "Tooth Filling", date, "09:00");
        assertThrows(ConflictException.class, () -> appointmentService.register(
                staff, patient.getPatientId(), "Dr Anika Perera", "Dental X-Ray", date, "09:00"));
    }

    @Test
    void searchValidatesEmptyInvalidAndMissingNumbers() {
        ValidationException empty = assertThrows(ValidationException.class, () -> appointmentService.searchByNumber(staff, " "));
        assertEquals("Enter a patient name, dentist name, or appointment number.", empty.getMessage());
        ValidationException invalid = assertThrows(ValidationException.class, () -> appointmentService.searchByNumber(staff, "APT-1"));
        assertTrue(invalid.getMessage().contains("valid appointment number"));
        NotFoundException missing = assertThrows(NotFoundException.class, () -> appointmentService.searchByNumber(staff, "APT-20260901-0099"));
        assertEquals("No appointment was found for that number.", missing.getMessage());
    }

    @Test
    void staffAndAdminCanSearchAppointmentsByPatientName() {
        appointmentService.register(staff, patient.getPatientId(), "Dr Anika Perera", "Tooth Filling", nextOpenDay(), "09:00");
        List<AppointmentRecord> byPatient = appointmentService.search(staff, "Ayesha");
        assertEquals(1, byPatient.size());
        assertEquals("Ayesha Fernando", byPatient.get(0).getPatientName());
        List<AppointmentRecord> byDentist = appointmentService.search(admin, "Anika");
        assertEquals(1, byDentist.size());
        assertEquals("Dr Anika Perera", byDentist.get(0).getDentistName());
    }

    @Test
    void rejectsADentistWhoIsNotOnTheClinicList() {
        ValidationException missing = assertThrows(ValidationException.class, () -> appointmentService.register(
                staff, patient.getPatientId(), "Dr Unknown", "Tooth Filling", nextOpenDay(), "09:00"));
        assertEquals("Select a dentist from the clinic list.", missing.getMessage());
    }

    @Test
    void staffCanCancelAScheduledAppointmentAndFreeTheSlot() {
        String date = nextOpenDay();
        AppointmentRecord created = appointmentService.register(staff, patient.getPatientId(), "Dr Anika Perera", "Tooth Filling", date, "09:00");
        AppointmentRecord cancelled = appointmentService.cancel(staff, created.getAppointment().getAppointmentId());
        assertEquals(AppointmentStatus.CANCELLED, cancelled.getAppointment().getStatus());
        AppointmentRecord rebooked = appointmentService.register(staff, patient.getPatientId(), "Dr Anika Perera", "Dental X-Ray", date, "09:00");
        assertEquals("Dental X-Ray", rebooked.getTreatmentType());
    }

    @Test
    void adminCannotCancelAppointmentsAndCompletedOnesStayBooked() {
        AppointmentRecord created = appointmentService.register(staff, patient.getPatientId(), "Dr Anika Perera", "Tooth Filling", nextOpenDay(), "09:00");
        assertThrows(ForbiddenException.class, () -> appointmentService.cancel(admin, created.getAppointment().getAppointmentId()));
        appointmentService.update(staff, created.getAppointment().getAppointmentId(), "Dr Anika Perera", "Tooth Filling", created.getAppointmentDate().toString(), "09:00", "COMPLETED");
        ValidationException completed = assertThrows(ValidationException.class, () -> appointmentService.cancel(staff, created.getAppointment().getAppointmentId()));
        assertEquals("A completed appointment cannot be cancelled.", completed.getMessage());
    }

    @Test
    void staffCanRestoreACancelledAppointment() {
        String date = nextOpenDay();
        AppointmentRecord created = appointmentService.register(staff, patient.getPatientId(), "Dr Anika Perera", "Tooth Filling", date, "09:00");
        appointmentService.cancel(staff, created.getAppointment().getAppointmentId());
        AppointmentRecord restored = appointmentService.restore(staff, created.getAppointment().getAppointmentId());
        assertEquals(AppointmentStatus.SCHEDULED, restored.getAppointment().getStatus());
    }

    private String nextOpenDay() {
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date.toString();
    }
}
