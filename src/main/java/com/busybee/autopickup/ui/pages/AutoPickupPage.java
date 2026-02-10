package com.busybee.autopickup.ui.pages;

import ai.kodari.hylib.commons.util.ChatUtil;
import com.busybee.autopickup.AutoPickupPlugin;
import com.busybee.autopickup.ui.data.AutoPickupPageData;
import com.busybee.autopickup.util.NotificationHelper;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class AutoPickupPage extends InteractiveCustomUIPage<AutoPickupPageData> {

    private final AutoPickupPlugin plugin;
    private boolean isEnabled;
    private int radiusValue;

    public AutoPickupPage(@Nonnull PlayerRef playerRef, AutoPickupPlugin plugin) {
        super(playerRef, CustomPageLifetime.CanDismiss, AutoPickupPageData.CODEC);
        this.plugin = plugin;
        this.isEnabled = plugin.getPlayerDataManager().isAutoPickupEnabled(playerRef.getUuid());
        this.radiusValue = plugin.getPlayerDataManager().getRadius(playerRef.getUuid());
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder events,
            @Nonnull Store<EntityStore> store
    ) {
        cmd.append("Pages/AutoPickup/AutoPickupPage.ui");
        this.setupValues(cmd);
        this.bindEvents(events);
    }

    private void setupValues(UICommandBuilder cmd) {
        cmd.set("#StatusLabel.TextSpans",
                this.isEnabled
                        ? ChatUtil.parse(plugin.getMessages().getString("ui.status-enabled", "<color:#22c55e>ENABLED"))
                        : ChatUtil.parse(plugin.getMessages().getString("ui.status-disabled", "<color:#ef4444>DISABLED"))
        );

        cmd.set("#RadiusLabel.Text", String.valueOf(this.radiusValue));

        String toggleNotifType = plugin.getConfig().getString("autopickup.toggle-notification-type", "TITLE");
        cmd.set("#ToggleNotifType.Text", toggleNotifType.toUpperCase());

        String pickupNotifType = plugin.getConfig().getString("autopickup.notification-type", "NOTIFICATION");
        cmd.set("#PickupNotifType.Text", pickupNotifType.toUpperCase());
    }

    private void bindEvents(UIEventBuilder events) {
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CloseButton",
                EventData.of("Button", "Close"),
                false
        );

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ToggleBtn",
                EventData.of("Button", "Toggle"),
                false
        );

        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#RadiusSlider",
                EventData.of("Button", "RadiusChanged").append("@Radius", "#RadiusSlider.Value"),
                false
        );
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull AutoPickupPageData data
    ) {
        super.handleDataEvent(ref, store, data);
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        if (data.getRadius() != null) {
            this.radiusValue = Math.round(data.getRadius());
            plugin.getPlayerDataManager().setRadius(this.playerRef.getUuid(), this.radiusValue);
        }

        if (data.getButton() != null) {
            switch (data.getButton()) {
                case "Close" -> {
                    player.getPageManager().setPage(ref, store, Page.None);
                    return;
                }
                case "Toggle" -> {
                    this.isEnabled = !this.isEnabled;
                    plugin.getPlayerDataManager().setAutoPickupEnabled(this.playerRef.getUuid(), this.isEnabled);

                    String notificationType = plugin.getConfig().getString("autopickup.toggle-notification-type", "TITLE");

                    if (this.isEnabled) {
                        NotificationHelper.sendToggleNotification(
                                this.playerRef,
                                notificationType,
                                plugin.getMessages().getString("titles.enabled", "<color:#22c55e>AutoPickup Enabled"),
                                plugin.getMessages().getString("titles.enabled-subtitle", "<white>Items will be automatically picked up")
                        );
                    } else {
                        NotificationHelper.sendToggleNotification(
                                this.playerRef,
                                notificationType,
                                plugin.getMessages().getString("titles.disabled", "<color:#ff0000>AutoPickup Disabled"),
                                plugin.getMessages().getString("titles.disabled-subtitle", "<white>Items will drop normally")
                        );
                    }

                    this.updateStatus();
                    return;
                }
                case "RadiusChanged" -> {
                    this.updateRadiusLabel();
                    return;
                }
            }
        }

        this.sendUpdate();
    }

    private void updateStatus() {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#StatusLabel.TextSpans",
                this.isEnabled
                        ? ChatUtil.parse(plugin.getMessages().getString("ui.status-enabled", "<color:#22c55e>ENABLED"))
                        : ChatUtil.parse(plugin.getMessages().getString("ui.status-disabled", "<color:#ef4444>DISABLED"))
        );

        UIEventBuilder events = new UIEventBuilder();
        this.bindEvents(events);
        this.sendUpdate(cmd, events, false);
    }

    private void updateRadiusLabel() {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#RadiusLabel.Text", String.valueOf(this.radiusValue));

        UIEventBuilder events = new UIEventBuilder();
        this.bindEvents(events);
        this.sendUpdate(cmd, events, false);
    }
}