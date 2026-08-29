package com.sunrisedental.dao;

import com.sunrisedental.model.Bill;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.Map;

public class BillDao extends BaseDao {

    public Map<String, Object> calculateViaProcedure(int appointmentId, BigDecimal discount, BigDecimal surcharge) {
        return withConnection(c -> {
            try (CallableStatement cs = c.prepareCall("{CALL sp_calculate_bill(?,?,?,?,?,?)}")) {
                cs.setInt(1, appointmentId);
                cs.setBigDecimal(2, discount == null ? BigDecimal.ZERO : discount);
                cs.setBigDecimal(3, surcharge == null ? BigDecimal.ZERO : surcharge);
                cs.registerOutParameter(4, Types.VARCHAR);
                cs.registerOutParameter(5, Types.DECIMAL);
                cs.registerOutParameter(6, Types.VARCHAR);
                cs.execute();
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("billNumber", cs.getString(4));
                result.put("total", cs.getBigDecimal(5));
                result.put("message", cs.getString(6));
                return result;
            }
        });
    }

    public Bill findByNumber(String billNumber) {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(fullSelect() + " WHERE b.bill_number = ?")) {
                ps.setString(1, billNumber);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? map(rs) : null;
                }
            }
        });
    }

    public Bill findByAppointmentNumber(String appointmentNumber) {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(fullSelect() + " WHERE a.appointment_number = ?")) {
                ps.setString(1, appointmentNumber);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? map(rs) : null;
                }
            }
        });
    }

    public String markPaid(String billNumber, String method, BigDecimal amount) {
        return withConnection(c -> {
            try (CallableStatement cs = c.prepareCall("{CALL sp_mark_bill_paid(?,?,?,?)}")) {
                cs.setString(1, billNumber);
                cs.setString(2, method);
                cs.setBigDecimal(3, amount);
                cs.registerOutParameter(4, Types.VARCHAR);
                cs.execute();
                return cs.getString(4);
            }
        });
    }

    private String fullSelect() {
        return """
                SELECT b.*, a.appointment_number, p.full_name AS patient_name, p.address, p.contact_number,
                       d.full_name AS dentist_name, t.treatment_name
                FROM bills b
                JOIN appointments a ON a.appointment_id = b.appointment_id
                JOIN patients p ON p.patient_id = a.patient_id
                JOIN dentists d ON d.dentist_id = a.dentist_id
                JOIN treatments t ON t.treatment_id = a.treatment_id
                """;
    }

    private Bill map(ResultSet rs) throws SQLException {
        Bill b = new Bill();
        b.setBillId(rs.getInt("bill_id"));
        b.setBillNumber(rs.getString("bill_number"));
        b.setAppointmentId(rs.getInt("appointment_id"));
        b.setAppointmentNumber(rs.getString("appointment_number"));
        b.setPatientName(rs.getString("patient_name"));
        b.setAddress(rs.getString("address"));
        b.setContactNumber(rs.getString("contact_number"));
        b.setDentistName(rs.getString("dentist_name"));
        b.setTreatmentName(rs.getString("treatment_name"));
        b.setConsultationFee(rs.getBigDecimal("consultation_fee"));
        b.setTreatmentCost(rs.getBigDecimal("treatment_cost"));
        b.setSurcharge(rs.getBigDecimal("surcharge"));
        b.setDiscount(rs.getBigDecimal("discount"));
        b.setTax(rs.getBigDecimal("tax"));
        b.setTotalAmount(rs.getBigDecimal("total_amount"));
        b.setPaymentStatus(rs.getString("payment_status"));
        b.setPaymentMethod(rs.getString("payment_method"));
        b.setAmountPaid(rs.getBigDecimal("amount_paid"));
        b.setCreatedAt(rs.getTimestamp("created_at"));
        return b;
    }
}
