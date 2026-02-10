package com.busybee.autopickup.manager;

import ai.kodari.hylib.config.YamlConfig;
import com.busybee.autopickup.AutoPickupPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final AutoPickupPlugin plugin;
    private final YamlConfig playerData;
    private final Map<UUID, Boolean> enabledCache;
    private final Map<UUID, Integer> radiusCache;

    public PlayerDataManager(AutoPickupPlugin plugin) {
        this.plugin = plugin;
        this.playerData = new YamlConfig("players.yml");
        this.enabledCache = new HashMap<>();
        this.radiusCache = new HashMap<>();
        loadAllData();
    }

    private void loadAllData() {
        if (playerData.contains("players")) {
            for (String uuidStr : playerData.getKeys("players")) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    boolean enabled = playerData.getBoolean("players." + uuidStr + ".autopickup",
                            plugin.getConfig().getBoolean("autopickup.default-enabled", false));
                    int radius = playerData.getInt("players." + uuidStr + ".radius",
                            plugin.getConfig().getInt("autopickup.default-radius", 5));
                    enabledCache.put(uuid, enabled);
                    radiusCache.put(uuid, radius);
                } catch (IllegalArgumentException e) {
                    AutoPickupPlugin.LOGGER.atWarning().log("Invalid UUID in players.yml: " + uuidStr);
                }
            }
        }
    }

    public boolean isAutoPickupEnabled(UUID playerUUID) {
        return enabledCache.computeIfAbsent(playerUUID, uuid ->
                plugin.getConfig().getBoolean("autopickup.default-enabled", false));
    }

    public void setAutoPickupEnabled(UUID playerUUID, boolean enabled) {
        enabledCache.put(playerUUID, enabled);
        playerData.set("players." + playerUUID.toString() + ".autopickup", enabled);
        playerData.save();
    }

    public int getRadius(UUID playerUUID) {
        return radiusCache.computeIfAbsent(playerUUID, uuid ->
                plugin.getConfig().getInt("autopickup.default-radius", 5));
    }

    public void setRadius(UUID playerUUID, int radius) {
        radiusCache.put(playerUUID, radius);
        playerData.set("players." + playerUUID.toString() + ".radius", radius);
        playerData.save();
    }

    public void saveAllData() {
        for (Map.Entry<UUID, Boolean> entry : enabledCache.entrySet()) {
            playerData.set("players." + entry.getKey().toString() + ".autopickup", entry.getValue());
        }
        for (Map.Entry<UUID, Integer> entry : radiusCache.entrySet()) {
            playerData.set("players." + entry.getKey().toString() + ".radius", entry.getValue());
        }
        playerData.save();
    }
}