package com.busybee.autopickup.commands;

import ai.kodari.hylib.commons.message.Messenger;
import com.busybee.autopickup.AutoPickupPlugin;
import com.busybee.autopickup.util.ConfigValidator;
import com.busybee.autopickup.util.Permissions;
import com.busybee.autopickup.util.NotificationHelper;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class AutoPickupCommand extends AbstractPlayerCommand {

    private final AutoPickupPlugin plugin;

    public AutoPickupCommand(AutoPickupPlugin plugin) {
        super("autopickup", "Toggle AutoPickup or manage settings");
        this.plugin = plugin;
    }

    @Override
    protected void execute(
            @Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        if (!Permissions.canToggle(playerRef)) {
            NotificationHelper.sendNoPermissionNotification(
                playerRef,
                plugin.getMessages().getString("titles.no-permission", "<color:#ff0000>No Permission"),
                plugin.getMessages().getString("titles.no-permission-subtitle", "<white>You don't have access to toggle auto-pickup")
            );
            return;
        }

        if (!plugin.getConfig().getBoolean("autopickup.enabled", true)) {
            NotificationHelper.sendNoPermissionNotification(
                playerRef,
                plugin.getMessages().getString("titles.plugin-disabled", "<color:#ff0000>AutoPickup Disabled"),
                plugin.getMessages().getString("titles.plugin-disabled-subtitle", "<white>Plugin is disabled by administrator")
            );
            return;
        }

        boolean currentStatus = plugin.getPlayerDataManager().isAutoPickupEnabled(playerRef.getUuid());
        boolean newStatus = !currentStatus;
        plugin.getPlayerDataManager().setAutoPickupEnabled(playerRef.getUuid(), newStatus);

        String notificationType = plugin.getConfig().getString("autopickup.toggle-notification-type", "TITLE");

        if (newStatus) {
            NotificationHelper.sendToggleNotification(
                playerRef,
                notificationType,
                plugin.getMessages().getString("titles.enabled", "<color:#22c55e>AutoPickup Enabled"),
                plugin.getMessages().getString("titles.enabled-subtitle", "<white>Items will be automatically picked up")
            );
        } else {
            NotificationHelper.sendToggleNotification(
                playerRef,
                notificationType,
                plugin.getMessages().getString("titles.disabled", "<color:#ff0000>AutoPickup Disabled"),
                plugin.getMessages().getString("titles.disabled-subtitle", "<white>Items will drop normally")
            );
        }
    }

    public static class ReloadSubCommand extends AbstractPlayerCommand {

        private final AutoPickupPlugin plugin;

        public ReloadSubCommand(AutoPickupPlugin plugin) {
            super("reload", "Reload configuration files");
            this.plugin = plugin;
        }

        @Override
        protected void execute(
                @Nonnull CommandContext ctx,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            if (!Permissions.canReload(playerRef)) {
                NotificationHelper.sendNoPermissionNotification(
                    playerRef,
                    plugin.getMessages().getString("titles.no-permission", "<color:#ff0000>No Permission"),
                    plugin.getMessages().getString("titles.no-permission-reload-subtitle", "<white>You don't have access to reload the config")
                );
                return;
            }

            plugin.getConfig().reload();
            plugin.getMessages().reload();

            new ConfigValidator(plugin).validateAndRepair();
            
            AutoPickupPlugin.LOGGER.atInfo().log("Configuration and messages reloaded.");

            String prefix = plugin.getMessages().getString("chat.prefix", "<color:#22c55e>[AutoPickup]");
            String successMessage = plugin.getMessages().getString("chat.reload-success", "{prefix} <white>Configuration and messages reloaded successfully!")
                    .replace("{prefix}", prefix);
            
            Messenger.sendMessage(playerRef, successMessage);
        }
    }
}
