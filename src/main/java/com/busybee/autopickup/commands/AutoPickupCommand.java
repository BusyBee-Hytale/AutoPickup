package com.busybee.autopickup.commands;

import ai.kodari.hylib.commons.message.Messenger;
import com.busybee.autopickup.AutoPickupPlugin;
import com.busybee.autopickup.ui.pages.AutoPickupPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class AutoPickupCommand extends AbstractPlayerCommand {

    private final AutoPickupPlugin plugin;

    public AutoPickupCommand(AutoPickupPlugin plugin) {
        super("autopickup", "Open AutoPickup settings");
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
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        if (!plugin.getConfig().getBoolean("autopickup.enabled", true)) {
            Messenger.sendMessage(playerRef, "<color:#ff0000>AutoPickup is currently disabled by an administrator.");
            return;
        }

        player.getPageManager().openCustomPage(ref, store, new AutoPickupPage(playerRef, plugin));
    }
}
