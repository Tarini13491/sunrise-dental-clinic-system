package com.sunrisedental.controller;

import com.sunrisedental.service.HelpService;
import com.sunrisedental.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

public class HelpServlet extends HttpServlet {

    private final HelpService help = new HelpService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonUtil.ok(resp, "Staff handbook.", Map.of("steps", help.steps()));
    }
}
