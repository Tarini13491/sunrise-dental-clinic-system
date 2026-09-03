package com.sunrise.clinic.web;

import com.google.gson.JsonObject;
import com.sunrise.clinic.config.AppSettings;
import com.sunrise.clinic.model.SessionUser;
import com.sunrise.clinic.service.AuthService;
import com.sunrise.clinic.service.ClinicServices;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet(urlPatterns = {"/api/auth", "/api/auth/*"})
public class AuthServlet extends ApiServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            if (!"session".equals(lastSegment(request))) {
                writeError(response, HttpServletResponse.SC_NOT_FOUND, "The requested service was not found.");
                return;
            }
            SessionUser user = currentUser(request);
            Map<String, Object> data = new LinkedHashMap<>();
            if (user == null) {
                data.put("authenticated", false);
                writeOk(response, data);
                return;
            }
            data.put("authenticated", true);
            data.put("user", sessionPayload(user));
            writeOk(response, data);
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            String action = lastSegment(request);
            AuthService auth = ClinicServices.INSTANCE.auth();
            if ("login".equals(action)) {
                JsonObject body = readBody(request);
                SessionUser user = auth.login(text(body, "username"), text(body, "password"));
                String previousToken = AuthCookies.read(request);
                if (previousToken != null) {
                    auth.revokePersistentLogin(previousToken);
                }
                HttpSession session = request.getSession(true);
                session.setMaxInactiveInterval(AppSettings.INSTANCE.sessionTimeoutMinutes() * 60);
                session.setAttribute(SESSION_KEY, user);
                if (flag(body, "rememberMe")) {
                    AuthCookies.store(request, response, auth.createPersistentToken(user.getUserId()));
                } else {
                    AuthCookies.clear(request, response);
                }
                writeOk(response, sessionPayload(user));
                return;
            }
            if ("logout".equals(action)) {
                String persistentToken = AuthCookies.read(request);
                if (persistentToken != null) {
                    auth.revokePersistentLogin(persistentToken);
                }
                AuthCookies.clear(request, response);
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("loggedOut", true);
                writeOk(response, data);
                return;
            }
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "The requested service was not found.");
        });
    }

    private Map<String, Object> sessionPayload(SessionUser user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getUserId());
        data.put("username", user.getUsername());
        data.put("fullName", user.getFullName());
        data.put("role", user.getRole().name());
        return data;
    }

    private String lastSegment(HttpServletRequest request) {
        String[] parts = segments(request);
        return parts.length == 0 ? "" : parts[parts.length - 1];
    }
}
