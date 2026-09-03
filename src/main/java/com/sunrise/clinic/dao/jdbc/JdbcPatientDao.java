package com.sunrise.clinic.dao.jdbc;

import com.sunrise.clinic.config.DatabaseManager;
import com.sunrise.clinic.dao.PatientDao;
import com.sunrise.clinic.exception.ClinicException;
import com.sunrise.clinic.model.Patient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcPatientDao implements PatientDao {
    @Override
    public Optional<Patient> findById(int patientId) {
        return queryOne("SELECT * FROM patients WHERE patient_id = ?", statement -> statement.setInt(1, patientId));
    }

    @Override
    public Optional<Patient> findByCode(String patientCode) {
        return queryOne("SELECT * FROM patients WHERE patient_code = ?", statement -> statement.setString(1, patientCode));
    }

    @Override
    public Optional<Patient> findByContact(String contactNumber) {
        return queryOne("SELECT * FROM patients WHERE contact_number = ?", statement -> statement.setString(1, contactNumber));
    }

    @Override
    public List<Patient> findAll() {
        return queryList("SELECT * FROM patients ORDER BY full_name", statement -> {
        });
    }

    @Override
    public List<Patient> search(String query) {
        String like = "%" + query + "%";
        return queryList(
                "SELECT * FROM patients WHERE full_name LIKE ? OR patient_code LIKE ? OR contact_number LIKE ? ORDER BY full_name",
                statement -> {
                    statement.setString(1, like);
                    statement.setString(2, like);
                    statement.setString(3, like);
                }
        );
    }

    @Override
    public int nextCodeSequence() {
        String sql = "SELECT COUNT(*) FROM patients";
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1) + 1;
            }
            return 1;
        } catch (SQLException exception) {
            throw wrap();
        }
    }

    @Override
    public boolean existsByContact(String contactNumber, Integer excludePatientId) {
        String sql = excludePatientId == null
                ? "SELECT COUNT(*) FROM patients WHERE contact_number = ?"
                : "SELECT COUNT(*) FROM patients WHERE contact_number = ? AND patient_id <> ?";
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, contactNumber);
            if (excludePatientId != null) {
                statement.setInt(2, excludePatientId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        } catch (SQLException exception) {
            throw wrap();
        }
    }

    @Override
    public int insert(Patient patient) {
        String sql = "INSERT INTO patients (patient_code, full_name, age, address, contact_number) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, patient.getPatientCode());
            statement.setString(2, patient.getFullName());
            statement.setInt(3, patient.getAge());
            statement.setString(4, patient.getAddress());
            statement.setString(5, patient.getContactNumber());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new ClinicException("Unable to register the patient.", 500);
        } catch (SQLException exception) {
            throw wrap();
        }
    }

    @Override
    public void update(Patient patient) {
        String sql = "UPDATE patients SET full_name = ?, age = ?, address = ?, contact_number = ? WHERE patient_id = ?";
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, patient.getFullName());
            statement.setInt(2, patient.getAge());
            statement.setString(3, patient.getAddress());
            statement.setString(4, patient.getContactNumber());
            statement.setInt(5, patient.getPatientId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw wrap();
        }
    }

    private Optional<Patient> queryOne(String sql, SqlConsumer binder) {
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.accept(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw wrap();
        }
    }

    private List<Patient> queryList(String sql, SqlConsumer binder) {
        List<Patient> patients = new ArrayList<>();
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.accept(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    patients.add(map(resultSet));
                }
            }
            return patients;
        } catch (SQLException exception) {
            throw wrap();
        }
    }

    private Patient map(ResultSet resultSet) throws SQLException {
        Patient patient = new Patient();
        patient.setPatientId(resultSet.getInt("patient_id"));
        patient.setPatientCode(resultSet.getString("patient_code"));
        patient.setFullName(resultSet.getString("full_name"));
        patient.setAge(resultSet.getInt("age"));
        patient.setAddress(resultSet.getString("address"));
        patient.setContactNumber(resultSet.getString("contact_number"));
        Timestamp created = resultSet.getTimestamp("created_at");
        if (created != null) {
            patient.setCreatedAt(created.toLocalDateTime());
        }
        return patient;
    }

    private ClinicException wrap() {
        return new ClinicException("Unable to complete the patient request. Please try again.", 500);
    }

    @FunctionalInterface
    private interface SqlConsumer {
        void accept(PreparedStatement statement) throws SQLException;
    }
}
