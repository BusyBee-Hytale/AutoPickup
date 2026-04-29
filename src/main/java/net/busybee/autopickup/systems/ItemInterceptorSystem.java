package net.busybee.autopickup.systems;

import net.busybee.autopickup.AutoPickupPlugin;
import net.busybee.autopickup.config.PluginConfig;
import net.busybee.autopickup.util.NotificationHelper;
import net.busybee.autopickup.util.Permissions;
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
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ItemInterceptorSystem extends RefSystem<EntityStore> {

    private final AutoPickupPlugin plugin;
    private final Set<Ref<EntityStore>> processedItems = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public ItemInterceptorSystem(AutoPickupPlugin plugin) {
        this.plugin = plugin;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(ItemComponent.getComponentType(), TransformComponent.getComponentType());
    }

    @Override
    public void onEntityAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull AddReason addReason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        if (addReason != AddReason.SPAWN) {
            return;
        }

        if (!processedItems.add(ref)) {
            return;
        }

        if (!plugin.getConfig().getBoolean("autopickup.enabled", true)) {
            return;
        }

        TransformComponent transform = commandBuffer.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }

        Vector3d position = transform.getPosition();
        Vector3i blockPos = new Vector3i(
                (int) Math.floor(position.x),
                (int) Math.floor(position.y),
                (int) Math.floor(position.z)
        );

        BreakBlockHandler.BreakEntry breakEntry = findNearbyBreak(blockPos);
        if (breakEntry == null) {
            return;
        }

        PlayerRef playerRef = breakEntry.getPlayerRef();
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }

        if (!plugin.getPlayerDataManager().isAutoPickupEnabled(playerRef.getUuid())) {
            return;
        }

        ItemComponent itemComponent = commandBuffer.getComponent(ref, ItemComponent.getComponentType());
        if (itemComponent == null) {
            return;
        }

        ItemStack itemStack = itemComponent.getItemStack();
        if (ItemStack.isEmpty(itemStack)) {
            return;
        }

        String itemId = itemStack.getItemId();
        if (!shouldPickup(breakEntry.isMobDrop() ? itemId : breakEntry.getBlockId())) {
            return;
        }

        Player player = getPlayerEntity(playerRef, store);
        if (player == null) {
            return;
        }

        if (!Permissions.canToggle(player)) {
            return;
        }

        PluginConfig config = plugin.getPluginConfig();
        if (config.isDisableInCreative()) {
            if (player.getGameMode() == com.hypixel.hytale.protocol.GameMode.Creative) {
                return;
            }
        }

        ItemStackTransaction transaction = player.getInventory()
                .getCombinedHotbarFirst()
                .addItemStack(itemStack);

        ItemStack remainder = transaction.getRemainder();
        if (ItemStack.isEmpty(remainder)) {
            commandBuffer.removeEntity(ref, RemoveReason.REMOVE);
            String notificationType = config.getNotificationType();
            NotificationHelper.sendPickupNotification(playerRef, notificationType, itemStack);

        } else if (remainder.getQuantity() < itemStack.getQuantity()) {
            itemComponent.setItemStack(remainder);

            int pickedUpQuantity = itemStack.getQuantity() - remainder.getQuantity();
            ItemStack pickedUp = itemStack.withQuantity(pickedUpQuantity);
            String notificationType = config.getNotificationType();
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
        processedItems.remove(ref);
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

        if (refStore != store) {
            return null;
        }

        return refStore.getComponent(playerEntityRef, Player.getComponentType());
    }

    private boolean shouldPickup(String blockId) {
        if (blockId == null) {
            return true;
        }

        PluginConfig config = plugin.getPluginConfig();
        boolean whitelistEnabled = config.isWhitelistEnabled();
        boolean blacklistEnabled = config.isBlacklistEnabled();

        if (!whitelistEnabled && !blacklistEnabled) {
            return true;
        }

        String blockIdLower = blockId.toLowerCase();

        if (whitelistEnabled) {
            for (String pattern : config.getWhitelist()) {
                if (blockIdLower.contains(pattern.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }

        if (blacklistEnabled) {
            for (String pattern : config.getBlacklist()) {
                if (blockIdLower.contains(pattern.toLowerCase())) {
                    return false;
                }
            }
            return true;
        }

        return true;
    }

    private BreakBlockHandler.BreakEntry findNearbyBreak(Vector3i itemPos) {
        BreakBlockHandler.BreakEntry exactMatch = plugin.getBreakBlockHandler().getRecentBreak(itemPos);
        if (exactMatch != null) {
            return exactMatch;
        }

        PluginConfig config = plugin.getPluginConfig();
        int radius = config.getPickupRadius();
        BreakBlockHandler.BreakEntry nearbyEntry = plugin.getBreakBlockHandler().findNearbyBreak(itemPos, radius);

        if (nearbyEntry == null && config.isTreeDetectionEnabled()) {
            int treeRadius = config.getTreePickupRadius();
            if (treeRadius > radius) {
                nearbyEntry = plugin.getBreakBlockHandler().findNearbyBreak(itemPos, treeRadius, true);
            }
        }

        return nearbyEntry;
    }
}
