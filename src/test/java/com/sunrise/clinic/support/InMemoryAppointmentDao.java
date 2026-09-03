package com.sunrise.clinic.support;

import com.sunrise.clinic.dao.AppointmentDao;
import com.sunrise.clinic.model.Appointment;
import com.sunrise.clinic.model.AppointmentRecord;
import com.sunrise.clinic.model.Patient;
import com.sunrise.clinic.scheduling.AppointmentRules;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryAppointmentDao implements AppointmentDao {
    private final List<Appointment> store = new ArrayList<>();
    private final InMemoryPatientDao patients;
    private final AtomicInteger sequence = new AtomicInteger(1);

    public InMemoryAppointmentDao(InMemoryPatientDao patients) {
        this.patients = patients;
    }

    @Override
    public Optional<Appointment> findById(int appointmentId) {
        return store.stream().filter(item -> item.getAppointmentId() == appointmentId).findFirst();
    }

    @Override
    public Optional<AppointmentRecord> findByNumber(String appointmentNumber) {
        return store.stream()
                .filter(item -> item.getAppointmentNumber().equalsIgnoreCase(appointmentNumber))
                .map(this::toRecord)
                .findFirst();
    }

    @Override
    public List<AppointmentRecord> searchByName(String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        return store.stream()
                .map(this::toRecord)
                .filter(record -> record.getPatientName().toLowerCase(Locale.ROOT).contains(needle)
                        || record.getDentistName().toLowerCase(Locale.ROOT).contains(needle)
                        || record.getAppointmentNumber().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    @Override
    public List<AppointmentRecord> findAll() {
        return store.stream().map(this::toRecord).toList();
    }

    @Override
    public int countForDate(LocalDate date) {
        return (int) store.stream().filter(item -> item.getAppointmentDate().equals(date)).count();
    }

    @Override
    public boolean dentistSlotTaken(String dentistName, LocalDate date, LocalTime time, Integer excludeAppointmentId) {
        return store.stream()
                .filter(item -> excludeAppointmentId == null || item.getAppointmentId() != excludeAppointmentId)
                .anyMatch(item -> AppointmentRules.occupiesSlot(item, dentistName, date, time));
    }

    @Override
    public boolean patientSlotTaken(int patientId, LocalDate date, LocalTime time, Integer excludeAppointmentId) {
        return store.stream()
                .filter(item -> excludeAppointmentId == null || item.getAppointmentId() != excludeAppointmentId)
                .anyMatch(item -> AppointmentRules.occupiesPatientSlot(item, patientId, date, time));
    }

    @Override
    public int insert(Appointment appointment) {
        appointment.setAppointmentId(sequence.getAndIncrement());
        appointment.setCreatedAt(LocalDateTime.now());
        store.add(appointment);
        return appointment.getAppointmentId();
    }

    @Override
    public void update(Appointment appointment) {
        findById(appointment.getAppointmentId()).ifPresent(existing -> {
            existing.setDentistName(appointment.getDentistName());
            existing.setTreatmentType(appointment.getTreatmentType());
            existing.setAppointmentDate(appointment.getAppointmentDate());
            existing.setAppointmentTime(appointment.getAppointmentTime());
            existing.setStatus(appointment.getStatus());
        });
    }

    private AppointmentRecord toRecord(Appointment appointment) {
        Patient patient = patients.findById(appointment.getPatientId()).orElse(new Patient());
        return new AppointmentRecord(appointment, patient);
    }
}
