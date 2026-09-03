package com.sunrise.clinic.dao.jdbc;

import com.sunrise.clinic.config.DatabaseManager;
import com.sunrise.clinic.dao.BillDao;
import com.sunrise.clinic.exception.ClinicException;
import com.sunrise.clinic.model.Appointment;
import com.sunrise.clinic.model.AppointmentStatus;
import com.sunrise.clinic.model.Bill;
import com.sunrise.clinic.model.BillRecord;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcBillDao implements BillDao {
    private static final String JOIN_SQL = """
            SELECT b.*, a.appointment_number, a.patient_id, a.dentist_name, a.appointment_date, a.appointment_time, a.status,
                   p.patient_code, p.full_name AS patient_name, p.age, p.address, p.contact_number
            FROM bills b
            INNER JOIN appointments a ON a.appointment_id = b.appointment_id
            INNER JOIN patients p ON p.patient_id = a.patient_id
            """;

    @Override
    public Optional<Bill> findById(int billId) {
        String sql = "SELECT * FROM bills WHERE bill_id = ?";
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, billId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapBill(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw wrap();
        }
    }

    @Override
    public Optional<BillRecord> findRecordById(int billId) {
        String sql = JOIN_SQL + " WHERE b.bill_id = ?";
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, billId);
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
    public Optional<Bill> findByAppointmentId(int appointmentId) {
        String sql = "SELECT * FROM bills WHERE appointment_id = ?";
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, appointmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapBill(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw wrap();
        }
    }

    @Override
    public List<BillRecord> findAll() {
        String sql = JOIN_SQL + " ORDER BY b.issued_at DESC";
        List<BillRecord> records = new ArrayList<>();
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
        String sql = "SELECT COUNT(*) FROM bills WHERE DATE(issued_at) = ?";
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
    public int insert(Bill bill) {
        String sql = """
                INSERT INTO bills (bill_number, appointment_id, treatment_type, treatment_cost, consultation_fee, total_amount, issued_by)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, bill.getBillNumber());
            statement.setInt(2, bill.getAppointmentId());
            statement.setString(3, bill.getTreatmentType());
            statement.setBigDecimal(4, bill.getTreatmentCost());
            statement.setBigDecimal(5, bill.getConsultationFee());
            statement.setBigDecimal(6, bill.getTotalAmount());
            statement.setInt(7, bill.getIssuedBy());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new ClinicException("Unable to create the bill.", 500);
        } catch (SQLException exception) {
            throw wrap();
        }
    }

    private Bill mapBill(ResultSet resultSet) throws SQLException {
        Bill bill = new Bill();
        bill.setBillId(resultSet.getInt("bill_id"));
        bill.setBillNumber(resultSet.getString("bill_number"));
        bill.setAppointmentId(resultSet.getInt("appointment_id"));
        bill.setTreatmentType(resultSet.getString("treatment_type"));
        bill.setTreatmentCost(resultSet.getBigDecimal("treatment_cost"));
        bill.setConsultationFee(resultSet.getBigDecimal("consultation_fee"));
        bill.setTotalAmount(resultSet.getBigDecimal("total_amount"));
        bill.setIssuedBy(resultSet.getInt("issued_by"));
        Timestamp issued = resultSet.getTimestamp("issued_at");
        if (issued != null) {
            bill.setIssuedAt(issued.toLocalDateTime());
        }
        return bill;
    }

    private BillRecord mapRecord(ResultSet resultSet) throws SQLException {
        Bill bill = mapBill(resultSet);
        Appointment appointment = new Appointment();
        appointment.setAppointmentId(bill.getAppointmentId());
        appointment.setAppointmentNumber(resultSet.getString("appointment_number"));
        appointment.setPatientId(resultSet.getInt("patient_id"));
        appointment.setDentistName(resultSet.getString("dentist_name"));
        appointment.setTreatmentType(resultSet.getString("treatment_type"));
        Date date = resultSet.getDate("appointment_date");
        Time time = resultSet.getTime("appointment_time");
        appointment.setAppointmentDate(date == null ? null : date.toLocalDate());
        appointment.setAppointmentTime(time == null ? null : time.toLocalTime());
        appointment.setStatus(AppointmentStatus.from(resultSet.getString("status")));
        Patient patient = new Patient();
        patient.setPatientId(appointment.getPatientId());
        patient.setPatientCode(resultSet.getString("patient_code"));
        patient.setFullName(resultSet.getString("patient_name"));
        patient.setAge(resultSet.getInt("age"));
        patient.setAddress(resultSet.getString("address"));
        patient.setContactNumber(resultSet.getString("contact_number"));
        return new BillRecord(bill, appointment, patient);
    }

    private ClinicException wrap() {
        return new ClinicException("Unable to complete the billing request. Please try again.", 500);
    }
}
