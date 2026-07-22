package net.busybee.autopickup.metrics;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import dev.faststats.Metrics;
import dev.faststats.hytale.HytaleContext;

public class FastStatsManager {
    private final HytaleContext context;

    public FastStatsManager(JavaPlugin plugin) {
        this.context = new HytaleContext.Factory(plugin, "36b24cf22e42d43369945a522a7e4b71")
            .metrics(Metrics.Factory::create)
            .create();
    }

    public void init() {
        context.ready();
    }

    public void shutdown() {
        context.shutdown();
    }
}
