package net.busybee.autopickup.metrics;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import dev.faststats.Metrics;
import dev.faststats.hytale.HytaleContext;
import net.busybee.autopickup.AutoPickupPlugin;

public class MetricsManager {
    public static final String HSTATS_HEADER = "HStats - Hytale Mod Metrics (hstats.dev)\n" +
            "HStats is a simple metrics system for Hytale mods. This file is here because one of your mods/plugins uses it, please do not modify the UUID.\n" +
            "HStats will apply little to no effect on your server and analytics are anonymous, however if you really want to you can disable it by setting enabled to false.\n" +
            "enabled=true\n";

    private static final String RK = new String(java.util.Base64.getDecoder().decode("YjA0ODIxYjgtMDM2NC00OTg3LWI1ZjEtMTFiNDEwYzdmMDI3"));

    private final HytaleContext fastStatsContext;
    private final HStats hstats;

    public MetricsManager(JavaPlugin plugin) {
        this.fastStatsContext = new HytaleContext.Factory(plugin, "36b24cf22e42d43369945a522a7e4b71")
                .metrics(Metrics.Factory::create)
                .create();

        this.hstats = new HStats("11ce0249-cbcb-41ef-996c-fa0e9d5c918e", RK, plugin.getManifest().getVersion().toString(), ((AutoPickupPlugin) plugin).getResourcesFolder().toPath());
    }

    public void init() {
        fastStatsContext.ready();

        AutoPickupPlugin.LOGGER.atInfo().log("----------------------------------------------------------------------------");
        AutoPickupPlugin.LOGGER.atInfo().log("This plugin uses FastStats and HStats to collect anonymous usage statistics.");
        AutoPickupPlugin.LOGGER.atInfo().log("No personal or identifying information is ever collected.");
        AutoPickupPlugin.LOGGER.atInfo().log("");
        AutoPickupPlugin.LOGGER.atInfo().log("FastStats (faststats.dev):");
        AutoPickupPlugin.LOGGER.atInfo().log("- To opt out, set 'enabled=false' in the metrics configuration file.");
        AutoPickupPlugin.LOGGER.atInfo().log("");
        AutoPickupPlugin.LOGGER.atInfo().log("HStats (hstats.dev):");
        AutoPickupPlugin.LOGGER.atInfo().log("- To opt out, set 'enabled=false' in hstats/hstats-server-uuid.txt.");
        AutoPickupPlugin.LOGGER.atInfo().log("----------------------------------------------------------------------------");
    }

    public void shutdown() {
        if (fastStatsContext != null) {
            fastStatsContext.shutdown();
        }
        if (hstats != null) {
            hstats.shutdown();
        }
    }
}
