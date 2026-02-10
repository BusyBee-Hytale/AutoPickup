package com.busybee.autopickup.util;

import ai.kodari.hylib.commons.message.Messenger;
import ai.kodari.hylib.commons.util.ChatUtil;
import ai.kodari.hylib.commons.util.Notifications;
import ai.kodari.hylib.commons.util.Titles;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class NotificationHelper {

    public static void sendPickupNotification(PlayerRef playerRef, String notificationType, ItemStack itemStack) {
        String itemName = itemStack.getItemId();
        int quantity = itemStack.getQuantity();

        String message = "+" + quantity + " " + itemName;

        switch (notificationType.toUpperCase()) {
            case "NOTIFICATION":
                Notifications.player(
                        playerRef,
                        ChatUtil.parse("<color:#22c55e>Item Picked Up"),
                        ChatUtil.parse("<white>" + message),
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
                Messenger.sendMessage(playerRef, "<color:#22c55e>[AutoPickup] <white>" + message);
                break;

            case "NONE":
            default:
                break;
        }
    }

    public static void sendToggleNotification(PlayerRef playerRef, String notificationType, Message title, Message subtitle) {
        switch (notificationType.toUpperCase()) {
            case "TITLE":
                Titles.player(playerRef, title, subtitle, true);
                break;

            case "NOTIFICATION":
                Notifications.player(
                        playerRef,
                        title,
                        subtitle,
                        null,
                        NotificationStyle.Success
                );
                break;

            case "CHAT":
                Messenger.sendMessage(playerRef, Message.join(title, Message.raw(" - "), subtitle));
                break;

            case "NONE":
            default:
                break;
        }
    }
}