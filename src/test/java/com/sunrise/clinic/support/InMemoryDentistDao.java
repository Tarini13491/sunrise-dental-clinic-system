package com.sunrise.clinic.support;

import com.sunrise.clinic.dao.DentistDao;
import com.sunrise.clinic.model.Dentist;
import com.sunrise.clinic.model.DentistStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryDentistDao implements DentistDao {
    private final List<Dentist> store = new ArrayList<>();
    private final AtomicInteger sequence = new AtomicInteger(1);

    @Override
    public Optional<Dentist> findById(int dentistId) {
        return store.stream().filter(dentist -> dentist.getDentistId() == dentistId).findFirst();
    }

    @Override
    public Optional<Dentist> findByName(String fullName) {
        return store.stream().filter(dentist -> dentist.getFullName().equalsIgnoreCase(fullName)).findFirst();
    }

    @Override
    public List<Dentist> findAll() {
        return store.stream()
                .sorted((left, right) -> left.getFullName().compareToIgnoreCase(right.getFullName()))
                .toList();
    }

    @Override
    public List<Dentist> findActive() {
        return store.stream()
                .filter(Dentist::isActive)
                .sorted((left, right) -> left.getFullName().compareToIgnoreCase(right.getFullName()))
                .toList();
    }

    @Override
    public int insert(Dentist dentist) {
        dentist.setDentistId(sequence.getAndIncrement());
        dentist.setCreatedAt(LocalDateTime.now());
        store.add(dentist);
        return dentist.getDentistId();
    }

    @Override
    public void updateStatus(int dentistId, DentistStatus status) {
        findById(dentistId).ifPresent(dentist -> dentist.setStatus(status));
    }
}
