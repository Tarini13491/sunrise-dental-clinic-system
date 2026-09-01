package com.sunrisedental.dao;

import com.sunrisedental.model.Patient;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class PatientDao extends BaseDao {

    public List<Patient> search(String query) {
        return withConnection(c -> {
            String like = query == null || query.isBlank() ? "%" : "%" + query.trim() + "%";
            List<Patient> list = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM patients WHERE full_name LIKE ? OR contact_number LIKE ? OR IFNULL(email,'') LIKE ? "
                            + "ORDER BY full_name")) {
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(map(rs));
                    }
                }
            }
            return list;
        });
    }

    public Patient findById(int patientId) {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM patients WHERE patient_id = ?")) {
                ps.setInt(1, patientId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? map(rs) : null;
                }
            }
        });
    }

    public int insert(Patient patient) {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO patients (full_name, address, contact_number, email, date_of_birth, gender, notes) "
                            + "VALUES (?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                bind(ps, patient);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
                return 0;
            }
        });
    }

    public void update(Patient patient) {
        withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE patients SET full_name = ?, address = ?, contact_number = ?, email = ?, "
                            + "date_of_birth = ?, gender = ?, notes = ? WHERE patient_id = ?")) {
                bind(ps, patient);
                ps.setInt(8, patient.getPatientId());
                ps.executeUpdate();
                return null;
            }
        });
    }

    public boolean hasAppointments(int patientId) {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM appointments WHERE patient_id = ?")) {
                ps.setInt(1, patientId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1) > 0;
                }
            }
        });
    }

    public boolean delete(int patientId) {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM patients WHERE patient_id = ?")) {
                ps.setInt(1, patientId);
                return ps.executeUpdate() > 0;
            }
        });
    }

    private void bind(PreparedStatement ps, Patient patient) throws SQLException {
        ps.setString(1, patient.getFullName());
        ps.setString(2, patient.getAddress());
        ps.setString(3, patient.getContactNumber());
        ps.setString(4, patient.getEmail());
        if (patient.getDateOfBirth() == null) {
            ps.setNull(5, Types.DATE);
        } else {
            ps.setDate(5, patient.getDateOfBirth());
        }
        if (patient.getGender() == null || patient.getGender().isBlank()) {
            ps.setNull(6, Types.VARCHAR);
        } else {
            ps.setString(6, patient.getGender());
        }
        ps.setString(7, patient.getNotes());
    }

    private Patient map(ResultSet rs) throws SQLException {
        Patient patient = new Patient();
        patient.setPatientId(rs.getInt("patient_id"));
        patient.setFullName(rs.getString("full_name"));
        patient.setAddress(rs.getString("address"));
        patient.setContactNumber(rs.getString("contact_number"));
        patient.setEmail(rs.getString("email"));
        Date dob = rs.getDate("date_of_birth");
        patient.setDateOfBirth(dob);
        patient.setGender(rs.getString("gender"));
        patient.setNotes(rs.getString("notes"));
        return patient;
    }
}