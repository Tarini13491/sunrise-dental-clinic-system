package com.sunrise.clinic.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public enum DatabaseManager {
    INSTANCE;

    DatabaseManager() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("MySQL driver is not available.", exception);
        }
    }

    public Connection open() throws SQLException {
        AppSettings settings = AppSettings.INSTANCE;
        return DriverManager.getConnection(settings.jdbcUrl(), settings.dbUser(), settings.dbPassword());
    }

    public Connection openServer() throws SQLException {
        AppSettings settings = AppSettings.INSTANCE;
        return DriverManager.getConnection(settings.jdbcServerUrl(), settings.dbUser(), settings.dbPassword());
    }
}
