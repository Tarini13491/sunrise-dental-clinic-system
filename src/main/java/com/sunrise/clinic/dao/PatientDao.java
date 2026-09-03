package com.sunrise.clinic.dao;

import com.sunrise.clinic.model.Patient;

import java.util.List;
import java.util.Optional;

public interface PatientDao {
    Optional<Patient> findById(int patientId);

    Optional<Patient> findByCode(String patientCode);

    Optional<Patient> findByContact(String contactNumber);

    List<Patient> findAll();

    List<Patient> search(String query);

    int nextCodeSequence();

    boolean existsByContact(String contactNumber, Integer excludePatientId);

    int insert(Patient patient);

    void update(Patient patient);
}
