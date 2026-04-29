package net.busybee.autopickup.database;

import net.busybee.autopickup.AutoPickupPlugin;
import net.busybee.autopickup.config.PluginConfig;
import net.busybee.autopickup.database.entity.PlayerData;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class DatabaseManager {

    private final AutoPickupPlugin plugin;
    private HikariDataSource dataSource;
    private ConnectionSource connectionSource;
    private Dao<PlayerData, String> playerDataDao;

    public DatabaseManager(AutoPickupPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        PluginConfig config = plugin.getPluginConfig();
        String type = config.getDbType().toLowerCase();

        try {
            HikariConfig hikariConfig = new HikariConfig();

            if (type.equals("mysql") || type.equals("mariadb")) {
                hikariConfig.setJdbcUrl("jdbc:mysql://" + config.getDbHost() + ":" + config.getDbPort() + "/" + config.getDbName() + "?useSSL=false&allowPublicKeyRetrieval=true");
                hikariConfig.setUsername(config.getDbUser());
                hikariConfig.setPassword(config.getDbPassword());

                hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
                hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
                hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
            } else {
                File dataFolder = plugin.getResourcesFolder();
                if (!dataFolder.exists()) {
                    dataFolder.mkdirs();
                }
                File dbFile = new File(dataFolder, "database.db");
                hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
                hikariConfig.setDriverClassName("org.sqlite.JDBC");
            }

            hikariConfig.setMinimumIdle(2);
            hikariConfig.setIdleTimeout(300000);
            hikariConfig.setMaxLifetime(600000);
            hikariConfig.setConnectionTimeout(10000);
            hikariConfig.setMaximumPoolSize(config.getDbPoolSize());
            hikariConfig.setPoolName("AutoPickup-Pool");

            dataSource = new HikariDataSource(hikariConfig);
            connectionSource = new DataSourceConnectionSource(dataSource, dataSource.getJdbcUrl());

            TableUtils.createTableIfNotExists(connectionSource, PlayerData.class);

            playerDataDao = DaoManager.createDao(connectionSource, PlayerData.class);

            AutoPickupPlugin.LOGGER.atInfo().log("Database connection established (" + type + ").");
            return true;
        } catch (Exception e) {
            AutoPickupPlugin.LOGGER.atWarning().log("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void shutdown() {
        try {
            if (connectionSource != null) {
                connectionSource.close();
            }
            if (dataSource != null) {
                dataSource.close();
            }
            AutoPickupPlugin.LOGGER.atInfo().log("Database connection closed.");
        } catch (Exception e) {
            AutoPickupPlugin.LOGGER.atWarning().log("Error closing database connection: " + e.getMessage());
        }
    }

    public Dao<PlayerData, String> getPlayerDataDao() {
        return playerDataDao;
    }

    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier);
    }

    public CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable);
    }
}
