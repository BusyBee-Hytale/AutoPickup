package net.busybee.autopickup.systems;

import java.util.concurrent.TimeUnit;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.world.World;
import net.busybee.autopickup.AutoPickupPlugin;
import net.busybee.autopickup.config.PluginConfig;
import net.busybee.autopickup.util.NotificationHelper;
import net.busybee.autopickup.util.Permissions;
import com.hypixel.hytale.component.query.Query;
import org.joml.Vector3d;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PickupItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PreventPickup;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.PropComponent;
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
        return Query.and(
                ItemComponent.getComponentType(),
                TransformComponent.getComponentType(),
                Query.not(PickupItemComponent.getComponentType()),
                Query.not(Interactable.getComponentType()),
                Query.not(PreventPickup.getComponentType()),
                Query.not(PropComponent.getComponentType())
        );
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

        // Additional validation to ensure we don't intercept something that already has excluded components
        if (commandBuffer.getComponent(ref, PickupItemComponent.getComponentType()) != null ||
            commandBuffer.getComponent(ref, Interactable.getComponentType()) != null ||
            commandBuffer.getComponent(ref, PreventPickup.getComponentType()) != null ||
            commandBuffer.getComponent(ref, PropComponent.getComponentType()) != null) {
            return;
        }

        if (!processedItems.add(ref)) {
            return;
        }

        int delayTicks = plugin.getPluginConfig().getPickupDelayTicks();
        if (delayTicks <= 0) {
            processPickup(ref, store, commandBuffer);
        } else {
            World world = store.getExternalData().getWorld();
            world.scheduleAfter(() -> {
                processPickup(ref, store, null);
            }, (long) delayTicks * 50, TimeUnit.MILLISECONDS);
        }
    }

    private void processPickup(Ref<EntityStore> ref, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        if (!ref.isValid()) {
            return;
        }


        if (!plugin.getConfig().getBoolean("autopickup.enabled", true)) {
            return;
        }

        // Late-binding validation for excluded components (important for delayed pickup)
        ItemComponent itemComponent = commandBuffer != null
                ? commandBuffer.getComponent(ref, ItemComponent.getComponentType())
                : store.getComponent(ref, ItemComponent.getComponentType());
        
        if (itemComponent == null || itemComponent.isRemovedByPlayerPickup()) {
            return;
        }

        if (commandBuffer != null) {
            if (commandBuffer.getComponent(ref, PickupItemComponent.getComponentType()) != null ||
                commandBuffer.getComponent(ref, Interactable.getComponentType()) != null ||
                commandBuffer.getComponent(ref, PreventPickup.getComponentType()) != null ||
                commandBuffer.getComponent(ref, PropComponent.getComponentType()) != null) {
                return;
            }
        } else {
            if (store.getComponent(ref, PickupItemComponent.getComponentType()) != null ||
                store.getComponent(ref, Interactable.getComponentType()) != null ||
                store.getComponent(ref, PreventPickup.getComponentType()) != null ||
                store.getComponent(ref, PropComponent.getComponentType()) != null) {
                return;
            }
        }

        TransformComponent transform = commandBuffer != null 
                ? commandBuffer.getComponent(ref, TransformComponent.getComponentType())
                : store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }

        Vector3d position = new Vector3d(transform.getPosition());
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

        ItemStack itemStack = itemComponent.getItemStack();
        if (ItemStack.isEmpty(itemStack)) {
            return;
        }

        String itemId = itemStack.getItemId();
        if (!shouldPickup(breakEntry.isMobDrop() ? itemId : breakEntry.getBlockId())) {
            return;
        }

        Player player = getPlayerEntity(playerRef, store, commandBuffer);
        if (player == null) {
            return;
        }

        if (!Permissions.canToggle(playerRef)) {
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
            
            // Clear the stack immediately to prevent other systems from picking it up
            itemComponent.setItemStack(ItemStack.EMPTY);
            itemComponent.setRemovedByPlayerPickup(true);
            
            if (commandBuffer != null) {
                // Follow Hytale's pattern: remove original and spawn new animation entity
                commandBuffer.removeEntity(ref, RemoveReason.REMOVE);
                com.hypixel.hytale.component.Holder<EntityStore> animHolder = ItemComponent.generatePickedUpItem(itemStack, position, commandBuffer, playerRef.getReference());
                commandBuffer.addEntity(animHolder, AddReason.SPAWN);
            } else {
                store.removeEntity(ref, RemoveReason.REMOVE);
                com.hypixel.hytale.component.Holder<EntityStore> animHolder = ItemComponent.generatePickedUpItem(itemStack, position, store, playerRef.getReference());
                store.addEntity(animHolder, AddReason.SPAWN);
            }
            
            String notificationType = config.getNotificationType();
            NotificationHelper.sendPickupNotification(playerRef, notificationType, itemStack);

        } else if (remainder.getQuantity() < itemStack.getQuantity()) {
            itemComponent.setItemStack(remainder);

            int pickedUpQuantity = itemStack.getQuantity() - remainder.getQuantity();
            ItemStack pickedUp = itemStack.withQuantity(pickedUpQuantity);
            
            if (commandBuffer != null) {
                com.hypixel.hytale.component.Holder<EntityStore> animHolder = ItemComponent.generatePickedUpItem(
                    pickedUp, position, commandBuffer, playerRef.getReference()
                );
                commandBuffer.addEntity(animHolder, AddReason.SPAWN);
            } else {
                com.hypixel.hytale.component.Holder<EntityStore> animHolder = ItemComponent.generatePickedUpItem(
                    pickedUp, position, store, playerRef.getReference()
                );
                store.addEntity(animHolder, AddReason.SPAWN);
            }

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

    private Player getPlayerEntity(PlayerRef playerRef, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
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

        return commandBuffer != null
                ? commandBuffer.getComponent(playerEntityRef, Player.getComponentType())
                : refStore.getComponent(playerEntityRef, Player.getComponentType());
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
