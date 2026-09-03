package com.sunrise.clinic.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sunrise.clinic.exception.ClinicException;
import com.sunrise.clinic.exception.ValidationException;
import com.sunrise.clinic.model.SessionUser;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ApiServlet extends HttpServlet {
    public static final String SESSION_KEY = "clinicUser";

    @FunctionalInterface
    protected interface ApiAction {
        void run() throws Exception;
    }

    protected SessionUser currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_KEY);
        if (value instanceof SessionUser user) {
            return user;
        }
        return null;
    }

    protected void handle(HttpServletResponse response, ApiAction action) throws IOException {
        try {
            action.run();
        } catch (ClinicException exception) {
            writeError(response, exception.getStatusCode(), exception.getMessage());
        } catch (Exception exception) {
            writeError(response, 500, "The system could not complete this request. Please try again.");
        }
    }

    protected void writeOk(HttpServletResponse response, Object data) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        body.put("data", data);
        write(response, HttpServletResponse.SC_OK, body);
    }

    protected void writeError(HttpServletResponse response, int status, String message) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", false);
        body.put("error", message);
        write(response, status, body);
    }

    protected void write(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write(JsonSupport.gson().toJson(body));
    }

    protected JsonObject readBody(HttpServletRequest request) throws IOException {
        try {
            JsonElement element = JsonParser.parseReader(request.getReader());
            if (element == null || !element.isJsonObject()) {
                return new JsonObject();
            }
            return element.getAsJsonObject();
        } catch (Exception exception) {
            return new JsonObject();
        }
    }

    protected String text(JsonObject body, String key) {
        if (body == null || !body.has(key) || body.get(key).isJsonNull()) {
            return null;
        }
        try {
            return body.get(key).getAsString();
        } catch (Exception exception) {
            return null;
        }
    }

    protected Integer integer(JsonObject body, String key) {
        if (body == null || !body.has(key) || body.get(key).isJsonNull()) {
            return null;
        }
        try {
            return body.get(key).getAsInt();
        } catch (Exception exception) {
            throw new ValidationException(key.substring(0, 1).toUpperCase() + key.substring(1) + " must be a number.");
        }
    }

    protected boolean flag(JsonObject body, String key) {
        if (body == null || !body.has(key) || body.get(key).isJsonNull()) {
            return false;
        }
        try {
            if (body.get(key).isJsonPrimitive() && body.get(key).getAsJsonPrimitive().isBoolean()) {
                return body.get(key).getAsBoolean();
            }
            return "true".equalsIgnoreCase(body.get(key).getAsString());
        } catch (Exception exception) {
            return false;
        }
    }

    protected String[] segments(HttpServletRequest request) {
        String info = request.getPathInfo();
        if (info == null || info.isBlank() || "/".equals(info)) {
            String uri = request.getRequestURI().substring(request.getContextPath().length());
            int query = uri.indexOf('?');
            if (query >= 0) {
                uri = uri.substring(0, query);
            }
            String[] bits = Arrays.stream(uri.split("/")).filter(part -> !part.isBlank()).toArray(String[]::new);
            if (bits.length >= 3 && "api".equals(bits[0])) {
                return Arrays.copyOfRange(bits, 2, bits.length);
            }
            return new String[0];
        }
        return Arrays.stream(info.split("/")).filter(part -> !part.isBlank()).toArray(String[]::new);
    }

    protected int pathId(String value, String label) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new ValidationException(label + " is invalid.");
        }
    }
}
