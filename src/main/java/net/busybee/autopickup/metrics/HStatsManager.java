package net.busybee.autopickup.metrics;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import net.busybee.autopickup.AutoPickupPlugin;

public class HStatsManager {
    public static final String HSTATS_HEADER = "HStats - Hytale Mod Metrics (hstats.dev)\n" +
            "HStats is a simple metrics system for Hytale mods. This file is here because one of your mods/plugins uses it, please do not modify the UUID.\n" +
            "HStats will apply little to no effect on your server and analytics are anonymous, however if you really want to you can disable it by setting enabled to false.\n" +
            "enabled=true\n";

    private final JavaPlugin plugin;
    private final HStats hstats;

    public HStatsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.hstats = new HStats("11ce0249-cbcb-41ef-996c-fa0e9d5c918e", plugin.getManifest().getVersion().toString());
    }

    public void init() {
        AutoPickupPlugin.LOGGER.atInfo().log("----------------------------------------------------------------------------");
        AutoPickupPlugin.LOGGER.atInfo().log("HStats - Hytale Mod Metrics (hstats.dev)");
        AutoPickupPlugin.LOGGER.atInfo().log("HStats is a simple metrics system for Hytale mods.");
        AutoPickupPlugin.LOGGER.atInfo().log("This plugin uses HStats to collect anonymous usage statistics.");
        AutoPickupPlugin.LOGGER.atInfo().log("No personal or identifying information is ever collected.");
        AutoPickupPlugin.LOGGER.atInfo().log("HStats will apply little to no effect on your server and analytics are anonymous.");
        AutoPickupPlugin.LOGGER.atInfo().log("To opt out, set 'enabled=false' in hstats-server-uuid.txt.");
        AutoPickupPlugin.LOGGER.atInfo().log("----------------------------------------------------------------------------");
    }

    public void shutdown() {
        if (hstats != null) {
            hstats.shutdown();
        }
    }
}
