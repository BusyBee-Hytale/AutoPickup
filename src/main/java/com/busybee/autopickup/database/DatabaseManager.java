package com.busybee.autopickup.database;

import com.busybee.autopickup.AutoPickupPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static com.busybee.autopickup.AutoPickupPlugin.LOGGER;

public class DatabaseManager {

    private final AutoPickupPlugin plugin;
    private Connection connection;
    private final String databasePath;

    public DatabaseManager(AutoPickupPlugin plugin) {
        this.plugin = plugin;
        File dataFolder = plugin.getResourcesFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.databasePath = "jdbc:sqlite:" + new File(dataFolder, "autopickup.db").getAbsolutePath();
    }

    public void initialize() {
        try {

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(databasePath);

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA synchronous=NORMAL");
                stmt.execute("PRAGMA cache_size=10000");
                stmt.execute("PRAGMA temp_store=MEMORY");
            }
            createTables();

            LOGGER.atInfo().log("SQLite database initialized successfully at: " + databasePath);
        } catch (ClassNotFoundException e) {
            LOGGER.atSevere().log("SQLite JDBC driver not found: " + e.getMessage());
        } catch (SQLException e) {
            LOGGER.atSevere().log("Failed to initialize database: " + e.getMessage());
        }
    }

    private void createTables() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS player_settings (
                uuid TEXT PRIMARY KEY,
                autopickup_enabled INTEGER NOT NULL DEFAULT 0,
                pickup_radius INTEGER NOT NULL DEFAULT 5,
                last_updated INTEGER NOT NULL
            )
            """;

        String createIndexSQL = """
            CREATE INDEX IF NOT EXISTS idx_autopickup_enabled
            ON player_settings(autopickup_enabled)
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            stmt.execute(createIndexSQL);

            LOGGER.atInfo().log("Database tables created/verified");
        } catch (SQLException e) {
            LOGGER.atSevere().log("Failed to create database tables: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            LOGGER.atWarning().log("Database connection lost, reconnecting...");
            connection = DriverManager.getConnection(databasePath);
        }
        return connection;
    }

    public void close() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    LOGGER.atInfo().log("Database connection closed");
                }
            } catch (SQLException e) {
                LOGGER.atWarning().log("Error closing database connection: " + e.getMessage());
            }
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
