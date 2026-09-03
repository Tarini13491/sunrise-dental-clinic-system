package com.sunrise.clinic.web;

import com.sunrise.clinic.catalog.TreatmentCatalog;
import com.sunrise.clinic.security.AccessPolicy;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/treatments")
public class TreatmentServlet extends ApiServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, () -> {
            AccessPolicy.requireUser(currentUser(request));
            writeOk(response, TreatmentCatalog.all());
        });
    }
}
