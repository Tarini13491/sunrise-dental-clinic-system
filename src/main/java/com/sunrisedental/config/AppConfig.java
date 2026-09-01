package com.sunrisedental.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = AppConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (in == null) {
                throw new IllegalStateException("application.properties was not found on the classpath.");
            }
            PROPS.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load application.properties", e);
        }
    }

    private AppConfig() {
    }

    public static String get(String key) {
        String envKey = key.toUpperCase().replace('.', '_');
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) {
            return env;
        }
        return PROPS.getProperty(key);
    }

    public static String get(String key, String fallback) {
        String value = get(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    public static int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(get(key, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static double getDouble(String key, double fallback) {
        try {
            return Double.parseDouble(get(key, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static boolean getBoolean(String key, boolean fallback) {
        String value = get(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }
}
