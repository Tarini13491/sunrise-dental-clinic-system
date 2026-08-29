package com.sunrisedental.controller;

import com.sunrisedental.service.AuthService;
import com.sunrisedental.util.JsonUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class LoginServlet extends HttpServlet {

    private final AuthService auth = new AuthService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = JsonUtil.readObject(req);
        String username = JsonUtil.str(body, "username");
        String password = JsonUtil.str(body, "password");
        boolean remember = body.has("remember") && body.get("remember").getAsBoolean();
        var result = auth.login(req, resp, username, password, remember);
        boolean ok = Boolean.TRUE.equals(result.get("success"));
        JsonUtil.write(resp, ok ? 200 : 401, ok, String.valueOf(result.get("message")), result);
    }
}
