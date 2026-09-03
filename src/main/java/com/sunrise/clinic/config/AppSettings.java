package com.sunrise.clinic.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public enum AppSettings {
    INSTANCE;

    private final Properties properties = new Properties();

    AppSettings() {
        try (InputStream stream = AppSettings.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load application settings.", exception);
        }
        Path localFile = Path.of("application.local.properties");
        if (Files.exists(localFile)) {
            try (InputStream stream = Files.newInputStream(localFile)) {
                properties.load(stream);
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to load local application settings.", exception);
            }
        }
    }

    public String dbHost() {
        return value("db.host", "127.0.0.1");
    }

    public int dbPort() {
        return Integer.parseInt(value("db.port", "3306"));
    }

    public String dbName() {
        return value("db.name", "sunrise_clinic");
    }

    public String dbUser() {
        return value("db.user", "root");
    }

    public String dbPassword() {
        return value("db.password", "");
    }

    public String jdbcUrl() {
        return jdbcServerUrl() + dbName() + connectionFlags();
    }

    public String jdbcServerUrl() {
        return "jdbc:mysql://" + dbHost() + ":" + dbPort() + "/";
    }

    public String serverHost() {
        return value("server.host", "0.0.0.0");
    }

    public int serverPort() {
        return Integer.parseInt(value("server.port", "8080"));
    }

    public int sessionTimeoutMinutes() {
        return Integer.parseInt(value("session.timeout.minutes", "30"));
    }

    public String clinicName() {
        return value("clinic.name", "Sunrise Dental Clinic");
    }

    public String clinicAddress() {
        return value("clinic.address", "42 Galle Road, Colombo 03, Sri Lanka");
    }

    public String clinicPhone() {
        return value("clinic.phone", "+94 11 234 5678");
    }

    public String adminUsername() {
        return value("admin.username", "admin");
    }

    public String adminPassword() {
        return value("admin.password", "Admin#Sunrise26");
    }

    public String adminFullName() {
        return value("admin.fullName", "Clinic Administrator");
    }

    public String adminEmail() {
        return value("admin.email", "admin@sunrisedental.lk");
    }

    public String adminContact() {
        return value("admin.contact", "0112345678");
    }

    private String connectionFlags() {
        return "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Colombo&characterEncoding=UTF-8";
    }

    private String value(String key, String fallback) {
        String found = properties.getProperty(key);
        if (found == null || found.isBlank()) {
            return fallback;
        }
        return found.trim();
    }
}
