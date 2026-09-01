package com.sunrisedental.dao;

import com.sunrisedental.model.User;
import com.sunrisedental.util.PasswordUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class UserDao extends BaseDao {

    public User findByUsername(String username) {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM users WHERE username = ? AND active = 1")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? map(rs) : null;
                }
            }
        });
    }

    public User findByUsernameAny(String username) {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM users WHERE username = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? map(rs) : null;
                }
            }
        });
    }

    public User findById(int userId) {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM users WHERE user_id = ?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? map(rs) : null;
                }
            }
        });
    }

    public int countUsers() {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM users");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        });
    }

    public void insert(User user) {
        withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO users (username, password_hash, salt, full_name, role, email, phone, active) "
                            + "VALUES (?,?,?,?,?,?,?,1)")) {
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getPasswordHash());
                ps.setString(3, user.getSalt());
                ps.setString(4, user.getFullName());
                ps.setString(5, user.getRole());
                ps.setString(6, user.getEmail());
                ps.setString(7, user.getPhone());
                ps.executeUpdate();
                return null;
            }
        });
    }

    public void saveRememberToken(int userId, String tokenHash, Timestamp expiresAt) {
        withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO remember_tokens (user_id, token_hash, expires_at) VALUES (?,?,?)")) {
                ps.setInt(1, userId);
                ps.setString(2, tokenHash);
                ps.setTimestamp(3, expiresAt);
                ps.executeUpdate();
                return null;
            }
        });
    }

    public User findByRememberToken(String tokenHash) {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT u.* FROM users u JOIN remember_tokens t ON t.user_id = u.user_id "
                            + "WHERE t.token_hash = ? AND t.expires_at > NOW() AND u.active = 1")) {
                ps.setString(1, tokenHash);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? map(rs) : null;
                }
            }
        });
    }

    public void deleteRememberToken(String tokenHash) {
        withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM remember_tokens WHERE token_hash = ?")) {
                ps.setString(1, tokenHash);
                ps.executeUpdate();
                return null;
            }
        });
    }

    public void recordSession(String sessionId, int userId, String ip, String agent) {
        withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO user_sessions (session_id, user_id, ip_address, user_agent) VALUES (?,?,?,?) "
                            + "ON DUPLICATE KEY UPDATE last_seen = CURRENT_TIMESTAMP, active = 1, user_id = VALUES(user_id)")) {
                ps.setString(1, sessionId);
                ps.setInt(2, userId);
                ps.setString(3, ip);
                ps.setString(4, truncate(agent, 250));
                ps.executeUpdate();
                return null;
            }
        });
    }

    public void touchSession(String sessionId) {
        withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE user_sessions SET last_seen = CURRENT_TIMESTAMP WHERE session_id = ?")) {
                ps.setString(1, sessionId);
                ps.executeUpdate();
                return null;
            }
        });
    }

    public void closeSession(String sessionId) {
        withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE user_sessions SET active = 0 WHERE session_id = ?")) {
                ps.setString(1, sessionId);
                ps.executeUpdate();
                return null;
            }
        });
    }

    public List<User> listStaff() {
        return withConnection(c -> {
            List<User> list = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM users WHERE role = 'STAFF' ORDER BY full_name, username");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        });
    }

    public boolean usernameTaken(String username, Integer excludeUserId) {
        return withConnection(c -> {
            String sql = excludeUserId == null
                    ? "SELECT COUNT(*) FROM users WHERE username = ?"
                    : "SELECT COUNT(*) FROM users WHERE username = ? AND user_id <> ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, username);
                if (excludeUserId != null) {
                    ps.setInt(2, excludeUserId);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1) > 0;
                }
            }
        });
    }

    public void updateStaff(User user) {
        withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE users SET username = ?, full_name = ?, email = ?, phone = ? "
                            + "WHERE user_id = ? AND role = 'STAFF'")) {
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getFullName());
                ps.setString(3, user.getEmail());
                ps.setString(4, user.getPhone());
                ps.setInt(5, user.getUserId());
                ps.executeUpdate();
                return null;
            }
        });
    }

    public void updatePassword(int userId, String passwordHash, String salt) {
        withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE users SET password_hash = ?, salt = ? WHERE user_id = ? AND role = 'STAFF'")) {
                ps.setString(1, passwordHash);
                ps.setString(2, salt);
                ps.setInt(3, userId);
                ps.executeUpdate();
                return null;
            }
        });
    }

    public void setActive(int userId, boolean active) {
        withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE users SET active = ? WHERE user_id = ? AND role = 'STAFF'")) {
                ps.setInt(1, active ? 1 : 0);
                ps.setInt(2, userId);
                ps.executeUpdate();
                return null;
            }
        });
    }

    public void closeSessionsForUser(int userId) {
        withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM user_sessions WHERE user_id = ?")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM remember_tokens WHERE user_id = ?")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public boolean deleteStaff(int userId) {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM users WHERE user_id = ? AND role = 'STAFF'")) {
                ps.setInt(1, userId);
                return ps.executeUpdate() > 0;
            }
        });
    }

    public void ensureDefaultUsers() {
        if (countUsers() == 0) {
            seed("admin", "Admin@123", "Nimali Jayasuriya", "ADMIN",
                    "admin@sunrisedental.lk", "0112345678");
            seed("staff", "Staff@123", "Kasun Perera", "STAFF",
                    "desk@sunrisedental.lk", "0112345679");
            return;
        }
        if (findByUsernameAny("staff") == null) {
            seed("staff", "Staff@123", "Kasun Perera", "STAFF",
                    "desk@sunrisedental.lk", "0112345679");
        }
    }

    private void seed(String username, String password, String name, String role, String email, String phone) {
        String salt = PasswordUtil.newSalt();
        User user = new User();
        user.setUsername(username);
        user.setSalt(salt);
        user.setPasswordHash(PasswordUtil.hash(password, salt));
        user.setFullName(name);
        user.setRole(role);
        user.setEmail(email);
        user.setPhone(phone);
        insert(user);
    }

    private User map(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setSalt(rs.getString("salt"));
        user.setFullName(rs.getString("full_name"));
        user.setRole(rs.getString("role"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setActive(rs.getInt("active") == 1);
        user.setCreatedAt(rs.getTimestamp("created_at"));
        return user;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}