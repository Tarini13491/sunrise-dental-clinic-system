package com.sunrisedental.dao;

import com.sunrisedental.model.DashboardStats;
import com.sunrisedental.model.NotificationRecord;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportDao extends BaseDao {

    public DashboardStats dashboard() {
        DashboardStats stats = new DashboardStats();
        withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT
                      COUNT(*) AS total,
                      SUM(status = 'SCHEDULED') AS scheduled,
                      SUM(status = 'COMPLETED') AS completed,
                      SUM(status = 'CANCELLED') AS cancelled
                    FROM appointments WHERE appointment_date = CURDATE()
                    """);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setTodayAppointments(rs.getInt("total"));
                    stats.setScheduled(rs.getInt("scheduled"));
                    stats.setCompletedToday(rs.getInt("completed"));
                    stats.setCancelledToday(rs.getInt("cancelled"));
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COALESCE(SUM(total_amount),0) FROM bills WHERE DATE(created_at) = CURDATE()");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                stats.setTodayRevenue(rs.getBigDecimal(1));
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COALESCE(SUM(total_amount),0) FROM bills WHERE YEAR(created_at)=YEAR(CURDATE()) AND MONTH(created_at)=MONTH(CURDATE())");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                stats.setMonthRevenue(rs.getBigDecimal(1));
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM patients");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                stats.setRegisteredPatients(rs.getInt(1));
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM notifications WHERE DATE(created_at)=CURDATE()");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                stats.setUnreadNotifications(rs.getInt(1));
            }
            return null;
        });
        return stats;
    }

    public List<Map<String, Object>> dailySummary(Date date) {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM vw_daily_clinic_summary WHERE appointment_date = ?")) {
                ps.setDate(1, date);
                return maps(ps);
            }
        });
    }

    public List<Map<String, Object>> dentistPerformance() {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM vw_dentist_workload ORDER BY revenue DESC")) {
                return maps(ps);
            }
        });
    }

    public List<Map<String, Object>> treatmentPopularity() {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM vw_treatment_popularity ORDER BY times_booked DESC")) {
                return maps(ps);
            }
        });
    }

    public List<Map<String, Object>> monthlyRevenue() {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM vw_revenue_by_month ORDER BY month_key DESC LIMIT 12")) {
                return maps(ps);
            }
        });
    }

    public List<NotificationRecord> recentNotifications(int limit) {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM notifications ORDER BY created_at DESC LIMIT ?")) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    List<NotificationRecord> list = new ArrayList<>();
                    while (rs.next()) {
                        NotificationRecord n = new NotificationRecord();
                        n.setNotificationId(rs.getInt("notification_id"));
                        n.setAppointmentId(getNullableInt(rs, "appointment_id"));
                        n.setChannel(rs.getString("channel"));
                        n.setRecipient(rs.getString("recipient"));
                        n.setSubject(rs.getString("subject"));
                        n.setMessage(rs.getString("message"));
                        n.setStatus(rs.getString("status"));
                        n.setSentAt(rs.getTimestamp("sent_at"));
                        n.setCreatedAt(rs.getTimestamp("created_at"));
                        list.add(n);
                    }
                    return list;
                }
            }
        });
    }

    public void insertNotification(Integer appointmentId, String channel, String recipient,
                                   String subject, String message, String status) {
        withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO notifications (appointment_id, channel, recipient, subject, message, status, sent_at) "
                            + "VALUES (?,?,?,?,?,?,?)")) {
                if (appointmentId == null) {
                    ps.setNull(1, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(1, appointmentId);
                }
                ps.setString(2, channel);
                ps.setString(3, recipient);
                ps.setString(4, subject);
                ps.setString(5, message);
                ps.setString(6, status);
                ps.setTimestamp(7, "SENT".equals(status) ? new Timestamp(System.currentTimeMillis()) : null);
                ps.executeUpdate();
                return null;
            }
        });
    }

    public List<Map<String, Object>> auditTrail(int limit) {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM audit_log ORDER BY created_at DESC LIMIT ?")) {
                ps.setInt(1, limit);
                return maps(ps);
            }
        });
    }

    private List<Map<String, Object>> maps(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<Map<String, Object>> rows = new ArrayList<>();
            var meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }
            return rows;
        }
    }
}
