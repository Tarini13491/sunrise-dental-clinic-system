package com.sunrise.clinic.support;

import com.sunrise.clinic.dao.PatientDao;
import com.sunrise.clinic.model.Patient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryPatientDao implements PatientDao {
    private final List<Patient> store = new ArrayList<>();
    private final AtomicInteger sequence = new AtomicInteger(1);

    @Override
    public Optional<Patient> findById(int patientId) {
        return store.stream().filter(patient -> patient.getPatientId() == patientId).findFirst();
    }

    @Override
    public Optional<Patient> findByCode(String patientCode) {
        return store.stream().filter(patient -> patient.getPatientCode().equalsIgnoreCase(patientCode)).findFirst();
    }

    @Override
    public Optional<Patient> findByContact(String contactNumber) {
        return store.stream().filter(patient -> patient.getContactNumber().equals(contactNumber)).findFirst();
    }

    @Override
    public List<Patient> findAll() {
        return List.copyOf(store);
    }

    @Override
    public List<Patient> search(String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        return store.stream()
                .filter(patient -> patient.getFullName().toLowerCase(Locale.ROOT).contains(needle)
                        || patient.getPatientCode().toLowerCase(Locale.ROOT).contains(needle)
                        || patient.getContactNumber().contains(query))
                .toList();
    }

    @Override
    public int nextCodeSequence() {
        return store.size() + 1;
    }

    @Override
    public boolean existsByContact(String contactNumber, Integer excludePatientId) {
        return store.stream().anyMatch(patient -> patient.getContactNumber().equals(contactNumber)
                && (excludePatientId == null || patient.getPatientId() != excludePatientId));
    }

    @Override
    public int insert(Patient patient) {
        patient.setPatientId(sequence.getAndIncrement());
        patient.setCreatedAt(LocalDateTime.now());
        store.add(patient);
        return patient.getPatientId();
    }

    @Override
    public void update(Patient patient) {
        findById(patient.getPatientId()).ifPresent(existing -> {
            existing.setFullName(patient.getFullName());
            existing.setAge(patient.getAge());
            existing.setAddress(patient.getAddress());
            existing.setContactNumber(patient.getContactNumber());
        });
    }
}
