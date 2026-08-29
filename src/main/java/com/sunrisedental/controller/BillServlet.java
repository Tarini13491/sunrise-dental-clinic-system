package com.sunrisedental.controller;

import com.google.gson.JsonObject;
import com.sunrisedental.service.BillingService;
import com.sunrisedental.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

public class BillServlet extends HttpServlet {

    private final BillingService service = new BillingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String appointmentNumber = req.getParameter("appointmentNumber");
        if (appointmentNumber == null || appointmentNumber.isBlank()) {
            JsonUtil.fail(resp, 400, "Enter an appointment number to prepare the bill.");
            return;
        }
        Map<String, Object> result = service.preview(appointmentNumber.trim().toUpperCase());
        boolean ok = Boolean.TRUE.equals(result.get("success"));
        JsonUtil.write(resp, ok ? 200 : 404, ok, String.valueOf(result.get("message")), result);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = JsonUtil.readObject(req);
        String action = JsonUtil.str(body, "action");
        if ("pay".equalsIgnoreCase(action)) {
            BigDecimal amount = body.has("amount") ? body.get("amount").getAsBigDecimal() : null;
            Map<String, Object> result = service.pay(
                    JsonUtil.str(body, "billNumber"),
                    JsonUtil.str(body, "method"),
                    amount);
            boolean ok = Boolean.TRUE.equals(result.get("success"));
            JsonUtil.write(resp, ok ? 200 : 400, ok, String.valueOf(result.get("message")), result);
            return;
        }
        BigDecimal extra = BigDecimal.ZERO;
        if (body.has("extraDiscount") && !body.get("extraDiscount").isJsonNull()) {
            extra = body.get("extraDiscount").getAsBigDecimal();
        }
        Map<String, Object> result = service.calculate(JsonUtil.str(body, "appointmentNumber"), extra);
        boolean ok = Boolean.TRUE.equals(result.get("success"));
        JsonUtil.write(resp, ok ? 200 : 400, ok, String.valueOf(result.get("message")), result);
    }
}
