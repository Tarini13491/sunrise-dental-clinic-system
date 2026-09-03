package com.sunrise.clinic.support;

import com.sunrise.clinic.dao.BillDao;
import com.sunrise.clinic.model.Appointment;
import com.sunrise.clinic.model.Bill;
import com.sunrise.clinic.model.BillRecord;
import com.sunrise.clinic.model.Patient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryBillDao implements BillDao {
    private final List<Bill> store = new ArrayList<>();
    private final InMemoryAppointmentDao appointments;
    private final InMemoryPatientDao patients;
    private final AtomicInteger sequence = new AtomicInteger(1);

    public InMemoryBillDao(InMemoryAppointmentDao appointments, InMemoryPatientDao patients) {
        this.appointments = appointments;
        this.patients = patients;
    }

    @Override
    public Optional<Bill> findById(int billId) {
        return store.stream().filter(bill -> bill.getBillId() == billId).findFirst();
    }

    @Override
    public Optional<BillRecord> findRecordById(int billId) {
        return findById(billId).map(this::toRecord);
    }

    @Override
    public Optional<Bill> findByAppointmentId(int appointmentId) {
        return store.stream().filter(bill -> bill.getAppointmentId() == appointmentId).findFirst();
    }

    @Override
    public List<BillRecord> findAll() {
        return store.stream().map(this::toRecord).toList();
    }

    @Override
    public int countForDate(LocalDate date) {
        return (int) store.stream()
                .filter(bill -> bill.getIssuedAt() != null && bill.getIssuedAt().toLocalDate().equals(date))
                .count();
    }

    @Override
    public int insert(Bill bill) {
        bill.setBillId(sequence.getAndIncrement());
        bill.setIssuedAt(LocalDateTime.now());
        store.add(bill);
        return bill.getBillId();
    }

    private BillRecord toRecord(Bill bill) {
        Appointment appointment = appointments.findById(bill.getAppointmentId()).orElse(new Appointment());
        Patient patient = patients.findById(appointment.getPatientId()).orElse(new Patient());
        return new BillRecord(bill, appointment, patient);
    }
}
