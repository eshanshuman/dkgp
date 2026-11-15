package com.campusexchange.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Single place that knows how to open a database connection.
 *
 * <p>Settings are read from {@code db.properties} in the working directory:
 * <pre>
 * db.url=jdbc:mysql://localhost:3306/campus_exchange
 * db.user=root
 * db.password=secret
 * </pre>
 * If the file (or a key) is missing, sensible local defaults are used.
 * A JVM system property with the same key (e.g. {@code -Ddb.url=...}) overrides the file,
 * which is how the unit tests point the DAOs at an in-memory H2 database.
 */
public final class Db {

    private static final String PROPERTIES_FILE = "db.properties";

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/campus_exchange";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private Db() {
        // utility class - no instances
    }

    /**
     * Opens a new connection. Callers must close it, ideally with try-with-resources.
     */
    public static Connection get() throws SQLException {
        Properties props = loadProperties();
        String url = setting(props, "db.url", DEFAULT_URL);
        String user = setting(props, "db.user", DEFAULT_USER);
        String password = setting(props, "db.password", DEFAULT_PASSWORD);
        return DriverManager.getConnection(url, user, password);
    }

    /** Reads db.properties if present; returns empty properties otherwise. */
    private static Properties loadProperties() {
        Properties props = new Properties();
        Path file = Path.of(PROPERTIES_FILE);
        if (Files.isRegularFile(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
            } catch (IOException e) {
                System.err.println("Could not read " + PROPERTIES_FILE + ", using defaults: " + e.getMessage());
            }
        }
        return props;
    }

    /** System property wins, then db.properties, then the built-in default. */
    private static String setting(Properties props, String key, String fallback) {
        String fromSystem = System.getProperty(key);
        if (fromSystem != null) {
            return fromSystem;
        }
        return props.getProperty(key, fallback).trim();
    }
}
