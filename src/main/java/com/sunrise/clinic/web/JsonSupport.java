package com.sunrise.clinic.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class JsonSupport {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (value, type, context) -> new JsonPrimitive(value.format(DATE)))
            .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, type, context) -> LocalDate.parse(json.getAsString(), DATE))
            .registerTypeAdapter(LocalTime.class, (JsonSerializer<LocalTime>) (value, type, context) -> new JsonPrimitive(value.format(TIME)))
            .registerTypeAdapter(LocalTime.class, (JsonDeserializer<LocalTime>) (json, type, context) -> LocalTime.parse(json.getAsString()))
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (value, type, context) -> new JsonPrimitive(value.format(DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, context) -> LocalDateTime.parse(json.getAsString(), DATE_TIME))
            .create();

    private JsonSupport() {
    }

    public static Gson gson() {
        return GSON;
    }
}
