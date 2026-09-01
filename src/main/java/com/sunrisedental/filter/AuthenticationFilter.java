package com.sunrisedental.filter;

import com.sunrisedental.model.User;
import com.sunrisedental.pattern.factory.DaoFactory;
import com.sunrisedental.service.AuthService;
import com.sunrisedental.util.JsonUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Set;

public class AuthenticationFilter implements Filter {

    private static final Set<String> PUBLIC = Set.of(
            "/api/login",
            "/api/session",
            "/api/health"
    );

    private final AuthService auth = new AuthService();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getServletPath();

        if (!path.startsWith("/api") || PUBLIC.contains(path) || "OPTIONS".equals(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        User user = session == null ? null : AuthService.current(req);
        if (user == null) {
            user = auth.fromRememberCookie(req);
            if (user != null) {
                auth.restoreSession(req, user);
            }
        }
        if (user == null) {
            JsonUtil.fail(res, 401, "Your session has ended. Please sign in again.");
            return;
        }
        DaoFactory.get().users().touchSession(req.getSession().getId());
        if (path.startsWith("/api/staff") && !"ADMIN".equals(user.getRole())) {
            JsonUtil.fail(res, 403, "Only the clinic administrator can manage staff accounts.");
            return;
        }
        chain.doFilter(request, response);
    }
}