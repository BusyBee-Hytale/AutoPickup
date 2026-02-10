package com.busybee.autopickup.systems;

import com.busybee.autopickup.AutoPickupPlugin;
import com.busybee.autopickup.util.NotificationHelper;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

import static com.busybee.autopickup.AutoPickupPlugin.LOGGER;

public class ItemInterceptorSystem extends RefSystem<EntityStore> {

    private final AutoPickupPlugin plugin;

    public ItemInterceptorSystem(AutoPickupPlugin plugin) {
        this.plugin = plugin;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        // Listen for entities with ItemComponent and TransformComponent
        return Query.and(ItemComponent.getComponentType(), TransformComponent.getComponentType());
    }

    @Override
    public void onEntityAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull AddReason addReason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // Only handle spawned entities
        if (addReason != AddReason.SPAWN) {
            return;
        }

        if (!plugin.getConfig().getBoolean("autopickup.enabled", true)) {
            return;
        }

        // Get item transform component
        TransformComponent transform = commandBuffer.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }

        // Get item position
        Vector3d position = transform.getPosition();
        Vector3i blockPos = new Vector3i(
                (int) Math.floor(position.x),
                (int) Math.floor(position.y),
                (int) Math.floor(position.z)
        );

        // Check if this position has a recent break/death
        BreakBlockHandler.BreakEntry breakEntry = plugin.getBreakBlockHandler().getRecentBreak(blockPos);
        if (breakEntry == null) {
            return;
        }

        // Get player ref
        PlayerRef playerRef = breakEntry.getPlayerRef();
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }

        // Check if player has autopickup enabled
        if (!plugin.getPlayerDataManager().isAutoPickupEnabled(playerRef.getUuid())) {
            return;
        }

        // Get item component
        ItemComponent itemComponent = commandBuffer.getComponent(ref, ItemComponent.getComponentType());
        if (itemComponent == null) {
            return;
        }

        ItemStack itemStack = itemComponent.getItemStack();
        if (ItemStack.isEmpty(itemStack)) {
            return;
        }

        // Check whitelist/blacklist (mob drops bypass this check)
        if (!breakEntry.isMobDrop() && !shouldPickup(breakEntry.getBlockId())) {
            return;
        }

        // Get player entity
        Player player = getPlayerEntity(playerRef, store);
        if (player == null) {
            return;
        }

        // Check creative mode
        if (plugin.getConfig().getBoolean("autopickup.disable-in-creative", true)) {
            if (player.getGameMode() == com.hypixel.hytale.protocol.GameMode.Creative) {
                return;
            }
        }

        LOGGER.atInfo().log("ItemInterceptor - Picking up item " + itemStack.getItemId() + " x" + itemStack.getQuantity() + " for player " + playerRef.getUuid());

        // Add item to player inventory
        ItemStackTransaction transaction = player.getInventory()
                .getCombinedHotbarFirst()
                .addItemStack(itemStack);

        ItemStack remainder = transaction.getRemainder();
        if (ItemStack.isEmpty(remainder)) {
            // All items picked up - remove entity
            commandBuffer.removeEntity(ref, RemoveReason.REMOVE);

            // Send notification
            String notificationType = plugin.getConfig().getString("autopickup.notification-type", "NOTIFICATION");
            NotificationHelper.sendPickupNotification(playerRef, notificationType, itemStack);
        } else if (remainder.getQuantity() < itemStack.getQuantity()) {
            // Partial pickup - update item stack
            itemComponent.setItemStack(remainder);

            // Send notification for picked up amount
            int pickedUpQuantity = itemStack.getQuantity() - remainder.getQuantity();
            ItemStack pickedUp = itemStack.withQuantity(pickedUpQuantity);
            String notificationType = plugin.getConfig().getString("autopickup.notification-type", "NOTIFICATION");
            NotificationHelper.sendPickupNotification(playerRef, notificationType, pickedUp);
        }
    }

    @Override
    public void onEntityRemove(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull RemoveReason removeReason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // No action needed on entity removal
    }

    private Player getPlayerEntity(PlayerRef playerRef, Store<EntityStore> store) {
        if (!playerRef.isValid()) {
            return null;
        }

        Ref<EntityStore> playerEntityRef = playerRef.getReference();
        if (playerEntityRef == null || !playerEntityRef.isValid()) {
            return null;
        }

        Store<EntityStore> refStore = playerEntityRef.getStore();
        if (refStore == null) {
            return null;
        }

        return refStore.getComponent(playerEntityRef, Player.getComponentType());
    }

    private boolean shouldPickup(String blockId) {
        if (blockId == null) {
            return true;
        }

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
}
