package com.busybee.autopickup;

import ai.kodari.hylib.commons.scheduler.Scheduler;
import ai.kodari.hylib.config.YamlConfig;
import com.busybee.autopickup.commands.AutoPickupCommand;
import com.busybee.autopickup.manager.PlayerDataManager;
import com.busybee.autopickup.systems.BreakBlockHandler;
import com.busybee.autopickup.systems.DropItemHandler;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;

public class AutoPickupPlugin extends JavaPlugin {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static AutoPickupPlugin instance;

    private YamlConfig config;
    private PlayerDataManager playerDataManager;
    private BreakBlockHandler breakBlockHandler;

    public AutoPickupPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static AutoPickupPlugin getInstance() {
        return instance;
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up AutoPickup plugin...");

        YamlConfig.init(this);
        this.config = new YamlConfig("config.yml");

        this.playerDataManager = new PlayerDataManager(this);

        this.breakBlockHandler = new BreakBlockHandler();

        getEntityStoreRegistry().registerSystem(breakBlockHandler);
        getEntityStoreRegistry().registerSystem(new DropItemHandler(this));

        getCommandRegistry().registerCommand(new AutoPickupCommand(this));

        LOGGER.atInfo().log("AutoPickup plugin setup complete!");
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("AutoPickup plugin started!");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("AutoPickup plugin shutting down...");
        playerDataManager.saveAllData();
        Scheduler.shutdown();
    }

    public YamlConfig getConfig() {
        return config;
    }
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    public BreakBlockHandler getBreakBlockHandler() {
        return breakBlockHandler;
    }
}
