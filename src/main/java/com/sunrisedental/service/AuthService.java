package com.sunrisedental.service;

import com.sunrisedental.config.AppConfig;
import com.sunrisedental.model.User;
import com.sunrisedental.pattern.factory.DaoFactory;
import com.sunrisedental.util.CookieUtil;
import com.sunrisedental.util.PasswordUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class AuthService {

    public static final String SESSION_USER = "currentUser";
    public static final String SESSION_ROLE = "currentRole";
    public static final String SESSION_NAME = "currentName";
    public static final String SESSION_LOGIN_AT = "loginAt";

    public Map<String, Object> login(HttpServletRequest request, HttpServletResponse response,
                                     String username, String password, boolean remember) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Map.of("success", false, "message", "Enter both username and password.");
        }
        User user = DaoFactory.get().users().findByUsername(username.trim());
        if (user == null || !PasswordUtil.matches(password, user.getSalt(), user.getPasswordHash())) {
            return Map.of("success", false, "message", "Those details are not recognised. Check with the clinic administrator.");
        }
        request.getSession().invalidate();
        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval(AppConfig.getInt("session.timeout.minutes", 30) * 60);
        session.setAttribute(SESSION_USER, user);
        session.setAttribute(SESSION_ROLE, user.getRole());
        session.setAttribute(SESSION_NAME, user.getFullName());
        session.setAttribute(SESSION_LOGIN_AT, Instant.now().toString());
        session.setAttribute("username", user.getUsername());

        DaoFactory.get().users().recordSession(session.getId(), user.getUserId(),
                request.getRemoteAddr(), request.getHeader("User-Agent"));

        CookieUtil.set(response, CookieUtil.ROLE_HINT_COOKIE, user.getRole(), 60 * 60 * 24 * 7, false);

        if (remember) {
            String token = PasswordUtil.randomToken();
            String hash = PasswordUtil.hash(token, "remember");
            Timestamp expires = Timestamp.from(Instant.now().plus(14, ChronoUnit.DAYS));
            DaoFactory.get().users().saveRememberToken(user.getUserId(), hash, expires);
            CookieUtil.set(response, CookieUtil.REMEMBER_COOKIE, token, 60 * 60 * 24 * 14, true);
        }

        return Map.of(
                "success", true,
                "message", "Welcome back, " + user.getFullName() + ".",
                "user", Map.of(
                        "fullName", user.getFullName(),
                        "role", user.getRole(),
                        "username", user.getUsername(),
                        "email", user.getEmail()
                )
        );
    }

    public User fromRememberCookie(HttpServletRequest request) {
        String token = CookieUtil.get(request, CookieUtil.REMEMBER_COOKIE);
        if (token == null || token.isBlank()) {
            return null;
        }
        String hash = PasswordUtil.hash(token, "remember");
        return DaoFactory.get().users().findByRememberToken(hash);
    }

    public void restoreSession(HttpServletRequest request, User user) {
        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval(AppConfig.getInt("session.timeout.minutes", 30) * 60);
        session.setAttribute(SESSION_USER, user);
        session.setAttribute(SESSION_ROLE, user.getRole());
        session.setAttribute(SESSION_NAME, user.getFullName());
        session.setAttribute(SESSION_LOGIN_AT, Instant.now().toString());
        session.setAttribute("username", user.getUsername());
        DaoFactory.get().users().recordSession(session.getId(), user.getUserId(),
                request.getRemoteAddr(), request.getHeader("User-Agent"));
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            DaoFactory.get().users().closeSession(session.getId());
            session.invalidate();
        }
        String token = CookieUtil.get(request, CookieUtil.REMEMBER_COOKIE);
        if (token != null) {
            DaoFactory.get().users().deleteRememberToken(PasswordUtil.hash(token, "remember"));
        }
        CookieUtil.clear(response, CookieUtil.REMEMBER_COOKIE);
        CookieUtil.clear(response, CookieUtil.ROLE_HINT_COOKIE);
    }

    public static User current(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_USER);
        return value instanceof User user ? user : null;
    }
}
