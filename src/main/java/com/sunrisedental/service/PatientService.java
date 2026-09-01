package com.sunrisedental.service;

import com.sunrisedental.dao.DaoException;
import com.sunrisedental.dao.PatientDao;
import com.sunrisedental.model.Patient;
import com.sunrisedental.pattern.factory.DaoFactory;
import com.sunrisedental.util.ValidationUtil;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public class PatientService {

    private static final Set<String> GENDERS = Set.of("MALE", "FEMALE", "OTHER");

    private final PatientDao patients = DaoFactory.get().patients();

    public Map<String, Object> list(String query) {
        return Map.of("success", true, "message", "Patient files loaded.", "patients", patients.search(query));
    }

    public Map<String, Object> save(Integer patientId, String fullName, String address, String contactNumber,
                                    String email, String dateOfBirth, String gender, String notes) {
        String error = ValidationUtil.firstError(
                ValidationUtil.require(fullName, "Patient name"),
                ValidationUtil.require(address, "Address"),
                ValidationUtil.phone(contactNumber),
                ValidationUtil.emailOptional(email)
        );
        if (error != null) {
            return fail(error);
        }
        String genderValue = blank(gender);
        if (genderValue != null && !GENDERS.contains(genderValue.toUpperCase())) {
            return fail("Choose Male, Female or Other.");
        }
        Date dob;
        try {
            dob = parseDate(dateOfBirth);
        } catch (IllegalArgumentException e) {
            return fail("Enter a valid date of birth.");
        }
        Patient patient = new Patient();
        patient.setFullName(fullName.trim());
        patient.setAddress(address.trim());
        patient.setContactNumber(contactNumber.trim());
        patient.setEmail(blank(email));
        patient.setDateOfBirth(dob);
        patient.setGender(genderValue == null ? null : genderValue.toUpperCase());
        patient.setNotes(blank(notes));
        if (patientId == null || patientId <= 0) {
            int id = patients.insert(patient);
            return Map.of("success", true, "message", "Patient file saved.", "patient", patients.findById(id));
        }
        if (patients.findById(patientId) == null) {
            return fail("Patient file was not found.");
        }
        patient.setPatientId(patientId);
        patients.update(patient);
        return Map.of("success", true, "message", "Patient details updated.", "patient", patients.findById(patientId));
    }

    public Map<String, Object> remove(int patientId) {
        if (patients.findById(patientId) == null) {
            return fail("Patient file was not found.");
        }
        if (patients.hasAppointments(patientId)) {
            return fail("This patient has appointments on file. Update their details instead of deleting the record.");
        }
        try {
            patients.delete(patientId);
            return Map.of("success", true, "message", "Patient file removed.");
        } catch (DaoException e) {
            return fail("This patient could not be removed because clinic records still refer to the file.");
        }
    }

    private Date parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Date.valueOf(LocalDate.parse(value.trim()));
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Map<String, Object> fail(String message) {
        return Map.of("success", false, "message", message);
    }
}