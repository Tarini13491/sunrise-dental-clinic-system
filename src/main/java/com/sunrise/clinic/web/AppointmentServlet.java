package com.sunrise.clinic.web;

import com.google.gson.JsonObject;
import com.sunrise.clinic.model.SessionUser;
import com.sunrise.clinic.service.AppointmentService;
import com.sunrise.clinic.service.ClinicServices;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {"/api/appointments", "/api/appointments/*"})
public class AppointmentServlet extends ApiServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            SessionUser actor = currentUser(request);
            AppointmentService appointments = ClinicServices.INSTANCE.appointments();
            String[] parts = segments(request);
            String query = request.getParameter("q");
            if (query == null) {
                query = request.getParameter("number");
            }
            if (parts.length == 0 && query != null) {
                writeOk(response, appointments.search(actor, query));
                return;
            }
            if (parts.length == 0) {
                writeOk(response, appointments.list(actor));
                return;
            }
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "The requested service was not found.");
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            SessionUser actor = currentUser(request);
            AppointmentService appointments = ClinicServices.INSTANCE.appointments();
            String[] parts = segments(request);
            if (parts.length == 2 && "cancel".equals(parts[1])) {
                writeOk(response, appointments.cancel(actor, pathId(parts[0], "Appointment")));
                return;
            }
            if (parts.length == 2 && "restore".equals(parts[1])) {
                writeOk(response, appointments.restore(actor, pathId(parts[0], "Appointment")));
                return;
            }
            if (parts.length != 0) {
                writeError(response, HttpServletResponse.SC_NOT_FOUND, "The requested service was not found.");
                return;
            }
            JsonObject body = readBody(request);
            writeOk(response, appointments.register(
                    actor,
                    integer(body, "patientId"),
                    text(body, "dentistName"),
                    text(body, "treatmentType"),
                    text(body, "appointmentDate"),
                    text(body, "appointmentTime")
            ));
        });
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            String[] parts = segments(request);
            if (parts.length != 1) {
                writeError(response, HttpServletResponse.SC_NOT_FOUND, "The requested service was not found.");
                return;
            }
            JsonObject body = readBody(request);
            writeOk(response, ClinicServices.INSTANCE.appointments().update(
                    currentUser(request),
                    pathId(parts[0], "Appointment"),
                    text(body, "dentistName"),
                    text(body, "treatmentType"),
                    text(body, "appointmentDate"),
                    text(body, "appointmentTime"),
                    text(body, "status")
            ));
        });
    }
}
