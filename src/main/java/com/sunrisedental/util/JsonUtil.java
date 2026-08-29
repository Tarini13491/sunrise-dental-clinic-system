package com.sunrisedental.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

public final class JsonUtil {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .registerTypeHierarchyAdapter(Date.class, (JsonSerializer<Date>) (src, type, ctx) -> {
                if (src == null) {
                    return JsonNull.INSTANCE;
                }
                if (src instanceof Time time) {
                    return new JsonPrimitive(time.toLocalTime().format(TIME));
                }
                if (src instanceof Timestamp timestamp) {
                    return new JsonPrimitive(timestamp.toInstant().toString());
                }
                if (src instanceof java.sql.Date sqlDate) {
                    return new JsonPrimitive(sqlDate.toLocalDate().toString());
                }
                return new JsonPrimitive(src.toInstant().toString());
            })
            .create();

    private JsonUtil() {
    }

    public static Gson gson() {
        return GSON;
    }

    public static JsonObject readObject(HttpServletRequest request) throws IOException {
        String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (body.isBlank()) {
            return new JsonObject();
        }
        return GSON.fromJson(body, JsonObject.class);
    }

    public static void write(HttpServletResponse response, int status, boolean success, String message, Object data)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> payload = Map.of(
                "success", success,
                "message", message == null ? "" : message,
                "data", data == null ? Map.of() : data
        );
        response.getWriter().write(GSON.toJson(payload));
    }

    public static void ok(HttpServletResponse response, String message, Object data) throws IOException {
        write(response, 200, true, message, data);
    }

    public static void fail(HttpServletResponse response, int status, String message) throws IOException {
        write(response, status, false, message, Map.of());
    }

    public static String str(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        String value = obj.get(key).getAsString();
        return value == null ? null : value.trim();
    }
}
