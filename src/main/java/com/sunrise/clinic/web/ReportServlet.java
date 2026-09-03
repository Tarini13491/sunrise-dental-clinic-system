package com.sunrise.clinic.web;

import com.sunrise.clinic.model.SessionUser;
import com.sunrise.clinic.service.ClinicServices;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {"/api/reports", "/api/reports/*"})
public class ReportServlet extends ApiServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            SessionUser actor = currentUser(request);
            String[] parts = segments(request);
            if (parts.length == 0 || (parts.length == 1 && "summary".equals(parts[0]))) {
                writeOk(response, ClinicServices.INSTANCE.reports().summary(actor));
                return;
            }
            if (parts.length == 1 && "patients".equals(parts[0])) {
                writeOk(response, ClinicServices.INSTANCE.patients().list(actor, request.getParameter("q")));
                return;
            }
            if (parts.length == 1 && "appointments".equals(parts[0])) {
                writeOk(response, ClinicServices.INSTANCE.appointments().list(actor));
                return;
            }
            if (parts.length == 1 && "billing".equals(parts[0])) {
                writeOk(response, ClinicServices.INSTANCE.billing().list(actor));
                return;
            }
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "The requested service was not found.");
        });
    }
}
