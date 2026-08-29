package com.sunrisedental.controller;

import com.sunrisedental.service.ReportService;
import com.sunrisedental.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class DashboardServlet extends HttpServlet {

    private final ReportService reports = new ReportService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonUtil.ok(resp, "Dashboard loaded.", reports.dashboard().asMap());
    }
}
