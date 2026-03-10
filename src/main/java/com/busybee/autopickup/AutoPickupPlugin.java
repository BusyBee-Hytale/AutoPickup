package com.busybee.autopickup;

import ai.kodari.hylib.commons.scheduler.Scheduler;
import ai.kodari.hylib.config.YamlConfig;
import com.busybee.autopickup.commands.AutoPickupCommand;
import com.busybee.autopickup.manager.PlayerDataManager;
import com.busybee.autopickup.systems.BreakBlockHandler;
import com.busybee.autopickup.systems.ItemInterceptorSystem;
import com.busybee.autopickup.systems.MobDropListener;
import com.busybee.autopickup.util.ConfigValidator;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.io.File;

public class AutoPickupPlugin extends JavaPlugin {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static AutoPickupPlugin instance;

    private YamlConfig config;
    private YamlConfig messages;
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
        YamlConfig.init(this);
        this.config = new YamlConfig("config.yml");
        this.messages = new YamlConfig("messages.yml");

        // Validate and repair configuration
        ConfigValidator validator = new ConfigValidator(this);
        validator.validateAndRepair();

        boolean verboseLogging = this.config.getBoolean("hstats.verbose-logging", false);
        new HStats("839433bf-1880-4752-84b8-64bda23d42ca", "2026.2.6", verboseLogging);
        this.playerDataManager = new PlayerDataManager(this);
        this.breakBlockHandler = new BreakBlockHandler();

        getEntityStoreRegistry().registerSystem(breakBlockHandler);
        getEntityStoreRegistry().registerSystem(new ItemInterceptorSystem(this));
        getEntityStoreRegistry().registerSystem(new MobDropListener(breakBlockHandler));

        AutoPickupCommand mainCommand = new AutoPickupCommand(this);
        mainCommand.addSubCommand(new AutoPickupCommand.ReloadSubCommand(this));
        mainCommand.addSubCommand(new AutoPickupCommand.SettingsSubCommand(this));
        getCommandRegistry().registerCommand(mainCommand);
    }

    @Override
    protected void start() {
    }

    @Override
    protected void shutdown() {
        if (playerDataManager != null) {
            playerDataManager.saveAllData();
        }

        if (breakBlockHandler != null) {
            breakBlockHandler.shutdown();
        }

        Scheduler.shutdown();
    }

    public YamlConfig getConfig() {
        return config;
    }
    public YamlConfig getMessages() {
        return messages;
    }
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    public BreakBlockHandler getBreakBlockHandler() {
        return breakBlockHandler;
    }

    public File getResourcesFolder() {
        return new File(System.getProperty("user.dir"), "mods/AutoPickup/data");
    }
}
