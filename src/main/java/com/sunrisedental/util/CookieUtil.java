package com.sunrisedental.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class CookieUtil {

    public static final String REMEMBER_COOKIE = "SDC_REMEMBER";
    public static final String ROLE_HINT_COOKIE = "SDC_ROLE";

    private CookieUtil() {
    }

    public static void set(HttpServletResponse response, String name, String value, int maxAgeSeconds, boolean httpOnly) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setHttpOnly(httpOnly);
        response.addCookie(cookie);
    }

    public static String get(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public static void clear(HttpServletResponse response, String name) {
        set(response, name, "", 0, true);
    }
}
