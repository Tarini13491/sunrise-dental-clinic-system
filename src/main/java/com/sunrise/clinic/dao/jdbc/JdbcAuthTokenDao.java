package com.sunrise.clinic.dao.jdbc;

import com.sunrise.clinic.config.DatabaseManager;
import com.sunrise.clinic.dao.AuthTokenDao;
import com.sunrise.clinic.exception.ClinicException;
import com.sunrise.clinic.model.AuthToken;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

public class JdbcAuthTokenDao implements AuthTokenDao {
    @Override
    public Optional<AuthToken> findByHash(String tokenHash) {
        String sql = "SELECT * FROM auth_tokens WHERE token_hash = ?";
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tokenHash);
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

    @Override
    public int insert(AuthToken token) {
        String sql = "INSERT INTO auth_tokens (user_id, token_hash, expires_at) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, token.getUserId());
            statement.setString(2, token.getTokenHash());
            statement.setTimestamp(3, Timestamp.valueOf(token.getExpiresAt()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new ClinicException("Unable to create the sign-in token.", 500);
        } catch (SQLException exception) {
            throw wrap();
        }
    }

    @Override
    public void deleteByHash(String tokenHash) {
        String sql = "DELETE FROM auth_tokens WHERE token_hash = ?";
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tokenHash);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw wrap();
        }
    }

    @Override
    public void deleteByUserId(int userId) {
        String sql = "DELETE FROM auth_tokens WHERE user_id = ?";
        try (Connection connection = DatabaseManager.INSTANCE.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw wrap();
        }
    }

    private AuthToken map(ResultSet resultSet) throws SQLException {
        AuthToken token = new AuthToken();
        token.setTokenId(resultSet.getInt("token_id"));
        token.setUserId(resultSet.getInt("user_id"));
        token.setTokenHash(resultSet.getString("token_hash"));
        Timestamp expires = resultSet.getTimestamp("expires_at");
        if (expires != null) {
            token.setExpiresAt(expires.toLocalDateTime());
        }
        Timestamp created = resultSet.getTimestamp("created_at");
        if (created != null) {
            token.setCreatedAt(created.toLocalDateTime());
        }
        return token;
    }

    private ClinicException wrap() {
        return new ClinicException("Unable to complete the sign-in request. Please try again.", 500);
    }
}
