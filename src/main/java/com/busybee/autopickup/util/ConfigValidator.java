package com.busybee.autopickup.util;

import ai.kodari.hylib.config.YamlConfig;
import com.busybee.autopickup.AutoPickupPlugin;

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
        validateIntRange(config, "autopickup.entry-expiry-ms", 3000L, 100L, 30000L);
        validateIntRange(config, "autopickup.pickup-radius", 3, 1, 20);
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
        validateList(config, "autopickup.tree-blocks");

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
            AutoPickupPlugin.LOGGER.atWarning().log("Repairing with default value: " + defaultValue);
            config.set(path, defaultValue);
            hasWarnings = true;
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
            AutoPickupPlugin.LOGGER.atWarning().log("Value for '" + path + "' (" + numValue + ") is out of range [" + min + ", " + max + "]");
            AutoPickupPlugin.LOGGER.atWarning().log("Repairing with default value: " + defaultValue);
            config.set(path, defaultValue);
            hasWarnings = true;
        }
    }

    private void validateEnum(YamlConfig config, String path, String defaultValue, List<String> validValues) {
        String value = config.getString(path, defaultValue);
        if (!validValues.contains(value)) {
            AutoPickupPlugin.LOGGER.atWarning().log("Invalid value for '" + path + "': " + value);
            AutoPickupPlugin.LOGGER.atWarning().log("Valid values are: " + validValues);
            AutoPickupPlugin.LOGGER.atWarning().log("Repairing with default value: " + defaultValue);
            config.set(path, defaultValue);
            hasWarnings = true;
        }
    }

    private void validateList(YamlConfig config, String path) {
        Object value = config.get(path);
        if (value != null && !(value instanceof List)) {
            AutoPickupPlugin.LOGGER.atWarning().log("Invalid list value for '" + path + "': " + value);
            AutoPickupPlugin.LOGGER.atWarning().log("Repairing with empty list");
            config.set(path, Arrays.asList());
            hasWarnings = true;
        }
    }
}
