package com.sunrisedental.controller;

import com.sunrisedental.model.User;
import com.sunrisedental.service.AuthService;
import com.sunrisedental.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;

public class SessionServlet extends HttpServlet {

    private final AuthService auth = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = AuthService.current(req);
        if (user == null) {
            user = auth.fromRememberCookie(req);
            if (user != null) {
                auth.restoreSession(req, user);
            }
        }
        if (user == null) {
            JsonUtil.write(resp, 200, false, "Not signed in.", Map.of("authenticated", false));
            return;
        }
        HttpSession session = req.getSession(false);
        JsonUtil.ok(resp, "Session active.", Map.of(
                "authenticated", true,
                "fullName", user.getFullName(),
                "role", user.getRole(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "sessionId", session.getId(),
                "loginAt", session.getAttribute(AuthService.SESSION_LOGIN_AT),
                "maxInactiveSeconds", session.getMaxInactiveInterval()
        ));
    }
}
