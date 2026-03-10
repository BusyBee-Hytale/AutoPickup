package com.busybee.autopickup.util;

import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.UUID;

public final class Permissions {

    public static final String TOGGLE = "autopickup.toggle";
    public static final String RELOAD = "autopickup.reload";
    public static final String SETTINGS = "autopickup.settings";

    private Permissions() {}

    public static boolean hasPermission(PlayerRef playerRef, String permission, boolean defaultValue) {
        return hasPermission(playerRef.getUuid(), permission, defaultValue);
    }

    public static boolean hasPermission(UUID uuid, String permission, boolean defaultValue) {
        return PermissionsModule.get().hasPermission(uuid, permission, defaultValue);
    }

    public static boolean canToggle(PlayerRef playerRef) {
        return hasPermission(playerRef.getUuid(), TOGGLE, true);
    }

    public static boolean canReload(PlayerRef playerRef) {
        return hasPermission(playerRef.getUuid(), RELOAD, false);
    }

    public static boolean canViewSettings(PlayerRef playerRef) {
        return hasPermission(playerRef.getUuid(), SETTINGS, true);
    }
}
