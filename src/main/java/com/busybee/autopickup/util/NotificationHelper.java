package com.busybee.autopickup.util;

import ai.kodari.hylib.commons.message.Messenger;
import ai.kodari.hylib.commons.util.ChatUtil;
import ai.kodari.hylib.commons.util.Notifications;
import ai.kodari.hylib.commons.util.Titles;
import ai.kodari.hylib.config.YamlConfig;
import com.busybee.autopickup.AutoPickupPlugin;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class NotificationHelper {

    public static void sendPickupNotification(PlayerRef playerRef, String notificationType, ItemStack itemStack) {
        AutoPickupPlugin plugin = AutoPickupPlugin.getInstance();
        YamlConfig messages = plugin.getMessages();

        String itemName = itemStack.getItemId();
        int quantity = itemStack.getQuantity();

        String pickupMessage = messages.getString("notifications.pickup-message", "<white>+{quantity} {item}")
                .replace("{quantity}", String.valueOf(quantity))
                .replace("{item}", itemName);

        switch (notificationType.toUpperCase()) {
            case "NOTIFICATION":
                Notifications.player(
                        playerRef,
                        ChatUtil.parse(messages.getString("notifications.pickup-title", "<color:#22c55e>Item Picked Up")),
                        ChatUtil.parse(pickupMessage),
                        null,
                        NotificationStyle.Success
                );
                break;

            case "TITLE":
                Titles.player(
                        playerRef,
                        ChatUtil.parse("<color:#22c55e>+" + quantity),
                        ChatUtil.parse("<white>" + itemName),
                        false
                );
                break;

            case "CHAT":
                String chatMessage = messages.getString("chat.pickup", "{prefix} <white>+{quantity} {item}")
                        .replace("{prefix}", messages.getString("chat.prefix", "<color:#22c55e>[AutoPickup]"))
                        .replace("{quantity}", String.valueOf(quantity))
                        .replace("{item}", itemName);
                Messenger.sendMessage(playerRef, chatMessage);
                break;

            case "NONE":
            default:
                break;
        }
    }

    public static void sendToggleNotification(PlayerRef playerRef, String notificationType, String title, String subtitle) {
        try {
            switch (notificationType.toUpperCase()) {
                case "TITLE":
                    Titles.player(
                        playerRef,
                        ChatUtil.parse(title),
                        ChatUtil.parse(subtitle),
                        false
                    );
                    break;

                case "NOTIFICATION":
                    boolean isEnabled = title.contains("Enabled");
                    Notifications.player(
                            playerRef,
                            ChatUtil.parse(title),
                            ChatUtil.parse(subtitle),
                            null,
                            isEnabled ? NotificationStyle.Success : NotificationStyle.Warning
                    );
                    break;

                case "CHAT":
                    AutoPickupPlugin plugin = AutoPickupPlugin.getInstance();
                    String prefix = plugin.getMessages().getString("chat.prefix", "<color:#22c55e>[AutoPickup]");
                    Messenger.sendMessage(playerRef, prefix + " " + title);
                    break;

                case "NONE":
                default:
                    break;
            }
        } catch (Exception e) {
            AutoPickupPlugin.LOGGER.atWarning().log("Failed to send toggle notification: " + e.getMessage());
            Messenger.sendMessage(playerRef, title + " " + subtitle);
        }
    }

    public static void sendNoPermissionNotification(PlayerRef playerRef, String title, String subtitle) {
        try {
            Titles.player(
                playerRef,
                ChatUtil.parse(title),
                ChatUtil.parse(subtitle),
                false
            );
        } catch (Exception e) {
            AutoPickupPlugin.LOGGER.atWarning().log("Failed to send no-permission notification: " + e.getMessage());
            Messenger.sendMessage(playerRef, title + " " + subtitle);
        }
    }

}
