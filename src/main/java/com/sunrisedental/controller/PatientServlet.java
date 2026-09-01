package com.sunrisedental.controller;

import com.google.gson.JsonObject;
import com.sunrisedental.service.PatientService;
import com.sunrisedental.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

public class PatientServlet extends HttpServlet {

    private final PatientService service = new PatientService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> result = service.list(req.getParameter("q"));
        JsonUtil.ok(resp, String.valueOf(result.get("message")), result);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = JsonUtil.readObject(req);
        String action = JsonUtil.str(body, "action");
        Map<String, Object> result;
        if ("remove".equalsIgnoreCase(action)) {
            result = service.remove(readId(body, "patientId"));
        } else {
            Integer id = readOptionalId(body, "patientId");
            result = service.save(id,
                    JsonUtil.str(body, "fullName"),
                    JsonUtil.str(body, "address"),
                    JsonUtil.str(body, "contactNumber"),
                    JsonUtil.str(body, "email"),
                    JsonUtil.str(body, "dateOfBirth"),
                    JsonUtil.str(body, "gender"),
                    JsonUtil.str(body, "notes"));
        }
        boolean ok = Boolean.TRUE.equals(result.get("success"));
        JsonUtil.write(resp, ok ? 200 : 400, ok, String.valueOf(result.get("message")), result);
    }

    private Integer readOptionalId(JsonObject body, String key) {
        int id = readId(body, key);
        return id <= 0 ? null : id;
    }

    private int readId(JsonObject body, String key) {
        try {
            String value = JsonUtil.str(body, key);
            if (value == null && body.has(key) && !body.get(key).isJsonNull()) {
                return body.get(key).getAsInt();
            }
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }
}