package com.sunrise.clinic.dao.jdbc;

import com.sunrise.clinic.config.DatabaseManager;
import com.sunrise.clinic.dao.DentistDao;
import com.sunrise.clinic.exception.ClinicException;
import com.sunrise.clinic.model.Dentist;
import com.sunrise.clinic.model.DentistStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcDentistDao implements DentistDao {
    @Override
    public Optional<Dentist> findById(int dentistId) {
        return queryOne("SELECT * FROM dentists WHERE dentist_id = ?", statement -> statement.setInt(1, dentistId));
    }

    @Override
    public Optional<Dentist> findByName(String fullName) {
        return queryOne("SELECT * FROM dentists WHERE full_name = ?", statement -> statement.setString(1, fullName));
    }

    @Override
    public List<Dentist> findAll() {
        return queryList("SELECT * FROM dentists ORDER BY full_name", statement -> {
        });
    }

    @Override
    public List<Dentist> findActive() {
        return queryList(
                "SELECT * FROM dentists WHERE dentist_status = ? ORDER BY full_name",
                statement -> statement.setString(1, DentistStatus.ACTIVE.name())
        );
    }

    @Override
    public int insert(Dentist dentist) {
        String sql = "INSERT INTO dentists (full_name, dentist_status) VALUES (?, ?)";
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, dentist.getFullName());
            statement.setString(2, dentist.getStatus().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new ClinicException("Unable to add the dentist.", 500);
        } catch (SQLException exception) {
            throw wrap();
        }
    }

    @Override
    public void updateStatus(int dentistId, DentistStatus status) {
        String sql = "UPDATE dentists SET dentist_status = ? WHERE dentist_id = ?";
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setInt(2, dentistId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw wrap();
        }
    }

    private Optional<Dentist> queryOne(String sql, SqlConsumer binder) {
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

    private List<Dentist> queryList(String sql, SqlConsumer binder) {
        List<Dentist> dentists = new ArrayList<>();
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.accept(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    dentists.add(map(resultSet));
                }
            }
            return dentists;
        } catch (SQLException exception) {
            throw wrap();
        }
    }

    private Dentist map(ResultSet resultSet) throws SQLException {
        Dentist dentist = new Dentist();
        dentist.setDentistId(resultSet.getInt("dentist_id"));
        dentist.setFullName(resultSet.getString("full_name"));
        dentist.setStatus(DentistStatus.from(resultSet.getString("dentist_status")));
        Timestamp created = resultSet.getTimestamp("created_at");
        if (created != null) {
            dentist.setCreatedAt(created.toLocalDateTime());
        }
        return dentist;
    }

    private ClinicException wrap() {
        return new ClinicException("Unable to complete the dentist request. Please try again.", 500);
    }

    @FunctionalInterface
    private interface SqlConsumer {
        void accept(PreparedStatement statement) throws SQLException;
    }
}
