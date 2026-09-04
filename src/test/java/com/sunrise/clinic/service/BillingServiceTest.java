package com.sunrise.clinic.service;

import com.sunrise.clinic.exception.ConflictException;
import com.sunrise.clinic.model.AppointmentRecord;
import com.sunrise.clinic.model.BillRecord;
import com.sunrise.clinic.model.Patient;
import com.sunrise.clinic.model.Role;
import com.sunrise.clinic.model.SessionUser;
import com.sunrise.clinic.support.InMemoryAppointmentDao;
import com.sunrise.clinic.support.InMemoryBillDao;
import com.sunrise.clinic.support.InMemoryDentistDao;
import com.sunrise.clinic.support.InMemoryPatientDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BillingServiceTest {
    private BillingService billingService;
    private AppointmentRecord appointment;
    private final SessionUser staff = new SessionUser(2, "nimal", "Nimal Perera", Role.STAFF);
    private final SessionUser admin = new SessionUser(1, "admin", "Clinic Administrator", Role.ADMIN);

    @BeforeEach
    void setUp() {
        InMemoryPatientDao patients = new InMemoryPatientDao();
        InMemoryAppointmentDao appointments = new InMemoryAppointmentDao(patients);
        InMemoryBillDao bills = new InMemoryBillDao(appointments, patients);
        InMemoryDentistDao dentists = new InMemoryDentistDao();
        billingService = new BillingService(bills, appointments, patients);
        PatientService patientService = new PatientService(patients);
        AppointmentService appointmentService = new AppointmentService(appointments, patients, dentists);
        Patient patient = patientService.register(staff, "Ayesha Fernando", 29, "12 Flower Road, Colombo", "0771234567");
        new DentistService(dentists).register(admin, "Dr Anika Perera");
        appointment = appointmentService.register(staff, patient.getPatientId(), "Dr Anika Perera", "Tooth Filling", nextOpenDay(), "09:00");
    }

    @Test
    void staffCanCalculateAndStoreABill() {
        BillRecord bill = billingService.create(staff, appointment.getAppointment().getAppointmentId());
        assertEquals(new BigDecimal("6000.00"), bill.getTreatmentCost());
        assertEquals(new BigDecimal("1500.00"), bill.getConsultationFee());
        assertEquals(new BigDecimal("7500.00"), bill.getTotalAmount());
        assertTrue(bill.getBillNumber().startsWith("BILL-"));
        assertEquals("Tooth Filling", bill.getTreatmentType());
    }

    @Test
    void adminCanCreateBills() {
        BillRecord bill = billingService.create(admin, appointment.getAppointment().getAppointmentId());
        assertEquals(new BigDecimal("7500.00"), bill.getTotalAmount());
        assertTrue(bill.getBillNumber().startsWith("BILL-"));
    }

    @Test
    void rejectsASecondBillForTheSameAppointment() {
        billingService.create(staff, appointment.getAppointment().getAppointmentId());
        assertThrows(ConflictException.class, () -> billingService.create(staff, appointment.getAppointment().getAppointmentId()));
    }

    private String nextOpenDay() {
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date.toString();
    }
}
