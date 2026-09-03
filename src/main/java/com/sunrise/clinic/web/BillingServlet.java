package com.sunrise.clinic.web;

import com.google.gson.JsonObject;
import com.sunrise.clinic.model.SessionUser;
import com.sunrise.clinic.service.BillingService;
import com.sunrise.clinic.service.ClinicServices;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {"/api/bills", "/api/bills/*"})
public class BillingServlet extends ApiServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            SessionUser actor = currentUser(request);
            BillingService billing = ClinicServices.INSTANCE.billing();
            String[] parts = segments(request);
            if (parts.length == 0) {
                writeOk(response, billing.list(actor));
                return;
            }
            if (parts.length == 1 && "preview".equals(parts[0])) {
                writeOk(response, billing.preview(actor, parseQueryInt(request.getParameter("appointmentId"))));
                return;
            }
            if (parts.length == 1) {
                writeOk(response, billing.find(actor, pathId(parts[0], "Bill")));
                return;
            }
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "The requested service was not found.");
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            JsonObject body = readBody(request);
            writeOk(response, ClinicServices.INSTANCE.billing().create(currentUser(request), integer(body, "appointmentId")));
        });
    }

    private Integer parseQueryInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
