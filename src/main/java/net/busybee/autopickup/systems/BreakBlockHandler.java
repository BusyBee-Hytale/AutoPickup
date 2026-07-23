package net.busybee.autopickup.systems;

import com.hypixel.hytale.component.*;
import net.busybee.autopickup.AutoPickupPlugin;
import net.busybee.autopickup.config.PluginConfig;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BreakBlockHandler extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    public static class BreakEntry {
        private final UUID playerUUID;
        private final String blockId;
        private final long timestamp;
        private final boolean mobDrop;
        private final boolean treeBlock;

        BreakEntry(UUID playerUUID, String blockId) {
            this(playerUUID, blockId, false, false);
        }

        BreakEntry(UUID playerUUID, String blockId, boolean mobDrop) {
            this(playerUUID, blockId, mobDrop, false);
        }

        BreakEntry(UUID playerUUID, String blockId, boolean mobDrop, boolean treeBlock) {
            this.playerUUID = playerUUID;
            this.blockId = blockId;
            this.timestamp = System.currentTimeMillis();
            this.mobDrop = mobDrop;
            this.treeBlock = treeBlock;
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
        public boolean isTreeBlock() {
            return treeBlock;
        }

        @Nullable
        public PlayerRef getPlayerRef() {
            return Universe.get().getPlayer(playerUUID);
        }
    }

    private final Cache<Vector3i, BreakEntry> recentBreaks;

    public BreakBlockHandler() {
        super(BreakBlockEvent.class);
        long expiryTime = AutoPickupPlugin.getInstance().getPluginConfig().getEntryExpiryMs();
        this.recentBreaks = Caffeine.newBuilder()
                .expireAfterWrite(expiryTime, TimeUnit.MILLISECONDS)
                .maximumSize(10000)
                .build();
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

        boolean isTreeBlock = isTreeRelatedBlock(blockId);
        recentBreaks.put(blockPos, new BreakEntry(playerUUID, blockId, false, isTreeBlock));
    }

    private boolean isTreeRelatedBlock(String blockId) {
        if (blockId == null) {
            return false;
        }

        PluginConfig config = AutoPickupPlugin.getInstance().getPluginConfig();
        if (!config.isTreeDetectionEnabled()) {
            return false;
        }

        String blockIdLower = blockId.toLowerCase();
        for (String pattern : config.getTreeBlocks()) {
            if (blockIdLower.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(Player.getComponentType());
    }

    @Nullable
    public BreakEntry getRecentBreak(Vector3i position) {
        return recentBreaks.getIfPresent(position);
    }

    @Nullable
    public BreakEntry findNearbyBreak(Vector3i itemPos, int radius) {
        return findNearbyBreak(itemPos, radius, false);
    }

    @Nullable
    public BreakEntry findNearbyBreak(Vector3i itemPos, int radius, boolean treeOnly) {
        BreakEntry closestEntry = null;
        double closestDistanceSq = Double.MAX_VALUE;
        double maxDistanceSq = (double) radius * radius;

        for (Map.Entry<Vector3i, BreakEntry> entry : recentBreaks.asMap().entrySet()) {
            Vector3i breakPos = entry.getKey();
            BreakEntry breakEntry = entry.getValue();

            if (treeOnly && !breakEntry.isTreeBlock()) {
                continue;
            }

            int dx = itemPos.x - breakPos.x;
            int dy = itemPos.y - breakPos.y;
            int dz = itemPos.z - breakPos.z;
            double distanceSq = (double) dx * dx + dy * dy + dz * dz;

            if (distanceSq <= maxDistanceSq && distanceSq < closestDistanceSq) {
                closestEntry = breakEntry;
                closestDistanceSq = distanceSq;
            }
        }

        return closestEntry;
    }

    public void markMobDeath(Vector3i position, UUID playerUUID) {
        recentBreaks.put(position, new BreakEntry(playerUUID, "MOB_DROP", true));
    }

    public void shutdown() {
        recentBreaks.invalidateAll();
    }
}
