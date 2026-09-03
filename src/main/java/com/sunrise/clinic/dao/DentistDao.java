package com.sunrise.clinic.dao;

import com.sunrise.clinic.model.Dentist;
import com.sunrise.clinic.model.DentistStatus;

import java.util.List;
import java.util.Optional;

public interface DentistDao {
    Optional<Dentist> findById(int dentistId);

    Optional<Dentist> findByName(String fullName);

    List<Dentist> findAll();

    List<Dentist> findActive();

    int insert(Dentist dentist);

    void updateStatus(int dentistId, DentistStatus status);
}
