package com.sunrise.clinic.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class AuthCookies {
    public static final String NAME = "clinicRemember";
    public static final int MAX_AGE_SECONDS = 60 * 60 * 24 * 30;

    private AuthCookies() {
    }

    public static String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (NAME.equals(cookie.getName())) {
                String value = cookie.getValue();
                if (value == null || value.isBlank()) {
                    return null;
                }
                return value;
            }
        }
        return null;
    }

    public static void store(HttpServletRequest request, HttpServletResponse response, String token) {
        write(request, response, token, MAX_AGE_SECONDS);
    }

    public static void clear(HttpServletRequest request, HttpServletResponse response) {
        write(request, response, "", 0);
    }

    private static void write(HttpServletRequest request, HttpServletResponse response, String token, int maxAge) {
        StringBuilder header = new StringBuilder();
        header.append(NAME).append("=").append(token == null ? "" : token);
        header.append("; Path=/; HttpOnly; SameSite=Lax; Max-Age=").append(maxAge);
        if (request.isSecure()) {
            header.append("; Secure");
        }
        response.addHeader("Set-Cookie", header.toString());
    }
}
