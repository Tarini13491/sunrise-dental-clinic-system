package com.sunrise.clinic.dao.jdbc;

import com.sunrise.clinic.config.DatabaseManager;
import com.sunrise.clinic.exception.ClinicException;
import com.sunrise.clinic.model.AccountStatus;
import com.sunrise.clinic.model.Role;
import com.sunrise.clinic.model.UserAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcUserDao implements com.sunrise.clinic.dao.UserDao {
    @Override
    public Optional<UserAccount> findById(int userId) {
        return queryOne("SELECT * FROM users WHERE user_id = ?", statement -> statement.setInt(1, userId));
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return queryOne("SELECT * FROM users WHERE username = ?", statement -> statement.setString(1, username));
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return queryOne("SELECT * FROM users WHERE email = ?", statement -> statement.setString(1, email));
    }

    @Override
    public Optional<UserAccount> findByContact(String contactNumber) {
        return queryOne("SELECT * FROM users WHERE contact_number = ?", statement -> statement.setString(1, contactNumber));
    }

    @Override
    public List<UserAccount> findStaffMembers() {
        return queryList("SELECT * FROM users WHERE role = ? ORDER BY full_name", statement -> statement.setString(1, Role.STAFF.name()));
    }

    @Override
    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }

    @Override
    public boolean existsByEmail(String email, Integer excludeUserId) {
        return existsExcluding("SELECT COUNT(*) FROM users WHERE email = ?", email, excludeUserId);
    }

    @Override
    public boolean existsByContact(String contactNumber, Integer excludeUserId) {
        return existsExcluding("SELECT COUNT(*) FROM users WHERE contact_number = ?", contactNumber, excludeUserId);
    }

    @Override
    public int insert(UserAccount user) {
        String sql = "INSERT INTO users (username, password_hash, full_name, email, contact_number, role, account_status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPasswordHash());
            statement.setString(3, user.getFullName());
            statement.setString(4, user.getEmail());
            statement.setString(5, user.getContactNumber());
            statement.setString(6, user.getRole().name());
            statement.setString(7, user.getStatus().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new ClinicException("Unable to create the user account.", 500);
        } catch (SQLException exception) {
            throw wrap(exception);
        }
    }

    @Override
    public void update(UserAccount user) {
        String sql = "UPDATE users SET full_name = ?, email = ?, contact_number = ?, password_hash = ? WHERE user_id = ?";
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getFullName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getContactNumber());
            statement.setString(4, user.getPasswordHash());
            statement.setInt(5, user.getUserId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw wrap(exception);
        }
    }

    @Override
    public void updateStatus(int userId, AccountStatus status) {
        String sql = "UPDATE users SET account_status = ? WHERE user_id = ?";
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setInt(2, userId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw wrap(exception);
        }
    }

    @Override
    public boolean hasRole(Role role) {
        String sql = "SELECT COUNT(*) FROM users WHERE role = ?";
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, role.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        } catch (SQLException exception) {
            throw wrap(exception);
        }
    }

    private boolean existsExcluding(String sql, String value, Integer excludeUserId) {
        String query = excludeUserId == null ? sql : sql + " AND user_id <> ?";
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, value);
            if (excludeUserId != null) {
                statement.setInt(2, excludeUserId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        } catch (SQLException exception) {
            throw wrap(exception);
        }
    }

    private Optional<UserAccount> queryOne(String sql, SqlConsumer binder) {
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
            throw wrap(exception);
        }
    }

    private List<UserAccount> queryList(String sql, SqlConsumer binder) {
        List<UserAccount> users = new ArrayList<>();
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.accept(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    users.add(map(resultSet));
                }
            }
            return users;
        } catch (SQLException exception) {
            throw wrap(exception);
        }
    }

    private UserAccount map(ResultSet resultSet) throws SQLException {
        UserAccount user = new UserAccount();
        user.setUserId(resultSet.getInt("user_id"));
        user.setUsername(resultSet.getString("username"));
        user.setPasswordHash(resultSet.getString("password_hash"));
        user.setFullName(resultSet.getString("full_name"));
        user.setEmail(resultSet.getString("email"));
        user.setContactNumber(resultSet.getString("contact_number"));
        user.setRole(Role.from(resultSet.getString("role")));
        user.setStatus(AccountStatus.from(resultSet.getString("account_status")));
        Timestamp created = resultSet.getTimestamp("created_at");
        if (created != null) {
            user.setCreatedAt(created.toLocalDateTime());
        }
        return user;
    }

    private ClinicException wrap(SQLException exception) {
        return new ClinicException("Unable to complete the account request. Please try again.", 500);
    }

    @FunctionalInterface
    private interface SqlConsumer {
        void accept(PreparedStatement statement) throws SQLException;
    }
}
