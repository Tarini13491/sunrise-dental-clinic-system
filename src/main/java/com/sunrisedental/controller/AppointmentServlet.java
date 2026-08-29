package com.sunrisedental.controller;

import com.google.gson.JsonObject;
import com.sunrisedental.service.AppointmentService;
import com.sunrisedental.service.AuthService;
import com.sunrisedental.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

public class AppointmentServlet extends HttpServlet {

    private final AppointmentService service = new AppointmentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String lists = req.getParameter("lists");
        if ("1".equals(lists) || "true".equalsIgnoreCase(lists)) {
            JsonUtil.ok(resp, "Reference lists loaded.", service.lookupLists());
            return;
        }
        String slots = req.getParameter("slots");
        if (slots != null) {
            try {
                int dentistId = Integer.parseInt(req.getParameter("dentistId"));
                LocalDate date = LocalDate.parse(req.getParameter("date"));
                JsonUtil.ok(resp, "Occupied slots.", Map.of("occupied", service.occupied(dentistId, date)));
            } catch (Exception e) {
                JsonUtil.fail(resp, 400, "Choose a dentist and a date to see free times.");
            }
            return;
        }
        JsonUtil.ok(resp, "Upcoming appointments.", Map.of("upcoming", service.upcoming()));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = JsonUtil.readObject(req);
        String action = JsonUtil.str(body, "action");
        if ("cancel".equalsIgnoreCase(action)) {
            String number = JsonUtil.str(body, "appointmentNumber");
            boolean ok = service.cancel(number);
            JsonUtil.write(resp, ok ? 200 : 404, ok,
                    ok ? "Appointment cancelled. The patient has been notified." : "Appointment was not found.",
                    Map.of());
            return;
        }
        Map<String, Object> result = service.register(
                JsonUtil.str(body, "patientName"),
                JsonUtil.str(body, "address"),
                JsonUtil.str(body, "contactNumber"),
                JsonUtil.str(body, "email"),
                JsonUtil.str(body, "dentistId"),
                JsonUtil.str(body, "treatmentId"),
                JsonUtil.str(body, "appointmentDate"),
                JsonUtil.str(body, "appointmentTime"),
                JsonUtil.str(body, "notes"),
                AuthService.current(req)
        );
        boolean ok = Boolean.TRUE.equals(result.get("success"));
        JsonUtil.write(resp, ok ? 200 : 400, ok, String.valueOf(result.get("message")), result);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doPost(req, resp);
    }
}
