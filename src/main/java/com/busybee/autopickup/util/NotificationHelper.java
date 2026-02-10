package com.busybee.autopickup.util;

import ai.kodari.hylib.commons.message.Messenger;
import ai.kodari.hylib.commons.util.ChatUtil;
import ai.kodari.hylib.commons.util.Notifications;
import ai.kodari.hylib.commons.util.Titles;
import ai.kodari.hylib.config.YamlConfig;
import com.busybee.autopickup.AutoPickupPlugin;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
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
                        Message.raw("Auto Pickup").color(extractColor(title)),
                        Message.raw(extractText(subtitle)).color("#ffffff"),
                        true
                    );
                    break;

                case "NOTIFICATION":
                    boolean isEnabled = title.contains("Enabled");
                    Notifications.player(
                            playerRef,
                            Message.raw("Auto Pickup").color(extractColor(title)),
                            Message.raw(extractText(subtitle)).color("#ffffff"),
                            null,
                            isEnabled ? NotificationStyle.Success : NotificationStyle.Warning
                    );
                    break;

                case "CHAT":
                    Messenger.sendMessage(playerRef, title + " - " + subtitle);
                    break;

                case "NONE":
                default:
                    break;
            }
        } catch (Exception e) {
            // Fallback to chat message if notification fails
            Messenger.sendMessage(playerRef, ChatUtil.parse(title) + " " + ChatUtil.parse(subtitle));
        }
    }

    private static String extractColor(String coloredText) {
        // Extract hex color from format like "<color:#22c55e>AutoPickup Enabled"
        if (coloredText.contains("<color:#")) {
            int start = coloredText.indexOf("#");
            int end = coloredText.indexOf(">", start);
            if (start != -1 && end != -1) {
                return coloredText.substring(start, end);
            }
        }
        return "#ffffff"; // default white
    }

    private static String extractText(String coloredText) {
        // Remove color tags from format like "<color:#22c55e>Text" or "<white>Text"
        String text = coloredText.replaceAll("<color:[^>]+>", "").replaceAll("<[^>]+>", "");
        return text.trim();
    }
}