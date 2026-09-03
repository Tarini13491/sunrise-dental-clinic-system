package com.sunrise.clinic.web;

import com.google.gson.JsonObject;
import com.sunrise.clinic.model.SessionUser;
import com.sunrise.clinic.service.ClinicServices;
import com.sunrise.clinic.service.DentistService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {"/api/dentists", "/api/dentists/*"})
public class DentistServlet extends ApiServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            SessionUser actor = currentUser(request);
            DentistService dentists = ClinicServices.INSTANCE.dentists();
            if ("true".equalsIgnoreCase(request.getParameter("active"))) {
                writeOk(response, dentists.listActive(actor));
                return;
            }
            writeOk(response, dentists.list(actor));
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            SessionUser actor = currentUser(request);
            DentistService dentists = ClinicServices.INSTANCE.dentists();
            String[] parts = segments(request);
            if (parts.length == 0) {
                JsonObject body = readBody(request);
                writeOk(response, dentists.register(actor, text(body, "fullName")));
                return;
            }
            if (parts.length == 2 && "remove".equals(parts[1])) {
                writeOk(response, dentists.remove(actor, pathId(parts[0], "Dentist")));
                return;
            }
            if (parts.length == 2 && "restore".equals(parts[1])) {
                writeOk(response, dentists.restore(actor, pathId(parts[0], "Dentist")));
                return;
            }
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "The requested service was not found.");
        });
    }
}
