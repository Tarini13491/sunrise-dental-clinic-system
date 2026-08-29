package com.sunrisedental.controller;

import com.sunrisedental.config.AppConfig;
import com.sunrisedental.pattern.singleton.DatabaseConnection;
import com.sunrisedental.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

public class HealthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        boolean db = DatabaseConnection.getInstance().ping();
        JsonUtil.write(resp, db ? 200 : 503, db,
                db ? "Sunrise Dental Clinic is online." : "The database is not reachable.",
                Map.of(
                        "clinic", AppConfig.get("clinic.name"),
                        "database", db ? "up" : "down"
                ));
    }
}
