package com.busybee.autopickup.util;

import ai.kodari.hylib.config.YamlConfig;
import com.busybee.autopickup.AutoPickupPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigValidator {

    private final AutoPickupPlugin plugin;
    private boolean hasWarnings = false;

    public ConfigValidator(AutoPickupPlugin plugin) {
        this.plugin = plugin;
    }

    public void validateAndRepair() {
        YamlConfig config = plugin.getConfig();
        
        AutoPickupPlugin.LOGGER.atInfo().log("Validating configuration...");

        // Validate boolean values
        validateBoolean(config, "autopickup.enabled", true);
        validateBoolean(config, "autopickup.default-enabled", true);
        validateBoolean(config, "autopickup.disable-in-creative", true);
        validateBoolean(config, "autopickup.tree-detection-enabled", true);
        validateBoolean(config, "autopickup.whitelist-enabled", false);
        validateBoolean(config, "autopickup.blacklist-enabled", false);
        validateBoolean(config, "hstats.verbose-logging", false);

        // Validate integer ranges
        validateIntRange(config, "autopickup.entry-expiry-ms", 5000L, 100L, 30000L);
        validateIntRange(config, "autopickup.pickup-radius", 5, 1, 20);
        validateIntRange(config, "autopickup.tree-pickup-radius", 15, 1, 50);
        validateIntRange(config, "autopickup.pickup-delay-ticks", 0, 0, 100);

        // Validate notification type enums
        validateEnum(config, "autopickup.notification-type", "NOTIFICATION", 
            Arrays.asList("TITLE", "NOTIFICATION", "CHAT", "NONE"));
        validateEnum(config, "autopickup.toggle-notification-type", "TITLE",
            Arrays.asList("TITLE", "NOTIFICATION", "CHAT", "NONE"));

        // Validate list configurations exist
        validateList(config, "autopickup.whitelist");
        validateList(config, "autopickup.blacklist");
        validateTreeBlocks(config);

        // Validate whitelist/blacklist mutual exclusivity
        boolean whitelistEnabled = config.getBoolean("autopickup.whitelist-enabled", false);
        boolean blacklistEnabled = config.getBoolean("autopickup.blacklist-enabled", false);
        if (whitelistEnabled && blacklistEnabled) {
            AutoPickupPlugin.LOGGER.atWarning().log("Both whitelist and blacklist are enabled! Whitelist will take priority.");
            AutoPickupPlugin.LOGGER.atWarning().log("Disabling blacklist to prevent conflicts...");
            config.set("autopickup.blacklist-enabled", false);
            hasWarnings = true;
        }

        if (hasWarnings) {
            AutoPickupPlugin.LOGGER.atWarning().log("Configuration validation found issues. Some values were auto-repaired.");
            AutoPickupPlugin.LOGGER.atWarning().log("Please review your config.yml file.");
        } else {
            AutoPickupPlugin.LOGGER.atInfo().log("Configuration validation completed successfully.");
        }
    }

    private void validateBoolean(YamlConfig config, String path, boolean defaultValue) {
        Object value = config.get(path);
        if (value != null && !(value instanceof Boolean)) {
            AutoPickupPlugin.LOGGER.atWarning().log("Invalid boolean value for '" + path + "': " + value);
            // Just warn, don't repair to avoid accidental loss of settings if YAML parsing was slightly off
        }
    }

    private void validateIntRange(YamlConfig config, String path, long defaultValue, long min, long max) {
        Object value = config.get(path);
        long numValue;

        if (value instanceof Number) {
            numValue = ((Number) value).longValue();
        } else {
            AutoPickupPlugin.LOGGER.atWarning().log("Invalid numeric value for '" + path + "': " + value);
            AutoPickupPlugin.LOGGER.atWarning().log("Repairing with default value: " + defaultValue);
            config.set(path, defaultValue);
            hasWarnings = true;
            return;
        }

        if (numValue < min || numValue > max) {
            AutoPickupPlugin.LOGGER.atWarning().log("Value for '" + path + "' (" + numValue + ") is outside recommended range [" + min + ", " + max + "]");
            // We won't auto-repair this anymore to avoid losing user settings, 
            // unless it's dangerously high (which we'll handle by just capping it in memory if needed, but for now we'll trust the user)
        }
    }

    private void validateEnum(YamlConfig config, String path, String defaultValue, List<String> validValues) {
        String value = config.getString(path, defaultValue);
        if (!validValues.contains(value)) {
            AutoPickupPlugin.LOGGER.atWarning().log("Invalid value for '" + path + "': " + value);
            AutoPickupPlugin.LOGGER.atWarning().log("Valid values are: " + validValues);
            // Just warn, don't auto-repair to avoid losing user choices that might be added by newer versions
        }
    }

    private void validateList(YamlConfig config, String path) {
        Object value = config.get(path);
        if (value != null && !(value instanceof List)) {
            AutoPickupPlugin.LOGGER.atWarning().log("Invalid list value for '" + path + "': " + value);
            // Just warn, don't auto-repair
        }
    }

    private void validateTreeBlocks(YamlConfig config) {
        String path = "autopickup.tree-blocks";
        Object value = config.get(path);
        
        List<String> requiredNewBlocks = Arrays.asList("Willow", "Cherry", "Redwood");
        
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> currentList = new ArrayList<>((List<Object>) value);
            boolean added = false;
            
            for (String newBlock : requiredNewBlocks) {
                boolean exists = false;
                for (Object item : currentList) {
                    if (item != null && item.toString().equalsIgnoreCase(newBlock)) {
                        exists = true;
                        break;
                    }
                }
                
                if (!exists) {
                    currentList.add(newBlock);
                    added = true;
                }
            }
            
            if (added) {
                config.set(path, currentList);
                hasWarnings = true;
                AutoPickupPlugin.LOGGER.atInfo().log("Added missing tree types to configuration: Willow, Cherry, Redwood");
            }
        } else if (value == null) {
            List<String> defaultBlocks = new ArrayList<>(Arrays.asList(
                    "Log", "Leaf", "Leaves", "Branch", "Trunk", "Stump", "Wood", "Sapling",
                    "Seed", "Fruit", "Apple", "Oak", "Birch", "Willow", "Spruce", "Pine",
                    "Redwood", "Palm", "Acacia", "Thorntree", "Bamboo", "Cherry",
                    "Frostwood", "Deadwood"
            ));
            config.set(path, defaultBlocks);
            hasWarnings = true;
            AutoPickupPlugin.LOGGER.atInfo().log("Created default tree-blocks configuration.");
        }
    }
}
