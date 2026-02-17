package com.busybee.autopickup.systems;

import com.busybee.autopickup.AutoPickupPlugin;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.busybee.autopickup.AutoPickupPlugin.LOGGER;

public class BreakBlockHandler extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    public static class BreakEntry {
        private final UUID playerUUID;
        private final String blockId;
        private final long timestamp;
        private final boolean mobDrop;

        BreakEntry(UUID playerUUID, String blockId) {
            this(playerUUID, blockId, false);
        }

        BreakEntry(UUID playerUUID, String blockId, boolean mobDrop) {
            this.playerUUID = playerUUID;
            this.blockId = blockId;
            this.timestamp = System.currentTimeMillis();
            this.mobDrop = mobDrop;
        }

        public UUID getPlayerUUID() {
            return playerUUID;
        }
        public String getBlockId() {
            return blockId;
        }
        public long getTimestamp() {
            return timestamp;
        }
        public boolean isMobDrop() {
            return mobDrop;
        }

        @Nullable
        public PlayerRef getPlayerRef() {
            return Universe.get().getPlayer(playerUUID);
        }
    }

    private final Map<Vector3i, BreakEntry> recentBreaks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public BreakBlockHandler() {
        super(BreakBlockEvent.class);
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
        Vector3i blockPos = event.getTargetBlock();

        if (blockPos == null) {
            return;
        }

        recentBreaks.put(blockPos, new BreakEntry(playerUUID, blockId));
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(Player.getComponentType());
    }

    @Nullable
    public BreakEntry getRecentBreak(Vector3i position) {
        BreakEntry entry = recentBreaks.get(position);
        if (entry != null) {
            long expiryTime = AutoPickupPlugin.getInstance().getConfig().getLong("autopickup.entry-expiry-ms", 500L);
            if (System.currentTimeMillis() - entry.timestamp <= expiryTime) {
                return entry;
            }
        }
        return null;
    }

    @Nullable
    public BreakEntry findNearbyBreak(Vector3i itemPos, int radius) {
        long expiryTime = AutoPickupPlugin.getInstance().getConfig().getLong("autopickup.entry-expiry-ms", 500L);
        long now = System.currentTimeMillis();

        BreakEntry closestEntry = null;
        double closestDistanceSq = Double.MAX_VALUE;

        // Search for the closest recent break within radius
        for (Map.Entry<Vector3i, BreakEntry> entry : recentBreaks.entrySet()) {
            Vector3i breakPos = entry.getKey();
            BreakEntry breakEntry = entry.getValue();

            // Check if entry is still valid (not expired)
            if (now - breakEntry.timestamp > expiryTime) {
                continue;
            }

            // Calculate distance
            int dx = itemPos.x - breakPos.x;
            int dy = itemPos.y - breakPos.y;
            int dz = itemPos.z - breakPos.z;
            double distanceSq = dx * dx + dy * dy + dz * dz;

            // Check if within radius and closer than previous matches
            if (distanceSq <= radius * radius && distanceSq < closestDistanceSq) {
                closestEntry = breakEntry;
                closestDistanceSq = distanceSq;
            }
        }

        return closestEntry;
    }

    public void markMobDeath(Vector3i position, UUID playerUUID) {
        recentBreaks.put(position, new BreakEntry(playerUUID, "MOB_DROP", true));
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
