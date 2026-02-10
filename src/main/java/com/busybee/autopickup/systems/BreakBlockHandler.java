package com.busybee.autopickup.systems;

import com.busybee.autopickup.AutoPickupPlugin;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.busybee.autopickup.AutoPickupPlugin.LOGGER;

public class BreakBlockHandler extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    private static class BreakEntry {
        final String blockId;
        final long timestamp;

        BreakEntry(String blockId) {
            this.blockId = blockId;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private final Map<UUID, BreakEntry> recentBreaks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public BreakBlockHandler() {
        super(BreakBlockEvent.class);
        // Clean up old entries every second
        scheduler.scheduleAtFixedRate(this::cleanupOldEntries, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull BreakBlockEvent event
    ) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);

        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            return;
        }

        UUID playerUUID = uuidComponent.getUuid();
        String blockId = event.getBlockType().getId();

        LOGGER.atInfo().log("BreakBlockEvent - Player: " + playerUUID + ", Block: " + blockId);
        recentBreaks.put(playerUUID, new BreakEntry(blockId));
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(Player.getComponentType());
    }

    public String getRecentBreak(UUID playerUUID) {
        BreakEntry entry = recentBreaks.get(playerUUID);
        if (entry != null) {
            // Check if entry is still valid (within 500ms as configured)
            long expiryTime = AutoPickupPlugin.getInstance().getConfig().getLong("autopickup.entry-expiry-ms", 500L);
            if (System.currentTimeMillis() - entry.timestamp <= expiryTime) {
                return entry.blockId;
            }
        }
        return null;
    }

    public void markMobDeath(UUID playerUUID) {
        // Mark as a special "mob_drop" entry that always passes whitelist/blacklist checks
        recentBreaks.put(playerUUID, new BreakEntry("MOB_DROP"));
        LOGGER.atInfo().log("Marked mob death for player: " + playerUUID);
    }

    private void cleanupOldEntries() {
        long expiryTime = AutoPickupPlugin.getInstance().getConfig().getLong("autopickup.entry-expiry-ms", 500L);
        long now = System.currentTimeMillis();
        recentBreaks.entrySet().removeIf(entry -> (now - entry.getValue().timestamp) > expiryTime);
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}
