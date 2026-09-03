package com.sunrise.clinic.config;

import com.sunrise.clinic.dao.DaoFactory;
import com.sunrise.clinic.dao.UserDao;
import com.sunrise.clinic.model.AccountStatus;
import com.sunrise.clinic.model.Role;
import com.sunrise.clinic.model.UserAccount;
import com.sunrise.clinic.security.PasswordHasher;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

public final class SchemaBootstrap {
    private SchemaBootstrap() {
    }

    public static void prepare() {
        AppSettings settings = AppSettings.INSTANCE;
        try (Connection connection = DatabaseManager.INSTANCE.openServer();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + settings.dbName()
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to prepare the clinic database. Check MySQL is running and the credentials in application.properties.", exception);
        }
        String schema = loadSchema();
        try (Connection connection = DatabaseManager.INSTANCE.open();
             Statement statement = connection.createStatement()) {
            for (String part : schema.split(";")) {
                String sql = part.trim();
                if (!sql.isEmpty()) {
                    statement.execute(sql);
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create the clinic tables.", exception);
        }
        seedAdmin();
    }

    private static void seedAdmin() {
        AppSettings settings = AppSettings.INSTANCE;
        UserDao users = DaoFactory.INSTANCE.users();
        if (users.hasRole(Role.ADMIN)) {
            return;
        }
        UserAccount admin = new UserAccount();
        admin.setUsername(settings.adminUsername());
        admin.setPasswordHash(PasswordHasher.hash(settings.adminPassword()));
        admin.setFullName(settings.adminFullName());
        admin.setEmail(settings.adminEmail());
        admin.setContactNumber(settings.adminContact());
        admin.setRole(Role.ADMIN);
        admin.setStatus(AccountStatus.ACTIVE);
        users.insert(admin);
    }

    private static String loadSchema() {
        InputStream stream = SchemaBootstrap.class.getClassLoader().getResourceAsStream("db/schema.sql");
        if (stream == null) {
            throw new IllegalStateException("Database schema file was not found on the classpath.");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to read the database schema file.", exception);
        }
    }
}
