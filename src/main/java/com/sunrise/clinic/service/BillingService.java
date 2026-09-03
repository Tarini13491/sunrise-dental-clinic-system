package com.sunrise.clinic.service;

import com.sunrise.clinic.billing.BillAmounts;
import com.sunrise.clinic.billing.BillCalculator;
import com.sunrise.clinic.catalog.TreatmentCatalog;
import com.sunrise.clinic.dao.AppointmentDao;
import com.sunrise.clinic.dao.BillDao;
import com.sunrise.clinic.dao.PatientDao;
import com.sunrise.clinic.exception.ConflictException;
import com.sunrise.clinic.exception.NotFoundException;
import com.sunrise.clinic.exception.ValidationException;
import com.sunrise.clinic.model.Appointment;
import com.sunrise.clinic.model.AppointmentStatus;
import com.sunrise.clinic.model.Bill;
import com.sunrise.clinic.model.BillRecord;
import com.sunrise.clinic.model.Patient;
import com.sunrise.clinic.model.SessionUser;
import com.sunrise.clinic.model.Treatment;
import com.sunrise.clinic.security.AccessPolicy;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BillingService {
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;
    private final BillDao bills;
    private final AppointmentDao appointments;
    private final PatientDao patients;
    private final BillCalculator calculator;

    public BillingService(BillDao bills, AppointmentDao appointments, PatientDao patients) {
        this(bills, appointments, patients, new BillCalculator());
    }

    public BillingService(BillDao bills, AppointmentDao appointments, PatientDao patients, BillCalculator calculator) {
        this.bills = bills;
        this.appointments = appointments;
        this.patients = patients;
        this.calculator = calculator;
    }

    public List<BillRecord> list(SessionUser actor) {
        AccessPolicy.requireUser(actor);
        return bills.findAll();
    }

    public BillRecord find(SessionUser actor, int billId) {
        AccessPolicy.requireUser(actor);
        return bills.findRecordById(billId)
                .orElseThrow(() -> new NotFoundException("Billing record was not found."));
    }

    public BillAmounts preview(SessionUser actor, Integer appointmentId) {
        AccessPolicy.requireStaff(actor);
        Appointment appointment = loadAppointment(appointmentId);
        Treatment treatment = TreatmentCatalog.require(appointment.getTreatmentType());
        return calculator.calculate(treatment);
    }

    public BillRecord create(SessionUser actor, Integer appointmentId) {
        AccessPolicy.requireStaff(actor);
        Appointment appointment = loadAppointment(appointmentId);
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ValidationException("A cancelled appointment cannot be billed.");
        }
        if (bills.findByAppointmentId(appointment.getAppointmentId()).isPresent()) {
            throw new ConflictException("A bill has already been created for this appointment.");
        }
        Patient patient = patients.findById(appointment.getPatientId())
                .orElseThrow(() -> new NotFoundException("Patient record was not found."));
        Treatment treatment = TreatmentCatalog.require(appointment.getTreatmentType());
        BillAmounts amounts = calculator.calculate(treatment);
        Bill bill = new Bill();
        bill.setBillNumber(nextBillNumber());
        bill.setAppointmentId(appointment.getAppointmentId());
        bill.setTreatmentType(treatment.getName());
        bill.setTreatmentCost(amounts.getTreatmentCost());
        bill.setConsultationFee(amounts.getConsultationFee());
        bill.setTotalAmount(amounts.getTotalAmount());
        bill.setIssuedBy(actor.getUserId());
        bill.setBillId(bills.insert(bill));
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointments.update(appointment);
        return new BillRecord(bill, appointment, patient);
    }

    private Appointment loadAppointment(Integer appointmentId) {
        if (appointmentId == null) {
            throw new ValidationException("An appointment must be selected.");
        }
        return appointments.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment record was not found."));
    }

    private String nextBillNumber() {
        int sequence = bills.countForDate(LocalDate.now()) + 1;
        return "BILL-" + LocalDate.now().format(DAY) + "-" + String.format("%04d", sequence);
    }
}
