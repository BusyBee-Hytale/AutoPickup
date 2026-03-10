package com.busybee.autopickup.util;

import com.hypixel.hytale.server.core.permissions.PermissionHolder;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.UUID;

public final class Permissions {

    public static final String TOGGLE = "autopickup.toggle";
    public static final String RELOAD = "autopickup.reload";
    public static final String SETTINGS = "autopickup.settings";

    private Permissions() {}

    public static boolean hasPermission(PermissionHolder holder, String permission, boolean defaultValue) {
        if (holder == null) {
            return defaultValue;
        }
        if (holder instanceof com.hypixel.hytale.server.core.command.system.CommandSender sender) {
            UUID uuid = sender.getUuid();
            if (uuid != null) {
                return hasPermission(uuid, permission, defaultValue);
            }
        }
        return holder.hasPermission(permission, defaultValue);
    }

    public static boolean hasPermission(PlayerRef playerRef, String permission, boolean defaultValue) {
        return hasPermission(playerRef.getUuid(), permission, defaultValue);
    }

    public static boolean hasPermission(UUID uuid, String permission, boolean defaultValue) {
        return PermissionsModule.get().hasPermission(uuid, permission, defaultValue);
    }

    public static boolean canToggle(PermissionHolder holder) {
        return hasPermission(holder, TOGGLE, true);
    }

    public static boolean canToggle(PlayerRef playerRef) {
        return hasPermission(playerRef.getUuid(), TOGGLE, true);
    }

    public static boolean canReload(PermissionHolder holder) {
        return hasPermission(holder, RELOAD, false);
    }

    public static boolean canReload(PlayerRef playerRef) {
        return hasPermission(playerRef.getUuid(), RELOAD, false);
    }

    public static boolean canViewSettings(PermissionHolder holder) {
        return hasPermission(holder, SETTINGS, true);
    }

    public static boolean canViewSettings(PlayerRef playerRef) {
        return hasPermission(playerRef.getUuid(), SETTINGS, true);
    }
}
