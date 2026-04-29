package net.busybee.autopickup.manager;

import ai.kodari.hylib.config.YamlConfig;
import net.busybee.autopickup.AutoPickupPlugin;
import net.busybee.autopickup.database.entity.PlayerData;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.busybee.autopickup.AutoPickupPlugin.LOGGER;

public class PlayerDataManager {

    private final AutoPickupPlugin plugin;
    private final Map<UUID, Boolean> enabledCache;

    public PlayerDataManager(AutoPickupPlugin plugin) {
        this.plugin = plugin;
        this.enabledCache = new ConcurrentHashMap<>();

        loadFromDatabase();

        checkMigration();
    }

    private void loadFromDatabase() {
        Dao<PlayerData, String> dao = plugin.getDatabaseManager().getPlayerDataDao();
        if (dao == null) return;

        try {
            for (PlayerData data : dao.queryForAll()) {
                enabledCache.put(data.getUuid(), data.isAutoPickupEnabled());
            }
            LOGGER.atInfo().log("Loaded " + enabledCache.size() + " player settings from database.");
        } catch (SQLException e) {
            LOGGER.atWarning().log("Failed to load player data from database: " + e.getMessage());
        }
    }

    private void checkMigration() {
        YamlConfig dataConfig = new YamlConfig("data.yml");
        if (!dataConfig.contains("players")) return;

        Set<String> playerKeys = dataConfig.getKeys("players");
        if (playerKeys == null || playerKeys.isEmpty()) return;

        LOGGER.atInfo().log("Found legacy data.yml, starting migration...");
        int count = 0;
        for (String playerKey : playerKeys) {
            try {
                UUID uuid = UUID.fromString(playerKey);
                if (enabledCache.containsKey(uuid)) continue; // Already in DB

                boolean enabled = dataConfig.getBoolean("players." + playerKey + ".autopickup-enabled", false);
                setAutoPickupEnabled(uuid, enabled);
                count++;
            } catch (IllegalArgumentException e) {
                LOGGER.atWarning().log("Invalid UUID in legacy data.yml: " + playerKey);
            }
        }
        LOGGER.atInfo().log("Migrated " + count + " player settings from data.yml to database.");
        
        // Optionally rename data.yml to prevent re-migration
        // dataConfig.getFile().renameTo(new File(dataConfig.getFile().getParent(), "data.yml.bak"));
        // Future update once all legacy data is migrated, a warning about this will go out.
        LOGGER.atWarning().log("Legacy data migration complete. Please remove data.yml to prevent re-migration in future updates.");
    }

    public boolean isAutoPickupEnabled(UUID playerUUID) {
        return enabledCache.computeIfAbsent(playerUUID, uuid -> {
            try {
                PlayerData data = plugin.getDatabaseManager().getPlayerDataDao().queryForId(uuid.toString());
                if (data != null) {
                    return data.isAutoPickupEnabled();
                }
            } catch (SQLException e) {
                LOGGER.atWarning().log("Error fetching player data for " + uuid + ": " + e.getMessage());
            }
            return plugin.getPluginConfig().isDefaultEnabled();
        });
    }

    public void setAutoPickupEnabled(UUID playerUUID, boolean enabled) {
        enabledCache.put(playerUUID, enabled);
        savePlayerSettingsAsync(playerUUID, enabled);
    }

    private void savePlayerSettingsAsync(UUID playerUUID, boolean enabled) {
        plugin.getDatabaseManager().runAsync(() -> {
            try {
                PlayerData data = new PlayerData(playerUUID, enabled, playerUUID.toString());
                plugin.getDatabaseManager().getPlayerDataDao().createOrUpdate(data);
            } catch (SQLException e) {
                LOGGER.atWarning().log("Failed to save player settings for " + playerUUID + ": " + e.getMessage());
            }
        });
    }

    public void saveAllData() {
        LOGGER.atInfo().log("Player data saved to database.");
    }
}
