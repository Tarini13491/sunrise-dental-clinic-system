package com.sunrisedental.controller;

import com.sunrisedental.pattern.factory.DaoFactory;
import com.sunrisedental.service.ReportService;
import com.sunrisedental.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

public class ReportServlet extends HttpServlet {

    private final ReportService reports = new ReportService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String type = req.getParameter("type");
        if ("audit".equals(type)) {
            JsonUtil.ok(resp, "Audit trail.", Map.of("rows", DaoFactory.get().reports().auditTrail(40)));
            return;
        }
        if ("notifications".equals(type)) {
            JsonUtil.ok(resp, "Notifications.", Map.of("rows", DaoFactory.get().reports().recentNotifications(40)));
            return;
        }
        LocalDate date = LocalDate.now();
        String dateParam = req.getParameter("date");
        if (dateParam != null && !dateParam.isBlank()) {
            date = LocalDate.parse(dateParam);
        }
        JsonUtil.ok(resp, "Report ready.", reports.report(type, date));
    }
}
