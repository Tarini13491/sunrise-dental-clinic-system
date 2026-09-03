package com.sunrise.clinic.service;

import com.sunrise.clinic.dao.PatientDao;
import com.sunrise.clinic.exception.ConflictException;
import com.sunrise.clinic.exception.NotFoundException;
import com.sunrise.clinic.model.Patient;
import com.sunrise.clinic.model.SessionUser;
import com.sunrise.clinic.security.AccessPolicy;
import com.sunrise.clinic.validation.FieldValidator;

import java.util.List;

public class PatientService {
    private final PatientDao patients;

    public PatientService(PatientDao patients) {
        this.patients = patients;
    }

    public List<Patient> list(SessionUser actor, String query) {
        AccessPolicy.requireUser(actor);
        if (query == null || query.isBlank()) {
            return patients.findAll();
        }
        return patients.search(query.trim());
    }

    public Patient find(SessionUser actor, int patientId) {
        AccessPolicy.requireUser(actor);
        return patients.findById(patientId)
                .orElseThrow(() -> new NotFoundException("Patient record was not found."));
    }

    public Patient register(SessionUser actor, String fullName, Integer age, String address, String contactNumber) {
        AccessPolicy.requireStaff(actor);
        Patient patient = new Patient();
        patient.setFullName(FieldValidator.personName(fullName, "Patient name"));
        patient.setAge(FieldValidator.age(age));
        patient.setAddress(FieldValidator.address(address));
        patient.setContactNumber(FieldValidator.contactNumber(contactNumber));
        if (patients.existsByContact(patient.getContactNumber(), null)) {
            throw new ConflictException("A patient with that contact number is already registered.");
        }
        patient.setPatientCode(nextPatientCode());
        patient.setPatientId(patients.insert(patient));
        return patient;
    }

    public Patient update(SessionUser actor, int patientId, String fullName, Integer age, String address, String contactNumber) {
        AccessPolicy.requireStaff(actor);
        Patient patient = patients.findById(patientId)
                .orElseThrow(() -> new NotFoundException("Patient record was not found."));
        patient.setFullName(FieldValidator.personName(fullName, "Patient name"));
        patient.setAge(FieldValidator.age(age));
        patient.setAddress(FieldValidator.address(address));
        patient.setContactNumber(FieldValidator.contactNumber(contactNumber));
        if (patients.existsByContact(patient.getContactNumber(), patient.getPatientId())) {
            throw new ConflictException("A patient with that contact number is already registered.");
        }
        patients.update(patient);
        return patient;
    }

    private String nextPatientCode() {
        return String.format("PAT-%04d", patients.nextCodeSequence());
    }
}
