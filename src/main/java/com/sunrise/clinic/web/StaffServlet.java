package com.sunrise.clinic.web;

import com.google.gson.JsonObject;
import com.sunrise.clinic.model.AccountStatus;
import com.sunrise.clinic.model.SessionUser;
import com.sunrise.clinic.service.ClinicServices;
import com.sunrise.clinic.service.StaffService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {"/api/staff", "/api/staff/*"})
public class StaffServlet extends ApiServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            SessionUser actor = currentUser(request);
            StaffService staff = ClinicServices.INSTANCE.staff();
            String[] parts = segments(request);
            if (parts.length == 0) {
                writeOk(response, staff.list(actor));
                return;
            }
            if (parts.length == 1) {
                writeOk(response, staff.find(actor, pathId(parts[0], "Staff member")));
                return;
            }
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "The requested service was not found.");
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            SessionUser actor = currentUser(request);
            StaffService staff = ClinicServices.INSTANCE.staff();
            String[] parts = segments(request);
            JsonObject body = readBody(request);
            if (parts.length == 0) {
                writeOk(response, staff.register(
                        actor,
                        text(body, "username"),
                        text(body, "password"),
                        text(body, "fullName"),
                        text(body, "email"),
                        text(body, "contactNumber")
                ));
                return;
            }
            if (parts.length == 2 && "block".equals(parts[1])) {
                writeOk(response, staff.changeStatus(actor, pathId(parts[0], "Staff member"), AccountStatus.BLOCKED));
                return;
            }
            if (parts.length == 2 && "activate".equals(parts[1])) {
                writeOk(response, staff.changeStatus(actor, pathId(parts[0], "Staff member"), AccountStatus.ACTIVE));
                return;
            }
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "The requested service was not found.");
        });
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            SessionUser actor = currentUser(request);
            String[] parts = segments(request);
            if (parts.length != 1) {
                writeError(response, HttpServletResponse.SC_NOT_FOUND, "The requested service was not found.");
                return;
            }
            JsonObject body = readBody(request);
            writeOk(response, ClinicServices.INSTANCE.staff().update(
                    actor,
                    pathId(parts[0], "Staff member"),
                    text(body, "fullName"),
                    text(body, "email"),
                    text(body, "contactNumber"),
                    text(body, "password")
            ));
        });
    }
}
