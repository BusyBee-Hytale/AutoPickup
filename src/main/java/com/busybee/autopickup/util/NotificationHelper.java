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
            boolean isEnabled = title.contains("Enabled");
            String cleanTitle = extractText(title);
            String cleanSubtitle = extractText(subtitle);
            String titleColor = extractColor(title);

            switch (notificationType.toUpperCase()) {
                case "TITLE":
                    // Use Message.raw() and .color() like ChestCollector does
                    Titles.player(
                        playerRef,
                        Message.raw(cleanTitle).color(titleColor),
                        Message.raw(cleanSubtitle).color("#ffffff"),
                        false
                    );
                    break;

                case "NOTIFICATION":
                    // Use Message.raw() and .color() for notifications
                    Notifications.player(
                            playerRef,
                            Message.raw(cleanTitle).color(titleColor),
                            Message.raw(cleanSubtitle).color("#ffffff"),
                            null,
                            isEnabled ? NotificationStyle.Success : NotificationStyle.Warning
                    );
                    break;

                case "CHAT":
                    AutoPickupPlugin plugin = AutoPickupPlugin.getInstance();
                    String prefix = plugin.getMessages().getString("chat.prefix", "<color:#22c55e>[AutoPickup]");
                    Messenger.sendMessage(playerRef, prefix + " " + cleanTitle);
                    break;

                case "NONE":
                default:
                    break;
            }
        } catch (Exception e) {
            AutoPickupPlugin.LOGGER.atWarning().log("Failed to send toggle notification: " + e.getMessage());
            // Fallback to chat message if notification fails
            Messenger.sendMessage(playerRef, title + " " + subtitle);
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

        // Handle named colors like <white>, <red>, <green>, etc.
        if (coloredText.contains("<white>")) return "#ffffff";
        if (coloredText.contains("<red>")) return "#ff0000";
        if (coloredText.contains("<green>")) return "#00ff00";
        if (coloredText.contains("<blue>")) return "#0000ff";
        if (coloredText.contains("<yellow>")) return "#ffff00";
        if (coloredText.contains("<gray>") || coloredText.contains("<grey>")) return "#808080";
        if (coloredText.contains("<black>")) return "#000000";

        return "#ffffff"; // default white
    }

    private static String extractText(String coloredText) {
        // Remove all color tags from format like "<color:#22c55e>Text" or "<white>Text"
        String text = coloredText.replaceAll("<color:[^>]+>", "").replaceAll("<[^>]+>", "");
        return text.trim();
    }
}