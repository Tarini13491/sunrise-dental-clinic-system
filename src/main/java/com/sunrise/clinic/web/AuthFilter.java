package com.sunrise.clinic.web;

import com.sunrise.clinic.config.AppSettings;
import com.sunrise.clinic.exception.UnauthorizedException;
import com.sunrise.clinic.model.SessionUser;
import com.sunrise.clinic.service.ClinicServices;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@WebFilter(urlPatterns = {"/api/*", "/desk.html"})
public class AuthFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean loginAttempt = "POST".equalsIgnoreCase(request.getMethod()) && "/api/auth/login".equals(path);
        SessionUser user = readUser(request);
        if (user == null && !loginAttempt) {
            user = restorePersistentLogin(request, response);
        }
        if (user != null) {
            request.setAttribute(ApiServlet.SESSION_KEY, user);
        }
        if (isPublic(request.getMethod(), path)) {
            chain.doFilter(request, response);
            return;
        }
        if (user == null) {
            if (path.startsWith("/api/")) {
                writeUnauthorized(response);
            } else {
                response.sendRedirect(request.getContextPath() + "/login.html");
            }
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isPublic(String method, String path) {
        if ("POST".equalsIgnoreCase(method) && "/api/auth/login".equals(path)) {
            return true;
        }
        if ("POST".equalsIgnoreCase(method) && "/api/auth/logout".equals(path)) {
            return true;
        }
        return "GET".equalsIgnoreCase(method) && "/api/auth/session".equals(path);
    }

    private SessionUser readUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(ApiServlet.SESSION_KEY);
        if (value instanceof SessionUser user) {
            return user;
        }
        return null;
    }

    private SessionUser restorePersistentLogin(HttpServletRequest request, HttpServletResponse response) {
        String token = AuthCookies.read(request);
        if (token == null) {
            return null;
        }
        SessionUser user = ClinicServices.INSTANCE.auth().restorePersistentLogin(token).orElse(null);
        if (user == null) {
            AuthCookies.clear(request, response);
            return null;
        }
        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval(AppSettings.INSTANCE.sessionTimeoutMinutes() * 60);
        session.setAttribute(ApiServlet.SESSION_KEY, user);
        return user;
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", false);
        body.put("error", new UnauthorizedException("Your session has ended. Please log in again.").getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write(JsonSupport.gson().toJson(body));
    }
}
