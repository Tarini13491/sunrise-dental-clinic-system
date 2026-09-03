package com.sunrise.clinic.dao.jdbc;

import com.sunrise.clinic.config.DatabaseManager;
import com.sunrise.clinic.dao.AppointmentDao;
import com.sunrise.clinic.exception.ClinicException;
import com.sunrise.clinic.model.Appointment;
import com.sunrise.clinic.model.AppointmentRecord;
import com.sunrise.clinic.model.AppointmentStatus;
import com.sunrise.clinic.model.Patient;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcAppointmentDao implements AppointmentDao {
    private static final String JOIN_SQL = """
            SELECT a.*, p.patient_code, p.full_name AS patient_name, p.age, p.address, p.contact_number, p.created_at AS patient_created_at
            FROM appointments a
            INNER JOIN patients p ON p.patient_id = a.patient_id
            """;

    @Override
    public Optional<Appointment> findById(int appointmentId) {
        String sql = "SELECT * FROM appointments WHERE appointment_id = ?";
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, appointmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapAppointment(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw wrap();
        }
    }

    @Override
    public Optional<AppointmentRecord> findByNumber(String appointmentNumber) {
        String sql = JOIN_SQL + " WHERE a.appointment_number = ?";
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, appointmentNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRecord(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw wrap();
        }
    }

    @Override
    public List<AppointmentRecord> searchByName(String query) {
        String sql = JOIN_SQL + """
                 WHERE LOWER(p.full_name) LIKE ? OR LOWER(a.dentist_name) LIKE ? OR LOWER(a.appointment_number) LIKE ?
                 ORDER BY a.appointment_date DESC, a.appointment_time DESC
                """;
        String pattern = "%" + query.toLowerCase() + "%";
        List<AppointmentRecord> records = new ArrayList<>();
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    records.add(mapRecord(resultSet));
                }
            }
            return records;
        } catch (SQLException exception) {
            throw wrap();
        }
    }

    @Override
    public List<AppointmentRecord> findAll() {
        String sql = JOIN_SQL + " ORDER BY a.appointment_date DESC, a.appointment_time DESC";
        List<AppointmentRecord> records = new ArrayList<>();
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                records.add(mapRecord(resultSet));
            }
            return records;
        } catch (SQLException exception) {
            throw wrap();
        }
    }

    @Override
    public int countForDate(LocalDate date) {
        String sql = "SELECT COUNT(*) FROM appointments WHERE appointment_date = ?";
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(date));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return 0;
            }
        } catch (SQLException exception) {
            throw wrap();
        }
    }

    @Override
    public boolean dentistSlotTaken(String dentistName, LocalDate date, LocalTime time, Integer excludeAppointmentId) {
        String sql = """
                SELECT COUNT(*) FROM appointments
                WHERE dentist_name = ? AND appointment_date = ? AND appointment_time = ? AND status <> ?
                """;
        if (excludeAppointmentId != null) {
            sql += " AND appointment_id <> ?";
        }
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, dentistName);
            statement.setDate(2, Date.valueOf(date));
            statement.setTime(3, Time.valueOf(time));
            statement.setString(4, AppointmentStatus.CANCELLED.name());
            if (excludeAppointmentId != null) {
                statement.setInt(5, excludeAppointmentId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        } catch (SQLException exception) {
            throw wrap();
        }
    }

    @Override
    public boolean patientSlotTaken(int patientId, LocalDate date, LocalTime time, Integer excludeAppointmentId) {
        String sql = """
                SELECT COUNT(*) FROM appointments
                WHERE patient_id = ? AND appointment_date = ? AND appointment_time = ? AND status <> ?
                """;
        if (excludeAppointmentId != null) {
            sql += " AND appointment_id <> ?";
        }
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            statement.setDate(2, Date.valueOf(date));
            statement.setTime(3, Time.valueOf(time));
            statement.setString(4, AppointmentStatus.CANCELLED.name());
            if (excludeAppointmentId != null) {
                statement.setInt(5, excludeAppointmentId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        } catch (SQLException exception) {
            throw wrap();
        }
    }

    @Override
    public int insert(Appointment appointment) {
        String sql = """
                INSERT INTO appointments (appointment_number, patient_id, dentist_name, treatment_type, appointment_date, appointment_time, status, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, appointment.getAppointmentNumber());
            statement.setInt(2, appointment.getPatientId());
            statement.setString(3, appointment.getDentistName());
            statement.setString(4, appointment.getTreatmentType());
            statement.setDate(5, Date.valueOf(appointment.getAppointmentDate()));
            statement.setTime(6, Time.valueOf(appointment.getAppointmentTime()));
            statement.setString(7, appointment.getStatus().name());
            statement.setInt(8, appointment.getCreatedBy());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new ClinicException("Unable to register the appointment.", 500);
        } catch (SQLException exception) {
            throw wrap();
        }
    }

    @Override
    public void update(Appointment appointment) {
        String sql = """
                UPDATE appointments
                SET dentist_name = ?, treatment_type = ?, appointment_date = ?, appointment_time = ?, status = ?
                WHERE appointment_id = ?
                """;
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, appointment.getDentistName());
            statement.setString(2, appointment.getTreatmentType());
            statement.setDate(3, Date.valueOf(appointment.getAppointmentDate()));
            statement.setTime(4, Time.valueOf(appointment.getAppointmentTime()));
            statement.setString(5, appointment.getStatus().name());
            statement.setInt(6, appointment.getAppointmentId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw wrap();
        }
    }

    private Appointment mapAppointment(ResultSet resultSet) throws SQLException {
        Appointment appointment = new Appointment();
        appointment.setAppointmentId(resultSet.getInt("appointment_id"));
        appointment.setAppointmentNumber(resultSet.getString("appointment_number"));
        appointment.setPatientId(resultSet.getInt("patient_id"));
        appointment.setDentistName(resultSet.getString("dentist_name"));
        appointment.setTreatmentType(resultSet.getString("treatment_type"));
        Date date = resultSet.getDate("appointment_date");
        Time time = resultSet.getTime("appointment_time");
        appointment.setAppointmentDate(date == null ? null : date.toLocalDate());
        appointment.setAppointmentTime(time == null ? null : time.toLocalTime());
        appointment.setStatus(AppointmentStatus.from(resultSet.getString("status")));
        appointment.setCreatedBy(resultSet.getInt("created_by"));
        Timestamp created = resultSet.getTimestamp("created_at");
        if (created != null) {
            appointment.setCreatedAt(created.toLocalDateTime());
        }
        return appointment;
    }

    private AppointmentRecord mapRecord(ResultSet resultSet) throws SQLException {
        Appointment appointment = mapAppointment(resultSet);
        Patient patient = new Patient();
        patient.setPatientId(appointment.getPatientId());
        patient.setPatientCode(resultSet.getString("patient_code"));
        patient.setFullName(resultSet.getString("patient_name"));
        patient.setAge(resultSet.getInt("age"));
        patient.setAddress(resultSet.getString("address"));
        patient.setContactNumber(resultSet.getString("contact_number"));
        Timestamp created = resultSet.getTimestamp("patient_created_at");
        if (created != null) {
            patient.setCreatedAt(created.toLocalDateTime());
        }
        return new AppointmentRecord(appointment, patient);
    }

    private ClinicException wrap() {
        return new ClinicException("Unable to complete the appointment request. Please try again.", 500);
    }
}
