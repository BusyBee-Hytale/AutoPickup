package net.busybee.autopickup.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.UUID;

@DatabaseTable(tableName = "autopickup_players")
public class PlayerData {

    @DatabaseField(id = true, columnName = "uuid")
    private String uuid;

    @DatabaseField(columnName = "autopickup_enabled")
    private boolean autoPickupEnabled;

    @DatabaseField(columnName = "last_known_name")
    private String lastKnownName;

    public PlayerData() {}

    public PlayerData(UUID uuid, boolean autoPickupEnabled, String lastKnownName) {
        this.uuid = uuid.toString();
        this.autoPickupEnabled = autoPickupEnabled;
        this.lastKnownName = lastKnownName;
    }

    public UUID getUuid() {
        return UUID.fromString(uuid);
    }
    public String getUuidString() {
        return uuid;
    }
    public boolean isAutoPickupEnabled() {
        return autoPickupEnabled;
    }
    public void setAutoPickupEnabled(boolean autoPickupEnabled) {
        this.autoPickupEnabled = autoPickupEnabled;
    }
    public String getLastKnownName() {
        return lastKnownName;
    }
    public void setLastKnownName(String lastKnownName) {
        this.lastKnownName = lastKnownName;
    }
}
