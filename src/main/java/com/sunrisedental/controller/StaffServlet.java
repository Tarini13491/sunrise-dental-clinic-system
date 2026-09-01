package com.sunrisedental.controller;

import com.google.gson.JsonObject;
import com.sunrisedental.model.User;
import com.sunrisedental.service.AuthService;
import com.sunrisedental.service.StaffService;
import com.sunrisedental.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

public class StaffServlet extends HttpServlet {

    private final StaffService service = new StaffService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requireAdmin(req, resp)) {
            return;
        }
        Map<String, Object> result = service.list();
        JsonUtil.ok(resp, String.valueOf(result.get("message")), result);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requireAdmin(req, resp)) {
            return;
        }
        JsonObject body = JsonUtil.readObject(req);
        String action = JsonUtil.str(body, "action");
        User actor = AuthService.current(req);
        Map<String, Object> result;
        if ("update".equalsIgnoreCase(action)) {
            result = service.update(readUserId(body), JsonUtil.str(body, "username"), JsonUtil.str(body, "password"),
                    JsonUtil.str(body, "fullName"), JsonUtil.str(body, "email"), JsonUtil.str(body, "phone"));
        } else if ("block".equalsIgnoreCase(action)) {
            result = service.setBlocked(readUserId(body), true, actor);
        } else if ("unblock".equalsIgnoreCase(action)) {
            result = service.setBlocked(readUserId(body), false, actor);
        } else if ("remove".equalsIgnoreCase(action)) {
            result = service.remove(readUserId(body), actor);
        } else {
            result = service.create(JsonUtil.str(body, "username"), JsonUtil.str(body, "password"),
                    JsonUtil.str(body, "fullName"), JsonUtil.str(body, "email"), JsonUtil.str(body, "phone"));
        }
        boolean ok = Boolean.TRUE.equals(result.get("success"));
        JsonUtil.write(resp, ok ? 200 : 400, ok, String.valueOf(result.get("message")), result);
    }

    private boolean requireAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = AuthService.current(req);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            JsonUtil.fail(resp, 403, "Only the clinic administrator can manage staff accounts.");
            return false;
        }
        return true;
    }

    private int readUserId(JsonObject body) {
        try {
            String value = JsonUtil.str(body, "userId");
            if (value == null && body.has("userId") && !body.get("userId").isJsonNull()) {
                return body.get("userId").getAsInt();
            }
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }
}