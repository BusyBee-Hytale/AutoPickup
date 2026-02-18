package com.busybee.autopickup.manager;

import com.busybee.autopickup.AutoPickupPlugin;
import com.busybee.autopickup.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.busybee.autopickup.AutoPickupPlugin.LOGGER;

public class PlayerDataManager {

    private final AutoPickupPlugin plugin;
    private final DatabaseManager database;
    private final Map<UUID, Boolean> enabledCache;

    public PlayerDataManager(AutoPickupPlugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        this.enabledCache = new ConcurrentHashMap<>();
        loadAllData();
    }

    private void loadAllData() {
        String selectSQL = "SELECT uuid, autopickup_enabled FROM player_settings";

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSQL);
             ResultSet rs = stmt.executeQuery()) {

            int count = 0;
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    boolean enabled = rs.getInt("autopickup_enabled") == 1;

                    enabledCache.put(uuid, enabled);
                    count++;
                } catch (IllegalArgumentException e) {
                    LOGGER.atWarning().log("Invalid UUID in database: " + rs.getString("uuid"));
                }
            }
        } catch (SQLException e) {
            LOGGER.atSevere().log("Failed to load player data from database", e);
        }
    }

    public boolean isAutoPickupEnabled(UUID playerUUID) {
        return enabledCache.computeIfAbsent(playerUUID, uuid -> {
            try {
                Boolean dbValue = loadEnabledFromDatabase(uuid);
                if (dbValue != null) {
                    return dbValue;
                }
            } catch (SQLException e) {
                LOGGER.atWarning().log("Failed to load enabled state for " + uuid + " from database", e);
            }
            return plugin.getConfig().getBoolean("autopickup.default-enabled", false);
        });
    }

    public void setAutoPickupEnabled(UUID playerUUID, boolean enabled) {
        enabledCache.put(playerUUID, enabled);
        savePlayerSettings(playerUUID, enabled);
    }

    private Boolean loadEnabledFromDatabase(UUID playerUUID) throws SQLException {
        String selectSQL = "SELECT autopickup_enabled FROM player_settings WHERE uuid = ?";

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSQL)) {

            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("autopickup_enabled") == 1;
            }
        }
        return null;
    }

    private void savePlayerSettings(UUID playerUUID, boolean enabled) {
        String upsertSQL = """
            INSERT INTO player_settings (uuid, autopickup_enabled, last_updated)
            VALUES (?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                autopickup_enabled = excluded.autopickup_enabled,
                last_updated = excluded.last_updated
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(upsertSQL)) {

            stmt.setString(1, playerUUID.toString());
            stmt.setInt(2, enabled ? 1 : 0);
            stmt.setLong(3, System.currentTimeMillis());

            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.atSevere().log("Failed to save player settings for " + playerUUID, e);
        }
    }

    public void saveAllData() {
        String upsertSQL = """
            INSERT INTO player_settings (uuid, autopickup_enabled, last_updated)
            VALUES (?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                autopickup_enabled = excluded.autopickup_enabled,
                last_updated = excluded.last_updated
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(upsertSQL)) {

            int savedCount = 0;

            for (Map.Entry<UUID, Boolean> entry : enabledCache.entrySet()) {
                UUID uuid = entry.getKey();
                boolean enabled = entry.getValue();

                stmt.setString(1, uuid.toString());
                stmt.setInt(2, enabled ? 1 : 0);
                stmt.setLong(3, System.currentTimeMillis());

                stmt.addBatch();
                savedCount++;
            }

            if (savedCount > 0) {
                stmt.executeBatch();
            }
        } catch (SQLException e) {
            LOGGER.atSevere().log("Failed to save all player data to database", e);
        }
    }
}
