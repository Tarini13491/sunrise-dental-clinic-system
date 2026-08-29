package com.sunrisedental.controller;

import com.sunrisedental.service.AuthService;
import com.sunrisedental.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

public class LogoutServlet extends HttpServlet {

    private final AuthService auth = new AuthService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        auth.logout(req, resp);
        JsonUtil.ok(resp, "You have signed out. The clinic desk is now locked.", Map.of());
    }
}
