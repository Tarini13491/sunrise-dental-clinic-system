package com.sunrisedental.config;

import com.sunrisedental.dao.UserDao;
import com.sunrisedental.pattern.factory.DaoFactory;
import com.sunrisedental.pattern.singleton.DatabaseConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Creates schema, routines and demo staff accounts the first time the app starts.
 */
public final class DatabaseBootstrap {

    private static final Logger LOG = Logger.getLogger(DatabaseBootstrap.class.getName());

    private DatabaseBootstrap() {
    }

    public static void run() {
        DatabaseConnection.getInstance();
        Path root = Path.of("").toAbsolutePath();
        Path schema = resolve(root, "database/schema.sql");
        Path routines = resolve(root, "database/routines.sql");
        Path seed = resolve(root, "database/seed.sql");

        if (!tableExists("users")) {
            LOG.info("Installing MySQL schema...");
            if (schema != null) {
                executeScript(schema, ";");
            }
        }
        if (!routineExists("fn_next_appointment_number")) {
            LOG.info("Installing stored procedures, functions and triggers...");
            if (routines != null) {
                executeScriptWithDelimiter(routines);
            }
        }
        if (tableExists("treatments") && count("treatments") == 0 && seed != null) {
            executeScript(seed, ";");
        }
        new UserDao().ensureDefaultUsers();
        linkDentistAccounts();
        seedDemoAppointments();
        LOG.info("Database ready.");
    }

    private static Path resolve(Path root, String relative) {
        Path path = root.resolve(relative);
        if (Files.isRegularFile(path)) {
            return path;
        }
        Path alt = root.resolve("sunrise-dental-clinic").resolve(relative);
        return Files.isRegularFile(alt) ? alt : path;
    }

    private static boolean tableExists(String table) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?")) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private static boolean routineExists(String name) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema = DATABASE() AND routine_name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private static int count(String table) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            return 0;
        }
    }

    private static void executeScript(Path file, String delimiter) {
        try {
            String sql = Files.readString(file, StandardCharsets.UTF_8);
            runBatches(split(sql, delimiter));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + file, e);
        }
    }

    private static void executeScriptWithDelimiter(Path file) {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String delimiter = ";";
            StringBuilder buf = new StringBuilder();
            List<String> batches = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.toUpperCase().startsWith("DELIMITER")) {
                    delimiter = trimmed.substring("DELIMITER".length()).trim();
                    continue;
                }
                buf.append(line).append('\n');
                String current = buf.toString();
                String check = current.stripTrailing();
                if (check.endsWith(delimiter)) {
                    String stmt = check.substring(0, check.length() - delimiter.length()).trim();
                    if (!stmt.isEmpty()) {
                        batches.add(stmt);
                    }
                    buf.setLength(0);
                }
            }
            runBatches(batches);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + file, e);
        }
    }

    private static List<String> split(String sql, String delimiter) {
        List<String> batches = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (String line : sql.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                continue;
            }
            buf.append(line).append('\n');
            if (trimmed.endsWith(delimiter)) {
                String stmt = buf.toString().trim();
                if (stmt.endsWith(delimiter)) {
                    stmt = stmt.substring(0, stmt.length() - delimiter.length()).trim();
                }
                if (!stmt.isEmpty()) {
                    batches.add(stmt);
                }
                buf.setLength(0);
            }
        }
        return batches;
    }

    private static void runBatches(List<String> batches) {
        Connection c = null;
        try {
            c = DatabaseConnection.getInstance().getConnection();
            try (Statement st = c.createStatement()) {
                for (String batch : batches) {
                    String upper = batch.toUpperCase();
                    if (upper.startsWith("USE ") || upper.startsWith("CREATE DATABASE")) {
                        continue;
                    }
                    try {
                        st.execute(batch);
                    } catch (SQLException e) {
                        LOG.warning("SQL skipped: " + e.getMessage() + " :: " + batch.substring(0, Math.min(80, batch.length())));
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Schema install failed", e);
        } finally {
            DatabaseConnection.getInstance().release(c);
        }
    }

    private static void linkDentistAccounts() {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE dentists d JOIN users u ON u.full_name = d.full_name SET d.user_id = u.user_id WHERE d.user_id IS NULL")) {
            ps.executeUpdate();
        } catch (SQLException ignored) {
            // optional convenience link
        }
    }

    private static void seedDemoAppointments() {
        if (count("appointments") > 0) {
            return;
        }
        try {
            var dao = DaoFactory.get().appointments();
            var dentists = dao.listDentists();
            var treatments = dao.listTreatments();
            if (dentists.isEmpty() || treatments.isEmpty()) {
                return;
            }
            LocalDate day = LocalDate.now();
            if (day.getDayOfWeek().getValue() == 7) {
                day = day.plusDays(1);
            }
            dao.registerViaProcedure("Ishara Fernando", "12 Flower Road, Colombo 07",
                    "0775551001", "ishara.f@example.com",
                    dentists.get(0).getDentistId(), treatments.get(1).getTreatmentId(),
                    java.sql.Date.valueOf(day), java.sql.Time.valueOf(LocalTime.of(9, 0)),
                    "First visit — mild sensitivity", 1);
            dao.registerViaProcedure("Dinesh Perera", "88 Marine Drive, Colombo 03",
                    "0775551002", "dinesh.p@example.com",
                    dentists.get(Math.min(1, dentists.size() - 1)).getDentistId(),
                    treatments.get(Math.min(3, treatments.size() - 1)).getTreatmentId(),
                    java.sql.Date.valueOf(day), java.sql.Time.valueOf(LocalTime.of(10, 30)),
                    "Follow-up after filling", 1);
            dao.registerViaProcedure("Amaya Silva", "5 Ward Place, Colombo 07",
                    "0775551003", "amaya.s@example.com",
                    dentists.get(Math.min(2, dentists.size() - 1)).getDentistId(),
                    treatments.get(Math.min(8, treatments.size() - 1)).getTreatmentId(),
                    java.sql.Date.valueOf(day.plusDays(1)), java.sql.Time.valueOf(LocalTime.of(14, 0)),
                    "Child check-up", 1);
        } catch (RuntimeException e) {
            LOG.warning("Demo appointments were not created: " + e.getMessage());
        }
    }
}
