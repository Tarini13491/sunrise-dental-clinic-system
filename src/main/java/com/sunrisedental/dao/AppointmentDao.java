package com.sunrisedental.dao;

import com.sunrisedental.model.Appointment;
import com.sunrisedental.model.Dentist;
import com.sunrisedental.model.Treatment;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AppointmentDao extends BaseDao {

    public Map<String, Object> registerViaProcedure(String patientName, String address, String contact,
                                                    String email, int dentistId, int treatmentId,
                                                    Date date, Time time, String notes, Integer createdBy) {
        return withConnection(c -> {
            try (CallableStatement cs = c.prepareCall("{CALL sp_register_appointment(?,?,?,?,?,?,?,?,?,?,?,?,?)}")) {
                cs.setString(1, patientName);
                cs.setString(2, address);
                cs.setString(3, contact);
                cs.setString(4, email);
                cs.setInt(5, dentistId);
                cs.setInt(6, treatmentId);
                cs.setDate(7, date);
                cs.setTime(8, time);
                cs.setString(9, notes);
                if (createdBy == null) {
                    cs.setNull(10, Types.INTEGER);
                } else {
                    cs.setInt(10, createdBy);
                }
                cs.registerOutParameter(11, Types.VARCHAR);
                cs.registerOutParameter(12, Types.INTEGER);
                cs.registerOutParameter(13, Types.VARCHAR);
                cs.execute();
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("appointmentNumber", cs.getString(11));
                result.put("appointmentId", cs.getObject(12));
                result.put("message", cs.getString(13));
                return result;
            }
        });
    }

    public Appointment findByNumber(String appointmentNumber) {
        return withConnection(c -> {
            try (CallableStatement cs = c.prepareCall("{CALL sp_search_appointment(?)}")) {
                cs.setString(1, appointmentNumber);
                try (ResultSet rs = cs.executeQuery()) {
                    return rs.next() ? mapFull(rs) : null;
                }
            }
        });
    }

    public List<Appointment> listUpcoming(int limit) {
        return withConnection(c -> {
            String sql = """
                    SELECT a.appointment_id, a.appointment_number, a.appointment_date, a.appointment_time,
                           a.status, p.full_name AS patient_name, p.contact_number,
                           d.full_name AS dentist_name, t.treatment_name
                    FROM appointments a
                    JOIN patients p ON p.patient_id = a.patient_id
                    JOIN dentists d ON d.dentist_id = a.dentist_id
                    JOIN treatments t ON t.treatment_id = a.treatment_id
                    WHERE a.appointment_date >= CURDATE() AND a.status IN ('SCHEDULED','CHECKED_IN')
                    ORDER BY a.appointment_date, a.appointment_time
                    LIMIT ?
                    """;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Appointment> list = new ArrayList<>();
                    while (rs.next()) {
                        Appointment a = new Appointment();
                        a.setAppointmentId(rs.getInt("appointment_id"));
                        a.setAppointmentNumber(rs.getString("appointment_number"));
                        a.setAppointmentDate(rs.getDate("appointment_date"));
                        a.setAppointmentTime(rs.getTime("appointment_time"));
                        a.setStatus(rs.getString("status"));
                        a.setPatientName(rs.getString("patient_name"));
                        a.setContactNumber(rs.getString("contact_number"));
                        a.setDentistName(rs.getString("dentist_name"));
                        a.setTreatmentName(rs.getString("treatment_name"));
                        list.add(a);
                    }
                    return list;
                }
            }
        });
    }

    public List<Appointment> listByDate(Date date) {
        return withConnection(c -> {
            String sql = """
                    SELECT a.appointment_id, a.appointment_number, a.appointment_date, a.appointment_time,
                           a.status, p.full_name AS patient_name, d.full_name AS dentist_name,
                           t.treatment_name, b.bill_number, b.total_amount, b.payment_status
                    FROM appointments a
                    JOIN patients p ON p.patient_id = a.patient_id
                    JOIN dentists d ON d.dentist_id = a.dentist_id
                    JOIN treatments t ON t.treatment_id = a.treatment_id
                    LEFT JOIN bills b ON b.appointment_id = a.appointment_id
                    WHERE a.appointment_date = ?
                    ORDER BY a.appointment_time
                    """;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setDate(1, date);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Appointment> list = new ArrayList<>();
                    while (rs.next()) {
                        Appointment a = new Appointment();
                        a.setAppointmentId(rs.getInt("appointment_id"));
                        a.setAppointmentNumber(rs.getString("appointment_number"));
                        a.setAppointmentDate(rs.getDate("appointment_date"));
                        a.setAppointmentTime(rs.getTime("appointment_time"));
                        a.setStatus(rs.getString("status"));
                        a.setPatientName(rs.getString("patient_name"));
                        a.setDentistName(rs.getString("dentist_name"));
                        a.setTreatmentName(rs.getString("treatment_name"));
                        a.setBillNumber(rs.getString("bill_number"));
                        a.setBillTotal(rs.getBigDecimal("total_amount"));
                        a.setPaymentStatus(rs.getString("payment_status"));
                        list.add(a);
                    }
                    return list;
                }
            }
        });
    }

    public boolean updateStatus(String appointmentNumber, String status) {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE appointments SET status = ? WHERE appointment_number = ?")) {
                ps.setString(1, status);
                ps.setString(2, appointmentNumber);
                return ps.executeUpdate() > 0;
            }
        });
    }

    public List<Dentist> listDentists() {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM dentists WHERE available = 1 ORDER BY full_name");
                 ResultSet rs = ps.executeQuery()) {
                List<Dentist> list = new ArrayList<>();
                while (rs.next()) {
                    Dentist d = new Dentist();
                    d.setDentistId(rs.getInt("dentist_id"));
                    d.setUserId(getNullableInt(rs, "user_id"));
                    d.setFullName(rs.getString("full_name"));
                    d.setSpecialization(rs.getString("specialization"));
                    d.setConsultationFee(rs.getBigDecimal("consultation_fee"));
                    d.setPhone(rs.getString("phone"));
                    d.setEmail(rs.getString("email"));
                    d.setAvailable(rs.getInt("available") == 1);
                    list.add(d);
                }
                return list;
            }
        });
    }

    public List<Treatment> listTreatments() {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM treatments ORDER BY treatment_name");
                 ResultSet rs = ps.executeQuery()) {
                List<Treatment> list = new ArrayList<>();
                while (rs.next()) {
                    Treatment t = new Treatment();
                    t.setTreatmentId(rs.getInt("treatment_id"));
                    t.setTreatmentCode(rs.getString("treatment_code"));
                    t.setTreatmentName(rs.getString("treatment_name"));
                    t.setDescription(rs.getString("description"));
                    t.setCategory(rs.getString("category"));
                    t.setBaseCost(rs.getBigDecimal("base_cost"));
                    t.setDurationMinutes(rs.getInt("duration_minutes"));
                    list.add(t);
                }
                return list;
            }
        });
    }

    public Treatment findTreatment(int treatmentId) {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM treatments WHERE treatment_id = ?")) {
                ps.setInt(1, treatmentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    Treatment t = new Treatment();
                    t.setTreatmentId(rs.getInt("treatment_id"));
                    t.setTreatmentCode(rs.getString("treatment_code"));
                    t.setTreatmentName(rs.getString("treatment_name"));
                    t.setCategory(rs.getString("category"));
                    t.setBaseCost(rs.getBigDecimal("base_cost"));
                    return t;
                }
            }
        });
    }

    public Dentist findDentist(int dentistId) {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM dentists WHERE dentist_id = ?")) {
                ps.setInt(1, dentistId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    Dentist d = new Dentist();
                    d.setDentistId(rs.getInt("dentist_id"));
                    d.setFullName(rs.getString("full_name"));
                    d.setSpecialization(rs.getString("specialization"));
                    d.setConsultationFee(rs.getBigDecimal("consultation_fee"));
                    d.setEmail(rs.getString("email"));
                    d.setPhone(rs.getString("phone"));
                    return d;
                }
            }
        });
    }

    public List<Appointment> searchFlexible(String query) {
        return withConnection(c -> {
            String sql = """
                    SELECT a.appointment_id, a.appointment_number, a.appointment_date, a.appointment_time,
                           a.status, a.notes, p.full_name AS patient_name, p.address, p.contact_number,
                           p.email AS patient_email, d.full_name AS dentist_name, d.specialization,
                           t.treatment_name, b.bill_number, b.total_amount, b.payment_status
                    FROM appointments a
                    JOIN patients p ON p.patient_id = a.patient_id
                    JOIN dentists d ON d.dentist_id = a.dentist_id
                    JOIN treatments t ON t.treatment_id = a.treatment_id
                    LEFT JOIN bills b ON b.appointment_id = a.appointment_id
                    WHERE a.appointment_number = ?
                       OR p.full_name LIKE ?
                       OR p.contact_number LIKE ?
                    ORDER BY a.appointment_date DESC, a.appointment_time DESC
                    LIMIT 25
                    """;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                String like = "%" + query + "%";
                ps.setString(1, query.toUpperCase());
                ps.setString(2, like);
                ps.setString(3, like);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Appointment> list = new ArrayList<>();
                    while (rs.next()) {
                        Appointment a = new Appointment();
                        a.setAppointmentId(rs.getInt("appointment_id"));
                        a.setAppointmentNumber(rs.getString("appointment_number"));
                        a.setAppointmentDate(rs.getDate("appointment_date"));
                        a.setAppointmentTime(rs.getTime("appointment_time"));
                        a.setStatus(rs.getString("status"));
                        a.setNotes(rs.getString("notes"));
                        a.setPatientName(rs.getString("patient_name"));
                        a.setAddress(rs.getString("address"));
                        a.setContactNumber(rs.getString("contact_number"));
                        a.setPatientEmail(rs.getString("patient_email"));
                        a.setDentistName(rs.getString("dentist_name"));
                        a.setSpecialization(rs.getString("specialization"));
                        a.setTreatmentName(rs.getString("treatment_name"));
                        a.setBillNumber(rs.getString("bill_number"));
                        a.setBillTotal(rs.getBigDecimal("total_amount"));
                        a.setPaymentStatus(rs.getString("payment_status"));
                        list.add(a);
                    }
                    return list;
                }
            }
        });
    }

    public List<String> occupiedSlots(int dentistId, Date date) {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT appointment_time FROM appointments WHERE dentist_id = ? AND appointment_date = ? "
                            + "AND status NOT IN ('CANCELLED','NO_SHOW')")) {
                ps.setInt(1, dentistId);
                ps.setDate(2, date);
                try (ResultSet rs = ps.executeQuery()) {
                    List<String> slots = new ArrayList<>();
                    while (rs.next()) {
                        Time t = rs.getTime(1);
                        slots.add(t.toLocalTime().toString().substring(0, 5));
                    }
                    return slots;
                }
            }
        });
    }

    private Appointment mapFull(ResultSet rs) {
        try {
            Appointment a = new Appointment();
            a.setAppointmentId(rs.getInt("appointment_id"));
            a.setAppointmentNumber(rs.getString("appointment_number"));
            a.setAppointmentDate(rs.getDate("appointment_date"));
            a.setAppointmentTime(rs.getTime("appointment_time"));
            a.setStatus(rs.getString("status"));
            a.setNotes(rs.getString("notes"));
            a.setCreatedAt(rs.getTimestamp("created_at"));
            a.setPatientId(rs.getInt("patient_id"));
            a.setPatientName(rs.getString("patient_name"));
            a.setAddress(rs.getString("address"));
            a.setContactNumber(rs.getString("contact_number"));
            a.setPatientEmail(rs.getString("patient_email"));
            a.setDentistId(rs.getInt("dentist_id"));
            a.setDentistName(rs.getString("dentist_name"));
            a.setSpecialization(rs.getString("specialization"));
            a.setConsultationFee(rs.getBigDecimal("consultation_fee"));
            a.setTreatmentId(rs.getInt("treatment_id"));
            a.setTreatmentName(rs.getString("treatment_name"));
            a.setTreatmentCode(rs.getString("treatment_code"));
            a.setTreatmentCategory(rs.getString("treatment_category"));
            a.setTreatmentCost(rs.getBigDecimal("treatment_cost"));
            a.setBillId(getNullableInt(rs, "bill_id"));
            a.setBillNumber(rs.getString("bill_number"));
            a.setBillTotal(rs.getBigDecimal("total_amount"));
            a.setPaymentStatus(rs.getString("payment_status"));
            return a;
        } catch (SQLException e) {
            throw new DaoException("Could not read appointment row.", e);
        }
    }

    public int countPatients() {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM patients");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        });
    }

    public BigDecimal todayRevenue() {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COALESCE(SUM(total_amount),0) FROM bills WHERE DATE(created_at) = CURDATE()");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        });
    }
}
