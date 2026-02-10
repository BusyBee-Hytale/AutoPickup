package com.busybee.autopickup.manager;

import ai.kodari.hylib.config.YamlConfig;
import com.busybee.autopickup.AutoPickupPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final AutoPickupPlugin plugin;
    private final YamlConfig playerData;
    private final Map<UUID, Boolean> cache;

    public PlayerDataManager(AutoPickupPlugin plugin) {
        this.plugin = plugin;
        this.playerData = new YamlConfig("players.yml");
        this.cache = new HashMap<>();
        loadAllData();
    }

    private void loadAllData() {
        if (playerData.contains("players")) {
            for (String uuidStr : playerData.getKeys("players")) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    boolean enabled = playerData.getBoolean("players." + uuidStr + ".autopickup",
                            plugin.getConfig().getBoolean("autopickup.default-enabled", false));
                    cache.put(uuid, enabled);
                } catch (IllegalArgumentException e) {
                    AutoPickupPlugin.LOGGER.atWarning().log("Invalid UUID in players.yml: " + uuidStr);
                }
            }
        }
    }

    public boolean isAutoPickupEnabled(UUID playerUUID) {
        return cache.computeIfAbsent(playerUUID, uuid ->
                plugin.getConfig().getBoolean("autopickup.default-enabled", false));
    }

    public void setAutoPickupEnabled(UUID playerUUID, boolean enabled) {
        cache.put(playerUUID, enabled);
        playerData.set("players." + playerUUID.toString() + ".autopickup", enabled);
        playerData.save();
    }

    public void saveAllData() {
        for (Map.Entry<UUID, Boolean> entry : cache.entrySet()) {
            playerData.set("players." + entry.getKey().toString() + ".autopickup", entry.getValue());
        }
        playerData.save();
    }
}