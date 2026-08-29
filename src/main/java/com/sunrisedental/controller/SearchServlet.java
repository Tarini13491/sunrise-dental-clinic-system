package com.sunrisedental.controller;

import com.sunrisedental.model.Appointment;
import com.sunrisedental.service.AppointmentService;
import com.sunrisedental.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class SearchServlet extends HttpServlet {

    private final AppointmentService service = new AppointmentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String number = firstNonBlank(req.getParameter("appointmentNumber"), req.getParameter("q"));
        if (number != null) {
            Appointment exact = service.search(number);
            if (exact != null) {
                JsonUtil.ok(resp, "Appointment found.", Map.of("appointment", exact, "matches", List.of(exact)));
                return;
            }
            List<Appointment> matches = service.searchFlexible(number);
            if (matches.isEmpty()) {
                JsonUtil.fail(resp, 404, "No appointment matches that number, name or phone. Check the spelling and try again.");
                return;
            }
            JsonUtil.ok(resp, matches.size() + " matching visit(s).",
                    Map.of("appointment", matches.get(0), "matches", matches));
            return;
        }
        String date = req.getParameter("date");
        LocalDate day = date == null || date.isBlank() ? LocalDate.now() : LocalDate.parse(date);
        JsonUtil.ok(resp, "Appointments for " + day + ".", Map.of("appointments", service.byDate(day)));
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return null;
    }
}
