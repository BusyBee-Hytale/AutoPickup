package com.busybee.autopickup.manager;

import ai.kodari.hylib.config.YamlConfig;
import com.busybee.autopickup.AutoPickupPlugin;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

import static com.busybee.autopickup.AutoPickupPlugin.LOGGER;

public class PlayerDataManager {

    private final AutoPickupPlugin plugin;
    private final YamlConfig dataConfig;
    private final Map<UUID, Boolean> enabledCache;

    public PlayerDataManager(AutoPickupPlugin plugin) {
        this.plugin = plugin;
        this.dataConfig = new YamlConfig("data.yml");
        this.enabledCache = new ConcurrentHashMap<>();
        loadAllData();
    }

    private void loadAllData() {
        if (dataConfig.contains("players")) {
            Set<String> playerKeys = dataConfig.getKeys("players");
            if (playerKeys != null) {
                int count = 0;
                for (String playerKey : playerKeys) {
                    try {
                        UUID uuid = UUID.fromString(playerKey);
                        boolean enabled = dataConfig.getBoolean("players." + playerKey + ".autopickup-enabled", false);
                        enabledCache.put(uuid, enabled);
                        count++;
                    } catch (IllegalArgumentException e) {
                        LOGGER.atWarning().log("Invalid UUID in data.yml: " + playerKey);
                    }
                }
                LOGGER.atInfo().log("Loaded " + count + " player settings from data.yml");
            }
        }
    }

    public boolean isAutoPickupEnabled(UUID playerUUID) {
        return enabledCache.computeIfAbsent(playerUUID, uuid -> {
            String path = "players." + uuid.toString() + ".autopickup-enabled";
            if (dataConfig.contains(path)) {
                return dataConfig.getBoolean(path, false);
            }
            return plugin.getConfig().getBoolean("autopickup.default-enabled", false);
        });
    }

    public void setAutoPickupEnabled(UUID playerUUID, boolean enabled) {
        enabledCache.put(playerUUID, enabled);
        savePlayerSettings(playerUUID, enabled);
    }

    private void savePlayerSettings(UUID playerUUID, boolean enabled) {
        String uuidString = playerUUID.toString();
        dataConfig.set("players." + uuidString + ".name", uuidString);
        dataConfig.set("players." + uuidString + ".autopickup-enabled", enabled);
        dataConfig.save();
    }

    public void saveAllData() {
        for (Map.Entry<UUID, Boolean> entry : enabledCache.entrySet()) {
            UUID uuid = entry.getKey();
            boolean enabled = entry.getValue();
            String uuidString = uuid.toString();

            dataConfig.set("players." + uuidString + ".name", uuidString);
            dataConfig.set("players." + uuidString + ".autopickup-enabled", enabled);
        }
        dataConfig.save();
        LOGGER.atInfo().log("Saved " + enabledCache.size() + " player settings to data.yml");
    }
}
