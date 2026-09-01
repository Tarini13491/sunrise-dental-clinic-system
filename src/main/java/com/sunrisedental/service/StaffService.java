package com.sunrisedental.service;

import com.sunrisedental.dao.DaoException;
import com.sunrisedental.dao.UserDao;
import com.sunrisedental.model.User;
import com.sunrisedental.pattern.factory.DaoFactory;
import com.sunrisedental.util.PasswordUtil;
import com.sunrisedental.util.ValidationUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class StaffService {

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._-]{3,50}$");

    private final UserDao users = DaoFactory.get().users();

    public Map<String, Object> list() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (User user : users.listStaff()) {
            rows.add(publicStaff(user));
        }
        return Map.of("success", true, "message", "Staff accounts loaded.", "staff", rows);
    }

    public Map<String, Object> create(String username, String password, String fullName, String email, String phone) {
        String error = validate(username, password, fullName, email, true);
        if (error != null) {
            return fail(error);
        }
        String cleanUser = username.trim();
        if (users.usernameTaken(cleanUser, null)) {
            return fail("That username is already in use.");
        }
        User user = new User();
        user.setUsername(cleanUser);
        String salt = PasswordUtil.newSalt();
        user.setSalt(salt);
        user.setPasswordHash(PasswordUtil.hash(password, salt));
        user.setFullName(fullName.trim());
        user.setRole("STAFF");
        user.setEmail(blankToDefault(email, cleanUser + "@sunrisedental.lk"));
        user.setPhone(blankToNull(phone));
        users.insert(user);
        User saved = users.findByUsernameAny(cleanUser);
        return Map.of("success", true, "message", "Staff account created. They can now sign in to the staff portal.",
                "staff", publicStaff(saved));
    }

    public Map<String, Object> update(int userId, String username, String password, String fullName,
                                      String email, String phone) {
        User existing = users.findById(userId);
        if (existing == null || !"STAFF".equals(existing.getRole())) {
            return fail("Staff account was not found.");
        }
        boolean passwordRequired = password != null && !password.isBlank();
        String error = validate(username, password, fullName, email, passwordRequired);
        if (error != null) {
            return fail(error);
        }
        String cleanUser = username.trim();
        if (users.usernameTaken(cleanUser, userId)) {
            return fail("That username is already in use.");
        }
        existing.setUsername(cleanUser);
        existing.setFullName(fullName.trim());
        existing.setEmail(blankToDefault(email, cleanUser + "@sunrisedental.lk"));
        existing.setPhone(blankToNull(phone));
        users.updateStaff(existing);
        if (passwordRequired) {
            String salt = PasswordUtil.newSalt();
            users.updatePassword(userId, PasswordUtil.hash(password, salt), salt);
            users.closeSessionsForUser(userId);
        }
        return Map.of("success", true, "message", "Staff details saved.",
                "staff", publicStaff(users.findById(userId)));
    }

    public Map<String, Object> setBlocked(int userId, boolean blocked, User actor) {
        User existing = users.findById(userId);
        if (existing == null || !"STAFF".equals(existing.getRole())) {
            return fail("Staff account was not found.");
        }
        if (actor != null && actor.getUserId() == userId) {
            return fail("You cannot block your own account.");
        }
        users.setActive(userId, !blocked);
        if (blocked) {
            users.closeSessionsForUser(userId);
        }
        String message = blocked
                ? "Staff account blocked. They can no longer sign in."
                : "Staff account is active again.";
        return Map.of("success", true, "message", message, "staff", publicStaff(users.findById(userId)));
    }

    public Map<String, Object> remove(int userId, User actor) {
        User existing = users.findById(userId);
        if (existing == null || !"STAFF".equals(existing.getRole())) {
            return fail("Staff account was not found.");
        }
        if (actor != null && actor.getUserId() == userId) {
            return fail("You cannot remove your own account.");
        }
        users.closeSessionsForUser(userId);
        try {
            boolean deleted = users.deleteStaff(userId);
            if (!deleted) {
                return fail("Staff account was not found.");
            }
            return Map.of("success", true, "message", "Staff account removed.");
        } catch (DaoException e) {
            users.setActive(userId, false);
            return Map.of("success", true,
                    "message", "This account is linked to clinic records, so it was blocked instead of deleted.",
                    "staff", publicStaff(users.findById(userId)));
        }
    }

    private String validate(String username, String password, String fullName, String email, boolean passwordRequired) {
        String required = ValidationUtil.firstError(
                ValidationUtil.require(username, "Username"),
                ValidationUtil.require(fullName, "Full name"),
                passwordRequired ? ValidationUtil.require(password, "Password") : null,
                ValidationUtil.emailOptional(email)
        );
        if (required != null) {
            return required;
        }
        if (!USERNAME.matcher(username.trim()).matches()) {
            return "Username must be 3–50 letters, numbers, dots, hyphens or underscores.";
        }
        if (passwordRequired && password.trim().length() < 6) {
            return "Password must be at least 6 characters.";
        }
        return null;
    }

    private Map<String, Object> publicStaff(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId", user.getUserId());
        map.put("username", user.getUsername());
        map.put("fullName", user.getFullName());
        map.put("role", user.getRole());
        map.put("email", user.getEmail() == null ? "" : user.getEmail());
        map.put("phone", user.getPhone() == null ? "" : user.getPhone());
        map.put("active", user.isActive());
        return map;
    }

    private Map<String, Object> fail(String message) {
        return Map.of("success", false, "message", message);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}