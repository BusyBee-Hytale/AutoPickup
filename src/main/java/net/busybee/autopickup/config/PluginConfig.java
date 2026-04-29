package net.busybee.autopickup.config;

import net.busybee.autopickup.AutoPickupPlugin;
import ai.kodari.hylib.config.YamlConfig;

import java.util.List;

public class PluginConfig {

    private final AutoPickupPlugin plugin;

    private long entryExpiryMs;
    private boolean treeDetectionEnabled;
    private List<String> treeBlocks;
    private int pickupRadius;
    private int treePickupRadius;
    private boolean disableInCreative;
    private String notificationType;
    private boolean defaultEnabled;
    private boolean whitelistEnabled;
    private boolean blacklistEnabled;
    private List<String> whitelist;
    private List<String> blacklist;

    private String dbType;
    private String dbHost;
    private int dbPort;
    private String dbName;
    private String dbUser;
    private String dbPassword;
    private int dbPoolSize;

    public PluginConfig(AutoPickupPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        YamlConfig config = plugin.getConfig();
        
        entryExpiryMs = config.getLong("autopickup.entry-expiry-ms", 5000L);
        treeDetectionEnabled = config.getBoolean("autopickup.tree-detection-enabled", true);
        treeBlocks = config.getStringList("autopickup.tree-blocks");
        pickupRadius = config.getInt("autopickup.pickup-radius", 3);
        treePickupRadius = config.getInt("autopickup.tree-pickup-radius", 5);
        disableInCreative = config.getBoolean("autopickup.disable-in-creative", true);
        notificationType = config.getString("autopickup.notification-type", "NOTIFICATION");
        defaultEnabled = config.getBoolean("autopickup.default-enabled", false);
        
        whitelistEnabled = config.getBoolean("autopickup.whitelist-enabled", false);
        blacklistEnabled = config.getBoolean("autopickup.blacklist-enabled", false);
        whitelist = config.getStringList("autopickup.whitelist");
        blacklist = config.getStringList("autopickup.blacklist");

        dbType = config.getString("database.type", "sqlite");
        dbHost = config.getString("database.host", "localhost");
        dbPort = config.getInt("database.port", 3306);
        dbName = config.getString("database.name", "autopickup");
        dbUser = config.getString("database.user", "root");
        dbPassword = config.getString("database.password", "");
        dbPoolSize = config.getInt("database.pool-size", 10);
    }

    public long getEntryExpiryMs() { return entryExpiryMs; }
    public boolean isTreeDetectionEnabled() { return treeDetectionEnabled; }
    public List<String> getTreeBlocks() { return treeBlocks; }
    public int getPickupRadius() { return pickupRadius; }
    public int getTreePickupRadius() { return treePickupRadius; }
    public boolean isDisableInCreative() { return disableInCreative; }
    public String getNotificationType() { return notificationType; }
    public boolean isDefaultEnabled() { return defaultEnabled; }
    public boolean isWhitelistEnabled() { return whitelistEnabled; }
    public boolean isBlacklistEnabled() { return blacklistEnabled; }
    public List<String> getWhitelist() { return whitelist; }
    public List<String> getBlacklist() { return blacklist; }
    
    public String getDbType() { return dbType; }
    public String getDbHost() { return dbHost; }
    public int getDbPort() { return dbPort; }
    public String getDbName() { return dbName; }
    public String getDbUser() { return dbUser; }
    public String getDbPassword() { return dbPassword; }
    public int getDbPoolSize() { return dbPoolSize; }
}
