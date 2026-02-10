package com.busybee.autopickup.systems;

import com.busybee.autopickup.AutoPickupPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import javax.annotation.Nonnull;
import java.util.UUID;

import static com.busybee.autopickup.AutoPickupPlugin.LOGGER;

public class MobDropListener extends DeathSystems.OnDeathSystem {

    private final BreakBlockHandler breakBlockHandler;

    public MobDropListener(BreakBlockHandler breakBlockHandler) {
        this.breakBlockHandler = breakBlockHandler;
    }

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        // Listen for NPC entity deaths (includes monsters)
        return NPCEntity.getComponentType();
    }

    @Override
    public void onComponentAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull DeathComponent deathComponent,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        AutoPickupPlugin plugin = AutoPickupPlugin.getInstance();
        if (!plugin.getConfig().getBoolean("autopickup.enabled", true)) {
            return;
        }

        // Get death info
        Damage deathInfo = deathComponent.getDeathInfo();
        if (deathInfo == null) {
            return;
        }

        // Get damage source
        Damage.Source source = deathInfo.getSource();
        if (!(source instanceof Damage.EntitySource)) {
            return;
        }

        // Get killer entity ref
        Damage.EntitySource entitySource = (Damage.EntitySource) source;
        Ref<EntityStore> killerRef = entitySource.getRef();

        // Check if killer is a player
        Player killerPlayer = store.getComponent(killerRef, Player.getComponentType());
        if (killerPlayer == null) {
            return;
        }

        // Check if disabled in creative
        if (plugin.getConfig().getBoolean("autopickup.disable-in-creative", true)) {
            if (killerPlayer.getGameMode() == com.hypixel.hytale.protocol.GameMode.Creative) {
                return;
            }
        }

        // Get killer UUID
        UUIDComponent killerUuidComponent = store.getComponent(killerRef, UUIDComponent.getComponentType());
        if (killerUuidComponent == null) {
            return;
        }

        UUID killerUUID = killerUuidComponent.getUuid();
        PlayerRef playerRef = Universe.get().getPlayer(killerUUID);
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }

        // Check if player has autopickup enabled
        if (!plugin.getPlayerDataManager().isAutoPickupEnabled(killerUUID)) {
            return;
        }

        // Get mob position for marking
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }

        Vector3i mobPos = new Vector3i(
                (int) Math.floor(transform.getPosition().x),
                (int) Math.floor(transform.getPosition().y),
                (int) Math.floor(transform.getPosition().z)
        );

        LOGGER.atInfo().log("MobDropListener - NPC death at " + mobPos + ", killed by player: " + killerUUID);

        // Mark this position as a mob_drop so items from this mob will be auto-picked up
        breakBlockHandler.markMobDeath(mobPos, killerUUID);
    }
}
