package com.busybee.autopickup.util;

import ai.kodari.hylib.commons.message.Messenger;
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
        // Try to get a nicer name if possible
        try {
            if (itemStack.getItem() != null && itemStack.getItem().getTranslationProperties() != null) {
                String translatedName = itemStack.getItem().getTranslationProperties().getName();
                if (translatedName != null && !translatedName.isEmpty()) {
                    itemName = translatedName;
                }
            }
        } catch (Exception ignored) {}

        int quantity = itemStack.getQuantity();

        switch (notificationType.toUpperCase()) {
            case "NOTIFICATION":
                String pickupTitle = messages.getString("notifications.pickup-title", "<color:#22c55e>Item Picked Up");
                String pickupMessage = messages.getString("notifications.pickup-message", "<white>+{quantity} {item}")
                        .replace("{quantity}", String.valueOf(quantity))
                        .replace("{item}", itemName);
                
                Notifications.player(
                        playerRef,
                        createMessage(pickupTitle),
                        createMessage(pickupMessage),
                        null,
                        NotificationStyle.Success
                );
                break;

            case "TITLE":
                Titles.player(
                        playerRef,
                        createMessage("<color:#22c55e>+" + quantity),
                        createMessage("<white>" + itemName),
                        false
                );
                break;

            case "CHAT":
                String chatPrefix = messages.getString("chat.prefix", "<color:#22c55e>[AutoPickup]");
                String chatMessage = messages.getString("chat.pickup", "{prefix} <white>+{quantity} {item}")
                        .replace("{prefix}", chatPrefix)
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
                        createMessage(title),
                        createMessage(subtitle),
                        false
                    );
                    break;

                case "NOTIFICATION":
                    boolean isEnabled = title.toLowerCase().contains("enabled");
                    Notifications.player(
                            playerRef,
                            createMessage(title),
                            createMessage(subtitle),
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
                createMessage(title),
                createMessage(subtitle),
                false
            );
        } catch (Exception e) {
            AutoPickupPlugin.LOGGER.atWarning().log("Failed to send no-permission notification: " + e.getMessage());
            Messenger.sendMessage(playerRef, title + " " + subtitle);
        }
    }

    public static void sendSafeChat(PlayerRef playerRef, String message) {
        if (playerRef == null || !playerRef.isValid()) return;
        Messenger.sendMessage(playerRef, message);
    }

    private static Message createMessage(String input) {
        if (input == null) return Message.raw("");
        String text = extractText(input);
        String color = extractColor(input);
        return Message.raw(text).color(color);
    }

    private static String extractColor(String input) {
        if (input.contains("<color:#")) {
            int start = input.indexOf("#");
            int end = input.indexOf(">", start);
            if (start != -1 && end != -1) {
                return input.substring(start, end);
            }
        }
        
        if (input.contains("<white>")) return "#ffffff";
        if (input.contains("<red>")) return "#ff0000";
        if (input.contains("<green>")) return "#00ff00";
        if (input.contains("<blue>")) return "#0000ff";
        if (input.contains("<yellow>")) return "#ffff00";
        
        return "#ffffff"; // Default
    }

    private static String extractText(String input) {
        if (input == null) return "";
        // Remove color tags
        String text = input.replaceAll("<color:[^>]+>", "");
        text = text.replaceAll("<[^>]+>", "");
        return text.trim();
    }

    private static com.hypixel.hytale.server.core.entity.entities.Player getPlayer(PlayerRef playerRef) {
        if (playerRef == null || !playerRef.isValid()) return null;
        com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return null;
        com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> store = ref.getStore();
        if (store == null) return null;
        return store.getComponent(ref, com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
    }

}
