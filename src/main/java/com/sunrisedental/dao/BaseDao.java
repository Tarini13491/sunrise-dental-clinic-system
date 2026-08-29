package com.sunrisedental.dao;

import com.sunrisedental.pattern.singleton.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * Shared JDBC helpers for every DAO. This keeps SQL in the data tier only.
 */
public abstract class BaseDao {

    protected Connection borrow() {
        try {
            return DatabaseConnection.getInstance().getConnection();
        } catch (SQLException e) {
            throw new DaoException("Could not borrow a database connection.", e);
        }
    }

    protected void release(Connection connection) {
        DatabaseConnection.getInstance().release(connection);
    }

    protected <T> T withConnection(SqlFunction<T> work) {
        Connection connection = borrow();
        try {
            return work.apply(connection);
        } catch (SQLException e) {
            throw new DaoException(friendly(e), e);
        } finally {
            release(connection);
        }
    }

    protected String friendly(SQLException e) {
        String state = e.getSQLState();
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if ("45000".equals(state) || msg.contains("Double booking") || msg.contains("already booked")) {
            return "This dentist already has a patient at the selected date and time.";
        }
        if (msg.contains("uq_active_dentist_slot") || msg.contains("Duplicate entry")) {
            return "This dentist already has a patient at the selected date and time.";
        }
        if (msg.contains("outside clinic hours") || msg.contains("in the past")) {
            return msg.contains("past")
                    ? "Appointment date cannot be in the past."
                    : "Clinic hours are 08:00 to 17:30.";
        }
        return "A database error occurred. Please try again.";
    }

    @FunctionalInterface
    protected interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }

    protected static Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    protected static String nvl(ResultSet rs, String column) throws SQLException {
        return rs.getString(column);
    }

    protected <T> T mapOne(ResultSet rs, Function<ResultSet, T> mapper) throws SQLException {
        return rs.next() ? mapper.apply(rs) : null;
    }
}
