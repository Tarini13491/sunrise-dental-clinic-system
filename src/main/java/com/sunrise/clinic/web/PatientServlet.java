package com.sunrise.clinic.web;

import com.google.gson.JsonObject;
import com.sunrise.clinic.model.SessionUser;
import com.sunrise.clinic.service.ClinicServices;
import com.sunrise.clinic.service.PatientService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {"/api/patients", "/api/patients/*"})
public class PatientServlet extends ApiServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            SessionUser actor = currentUser(request);
            PatientService patients = ClinicServices.INSTANCE.patients();
            String[] parts = segments(request);
            if (parts.length == 0) {
                writeOk(response, patients.list(actor, request.getParameter("q")));
                return;
            }
            if (parts.length == 1) {
                writeOk(response, patients.find(actor, pathId(parts[0], "Patient")));
                return;
            }
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "The requested service was not found.");
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            SessionUser actor = currentUser(request);
            JsonObject body = readBody(request);
            writeOk(response, ClinicServices.INSTANCE.patients().register(
                    actor,
                    text(body, "fullName"),
                    integer(body, "age"),
                    text(body, "address"),
                    text(body, "contactNumber")
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
            writeOk(response, ClinicServices.INSTANCE.patients().update(
                    currentUser(request),
                    pathId(parts[0], "Patient"),
                    text(body, "fullName"),
                    integer(body, "age"),
                    text(body, "address"),
                    text(body, "contactNumber")
            ));
        });
    }
}
