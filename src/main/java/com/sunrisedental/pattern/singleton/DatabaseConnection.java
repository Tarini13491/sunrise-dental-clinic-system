package com.sunrisedental.pattern.singleton;

import com.sunrisedental.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton + simple connection pool.
 *
 * Why Singleton: the clinic application must share one pool of MySQL
 * connections. Creating a new DriverManager connection for every servlet
 * request would exhaust MySQL's connection limit during busy clinic hours.
 *
 * Evaluation: Singleton is appropriate for a JVM-scoped resource. It is a
 * poor fit for per-patient state (that belongs in HttpSession). The pool is
 * intentionally small and easy to explain in the assignment report.
 */
public final class DatabaseConnection {

    private static volatile DatabaseConnection instance;

    private final String url;
    private final String user;
    private final String password;
    private final int poolSize;
    private final List<PooledConnection> pool = new ArrayList<>();

    private DatabaseConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC driver is missing from the classpath.", e);
        }
        this.url = AppConfig.get("db.url");
        this.user = AppConfig.get("db.user");
        this.password = AppConfig.get("db.password");
        this.poolSize = AppConfig.getInt("db.pool.size", 10);
        ensureDatabase();
        for (int i = 0; i < poolSize; i++) {
            pool.add(new PooledConnection(openPhysical()));
        }
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        synchronized (pool) {
            for (PooledConnection pc : pool) {
                if (!pc.inUse) {
                    if (!isValid(pc.delegate)) {
                        silentClose(pc.delegate);
                        pc.delegate = openPhysical();
                    }
                    pc.inUse = true;
                    return pc.delegate;
                }
            }
        }
        return openPhysical();
    }

    public void release(Connection connection) {
        if (connection == null) {
            return;
        }
        synchronized (pool) {
            for (PooledConnection pc : pool) {
                if (pc.delegate == connection) {
                    pc.inUse = false;
                    return;
                }
            }
        }
        silentClose(connection);
    }

    public boolean ping() {
        try (Connection c = DriverManager.getConnection(url, user, password)) {
            return c.isValid(3);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Connects without a schema first so a missing sunrise_dental database
     * does not crash the pool on a fresh MySQL install.
     */
    private void ensureDatabase() {
        String adminUrl = url.replaceFirst("/[^/?]+\\?", "/?");
        SQLException last = null;
        for (int attempt = 1; attempt <= 8; attempt++) {
            try (Connection c = DriverManager.getConnection(adminUrl, user, password);
                 Statement st = c.createStatement()) {
                st.executeUpdate("CREATE DATABASE IF NOT EXISTS sunrise_dental "
                        + "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                try {
                    st.execute("SET GLOBAL log_bin_trust_function_creators = 1");
                } catch (SQLException ignored) {
                    // The application user may not have SUPER privilege; that is fine
                    // when the DBA already enabled function creation.
                }
                return;
            } catch (SQLException e) {
                last = e;
                try {
                    Thread.sleep(500L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for MySQL.", ie);
                }
            }
        }
        throw new IllegalStateException(
                "Cannot connect to MySQL. Check that the server is running and application.properties is correct.",
                last);
    }

    private Connection openPhysical() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Cannot connect to MySQL. Check that the server is running and application.properties is correct.",
                    e);
        }
    }

    private boolean isValid(Connection c) {
        try {
            return c != null && c.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    private void silentClose(Connection c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (SQLException ignored) {
            // releasing a broken connection must not fail the request
        }
    }

    private static final class PooledConnection {
        private Connection delegate;
        private boolean inUse;

        private PooledConnection(Connection delegate) {
            this.delegate = delegate;
        }
    }
}
