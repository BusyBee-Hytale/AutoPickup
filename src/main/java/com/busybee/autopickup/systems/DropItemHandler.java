package com.busybee.autopickup.systems;

import ai.kodari.hylib.commons.scheduler.Scheduler;
import com.busybee.autopickup.AutoPickupPlugin;
import com.busybee.autopickup.util.NotificationHelper;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;

public class DropItemHandler extends EntityEventSystem<EntityStore, DropItemEvent.Drop> {

    private final AutoPickupPlugin plugin;

    public DropItemHandler(AutoPickupPlugin plugin) {
        super(DropItemEvent.Drop.class);
        this.plugin = plugin;
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull DropItemEvent.Drop event
    ) {
        if (!plugin.getConfig().getBoolean("autopickup.enabled", true)) {
            return;
        }

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);

        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComponent == null) return;

        UUID playerUUID = uuidComponent.getUuid();

        if (!plugin.getPlayerDataManager().isAutoPickupEnabled(playerUUID)) {
            return;
        }

        PlayerRef playerRef = Universe.get().getPlayer(playerUUID);
        if (playerRef == null || !playerRef.isValid()) return;

        if (plugin.getConfig().getBoolean("autopickup.disable-in-creative", true)) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null && isCreativeMode(player)) {
                return;
            }
        }

        String brokenBlock = plugin.getBreakBlockHandler().getAndClearRecentBreak(playerUUID);

        if (brokenBlock != null && !shouldPickup(brokenBlock)) {
            return;
        }

        World world = ((EntityStore) store.getExternalData()).getWorld();

        int delayTicks = plugin.getConfig().getInt("autopickup.pickup-delay-ticks", 0);

        if (delayTicks == 0) {
            performPickup(world, playerRef, ref, event.getItemStack());
            event.setCancelled(true);
        } else {
            Scheduler.runLater(() -> {
                world.execute(() -> {
                    if (ref.isValid()) {
                        performPickup(world, playerRef, ref, event.getItemStack());
                    }
                });
            }, delayTicks / 20);
        }
    }

    private boolean isCreativeMode(Player player) {
        return player.getGameMode() == com.hypixel.hytale.protocol.GameMode.Creative;
    }

    private void performPickup(World world, PlayerRef playerRef, Ref<EntityStore> ref, ItemStack itemStack) {
        world.execute(() -> {
            Player player = ref.getStore().getComponent(ref, Player.getComponentType());
            if (player == null) return;

            ItemStackTransaction transaction = player.getInventory()
                    .getCombinedHotbarFirst()
                    .addItemStack(itemStack);

            ItemStack remainder = transaction.getRemainder();
            if (remainder == null || remainder.isEmpty()) {
                String notificationType = plugin.getConfig().getString("autopickup.notification-type", "NOTIFICATION");
                NotificationHelper.sendPickupNotification(playerRef, notificationType, itemStack);
            }
        });
    }

    private boolean shouldPickup(String blockId) {
        boolean whitelistEnabled = plugin.getConfig().getBoolean("autopickup.whitelist-enabled", false);
        boolean blacklistEnabled = plugin.getConfig().getBoolean("autopickup.blacklist-enabled", false);

        if (!whitelistEnabled && !blacklistEnabled) {
            return true;
        }

        String blockIdLower = blockId.toLowerCase();

        if (whitelistEnabled) {
            for (String pattern : plugin.getConfig().getStringList("autopickup.whitelist")) {
                if (blockIdLower.contains(pattern.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }

        if (blacklistEnabled) {
            for (String pattern : plugin.getConfig().getStringList("autopickup.blacklist")) {
                if (blockIdLower.contains(pattern.toLowerCase())) {
                    return false;
                }
            }
            return true;
        }

        return true;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
