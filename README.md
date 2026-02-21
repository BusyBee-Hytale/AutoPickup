# AutoPickup

A Hytale server plugin that automatically picks up items from broken blocks and mob drops directly into player inventories.

## Features

- **Auto-pickup items** from broken blocks and mob deaths
- **Per-player toggle** - Players can enable/disable auto-pickup individually
- **Whitelist/Blacklist system** - Control which items are automatically picked up
- **Flexible notifications** - Choose between title, notification, chat, or no notifications
- **Creative mode support** - Optionally disable auto-pickup in creative mode
- **Smart radius detection** - Only picks up items from blocks the player broke
- **Intelligent tree detection** - Expanded pickup radius for tree-related blocks (logs, leaves, branches)
- **Cross-world protection** - Prevents crashes from cross-world entity interactions
- **Thread-safe operations** - Handles intensive drop events without concurrency issues
- **Config validation** - Automatic validation and repair of configuration files
- **Partial matching** - Use patterns to match multiple item types at once

## Installation

### For Server Owners

1. Download the latest `AutoPickup-X.X.X.jar` from the releases page
2. Place the jar file in your server's `plugins` folder
3. Start or restart your server
4. The plugin will generate a `config.yml` in `mods/AutoPickup/`
5. Configure the plugin to your preferences (see Configuration section)
6. Run `/autopickup reload` to apply config changes

## Commands

| Command                | Description                            | Permission            |
|------------------------|----------------------------------------|-----------------------|
| `/autopickup`          | Toggle auto-pickup on/off for yourself | `autopickup.toggle`   |
| `/autopickup reload`   | Reload the plugin configuration        | `autopickup.reload`   |

## Permissions

| Permission            | Description                       | Default        |
|-----------------------|-----------------------------------|----------------|
| `autopickup.toggle`   | Allow toggling auto-pickup on/off | All players    |
| `autopickup.reload`   | Allow reloading the configuration | Operators only |
| `autopickup.settings` | Allow viewing plugin settings     | All players    |

## Configuration

The plugin's `config.yml` file offers extensive customization options:

### General Settings

```yaml
autopickup:
  # Enable/disable the plugin globally
  enabled: true

  # Whether autopickup is enabled by default for new players
  default-enabled: false

  # How long to keep break/death locations in memory (milliseconds)
  entry-expiry-ms: 500

  # Radius in blocks to search for recent breaks/deaths when items spawn
  pickup-radius: 3

  # Disable autopickup when players are in creative mode
  disable-in-creative: true

  # Delay before items are picked up (ticks, 20 = 1 second)
  pickup-delay-ticks: 0
```

### Tree Detection Settings

```yaml
  # Enable intelligent tree detection for better multi-block structure handling
  tree-detection-enabled: true

  # Expanded radius for tree-related drops (logs, leaves, branches)
  # Trees scatter items more than regular blocks, so a larger radius helps
  tree-pickup-radius: 5

  # Block IDs that are considered part of trees (supports partial matching)
  tree-blocks:
    - "Log"
    - "Leaf"
    - "Branch"
    - "Palm"
    - "Oak"
    - "Birch"
    - "Spruce"
    - "Pine"
```

### Notification Settings

```yaml
  # Notification type when items are picked up
  # Options: TITLE, NOTIFICATION, CHAT, NONE
  notification-type: "NOTIFICATION"

  # Notification type when toggling autopickup on/off
  # Options: TITLE, NOTIFICATION, CHAT, NONE
  toggle-notification-type: "TITLE"
```

### Whitelist/Blacklist Settings

**Whitelist Mode** - Only specified items will be auto-picked up:

```yaml
  # Whitelist - Only these blocks/items will be auto-picked up (when whitelist-enabled: true)
  # Supports partial matching (e.g., "Ore" matches all ore types)
  
  whitelist:
     - "Ore_Copper_Shale"
     - "Ingredient_Bone_Fragment"
```

**Blacklist Mode** - Everything except specified items will be auto-picked up:

```yaml
  # Blacklist - These blocks/items will NOT be auto-picked up (when blacklist-enabled: true)
  # Supports partial matching (e.g., "Container" matches all containers)
  
  blacklist:
     - "Ingredient_Sac_Venom"
     - "Ingredient_Fabric_Scrap_Linen"
```

**Note:** Only one mode (whitelist OR blacklist) can be enabled at a time. If both are enabled, the plugin will automatically disable the blacklist and log a warning. If both are disabled, all items will be auto-picked up.

### Partial Matching

The whitelist/blacklist system supports partial matching with case-insensitive comparison:

- `"Ore"` - Matches all ore types (Ore_Copper, Ore_Iron, etc.)
- `"Ingredient_"` - Matches all ingredients
- `"Container"` - Matches all containers
- `"Ore_Copper"` - Matches only copper ore exactly

## Finding Item IDs

To find the correct item IDs to use in whitelist/blacklist:

1. Enable auto-pickup and break blocks or kill mobs
2. Check your server logs for item pickup messages (if debug is enabled)
3. Common ID patterns:
   - **Ores**: `Ore_Copper`, `Ore_Iron`, `Ore_Gold`, etc.
   - **Ingredients**: `Ingredient_Bone_Fragment`, `Ingredient_Leather_Medium`, etc.
   - **Furniture**: `Furniture_Village_Ladder`, `Furniture_Crude_Chest_Small`, etc.
   - **Containers**: `Container_Chest`, `Container_Barrel`, etc.

## Examples

### Example 1: Only Auto-Pickup Ores

```yaml
whitelist-enabled: true
blacklist-enabled: false
whitelist:
  - "Ore"  # Matches all ore types
```

### Example 2: Pickup Everything Except Containers

```yaml
whitelist-enabled: false
blacklist-enabled: true
blacklist:
  - "Container"  # Blocks all containers
  - "Furniture"  # Blocks all furniture
```

### Example 3: Only Pickup Valuable Items

```yaml
whitelist-enabled: true
blacklist-enabled: false
whitelist:
  - "Ore_Diamond"
  - "Ore_Emerald"
  - "Ore_Gold"
  - "Ingredient_Life_Essence"
```

## Player Usage

### Toggling Auto-Pickup

Players can toggle auto-pickup on or off at any time:

```
/autopickup
```

This will enable or disable auto-pickup for that player only. The setting is saved and persists across server restarts and player disconnects.

### Checking Settings

To view the current plugin configuration:

```
/autopickup settings
```

## Technical Details

### Configuration Validation

The plugin automatically validates your configuration file on startup and will:
- Check that all values are the correct type (boolean, number, string, list)
- Verify numeric values are within acceptable ranges
- Validate notification types are one of: TITLE, NOTIFICATION, CHAT, NONE
- Ensure whitelist and blacklist aren't both enabled
- Auto-repair invalid values and log warnings

Valid ranges:
- `entry-expiry-ms`: 100-10000 ms
- `pickup-radius`: 1-10 blocks
- `tree-pickup-radius`: 1-15 blocks
- `pickup-delay-ticks`: 0-100 ticks

### Performance & Stability

- **Thread-safe operations**: Uses `ConcurrentHashMap` and synchronized methods to prevent race conditions during intensive drop events (mob farms, mass tree cutting)
- **Cross-world protection**: Validates that players and items are in the same world before pickup to prevent crashes
- **Double-processing prevention**: Tracks processed items to avoid edge cases where items could be processed multiple times
- **Memory management**: Automatically cleans up expired break entries every second

## Support

For issues, suggestions, or questions, please open an issue on the GitHub repository.

## License

This plugin is provided as-is for use on Hytale servers.
