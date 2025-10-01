# Stormcraft-Events

**Version**: 0.1.0
**Platform**: Paper 1.21.3, Java 21
**Dependencies**: Stormcraft (required), MythicMobs (optional), Towny/TownsAndNations (optional)

## Project Overview

Dynamic event system that spawns storm-themed objectives and encounters based on player density, storm activity, and town presence. Events spawn **in and around storms** (not just inside) to accommodate all player levels.

## Core Features

### 🌩️ Event Types

1. **Storm Surge Retrieval** - Collect storm crystals before they disappear
2. **Tempest Guardian** - Mini-boss spawns near storms with 3+ players
3. **Storm Rift Defense** - Waves of storm-corrupted mobs from rifts
4. **Storm Titan** - World boss (MythicMobs integration) for 10+ players
5. **Town Siege** - Mob waves attack Towny towns during/after storms

### 📍 Spawn System

**Zone Types:**
- **Storm Core** (0-50 blocks from epicenter) - 60% spawn weight
- **Storm Periphery** (50-150 blocks) - 30% spawn weight
- **Storm Influence** (150-300 blocks) - 10% spawn weight

This allows lower-level players to participate from safer distances while keeping the theme.

### 👥 Density-Based Spawning

- Events have higher spawn chance with more players nearby
- Uses **logarithmic scaling** to prevent spam
- Cooldowns per event type prevent repetition

### 🏰 Town Integration

- **Towny/TownsAndNations Support**: Detect active towns near storms
- **Storm Siege**: Random chance (with cooldown) to spawn mob waves at towns
- **Low frequency**: Not constant, triggered by nearby storms + random chance
- **Opt-out**: Towns can disable sieges via config permission

## Architecture

```
StormcraftEvents/
├── event/
│   ├── EventManager.java           # Core coordinator, spawn logic
│   ├── Event.java                  # Base event class
│   ├── EventType.java              # Enum of all event types
│   ├── EventState.java             # SPAWNING, ACTIVE, COMPLETED, FAILED
│   ├── DamageTracker.java          # Track damage per player for bosses
│   └── events/
│       ├── StormSurgeEvent.java    # Collect storm crystals
│       ├── TempestGuardianEvent.java # Mini-boss fight
│       ├── StormRiftEvent.java     # Wave defense
│       ├── StormTitanEvent.java    # World boss (MythicMobs)
│       └── TownSiegeEvent.java     # Town defense
├── spawn/
│   ├── StormZoneCalculator.java    # Calculate spawn zones around storms
│   ├── DensityTracker.java         # Track player clustering
│   ├── EventSpawner.java           # Weighted spawn logic
│   └── SpawnZone.java              # Zone data class
├── integration/
│   ├── StormcraftIntegration.java  # Get active storms, intensity
│   ├── MythicMobsIntegration.java  # Spawn MythicMobs bosses
│   ├── TownyIntegration.java       # Detect towns, spawn sieges
│   └── EssenceIntegration.java     # Award storm essence (optional)
├── objectives/
│   ├── ObjectiveTracker.java       # Track player progress per event
│   ├── Objective.java              # Base objective class
│   └── objectives/
│       ├── CollectObjective.java   # Collect X items
│       ├── DefendObjective.java    # Survive X waves
│       ├── KillObjective.java      # Kill boss/mobs
│       └── ChannelObjective.java   # Channel at location
├── ui/
│   ├── EventNotifier.java          # Announce events (chat, title)
│   ├── EventHUD.java               # Boss bars for event progress
│   └── ParticleEffects.java        # Visual effects for events
├── config/
│   ├── EventConfig.java            # Config loader
│   └── ConfigManager.java          # YAML management
└── StormcraftEventsPlugin.java     # Main plugin class
```

## Configuration Structure

```yaml
# config.yml
events:
  # Global settings
  globalCooldown: 60              # Seconds between any events
  announceRadius: 900             # Blocks to announce events (nearby storm = within 900 blocks)
  useActionBar: true              # Use action bar for event updates
  useBossBar: true                # Use boss bar for boss health
  chatAnnouncements: true         # Broadcast boss spawn/death in chat

  # Storm zone spawn weights
  spawnZones:
    stormCore:
      minDistance: 0
      maxDistance: 50
      weight: 60
    stormPeriphery:
      minDistance: 50
      maxDistance: 150
      weight: 30
    stormInfluence:
      minDistance: 150
      maxDistance: 300
      weight: 10

  # Density-based spawning
  density:
    checkInterval: 30             # Seconds between density checks
    baseChance: 0.05              # 5% base spawn chance
    playerMultiplier: 0.1         # Additional chance per player (logarithmic)
    maxChance: 0.5                # Max 50% spawn chance

  # Event type configs
  types:
    STORM_SURGE:
      enabled: true
      weight: 30
      minPlayers: 1
      requiresStorm: true
      cooldown: 300                # 5 minutes
      duration: 120                # 2 minutes
      crystalCount: 5
      essenceReward: 50

    TEMPEST_GUARDIAN:
      enabled: true
      weight: 15
      minPlayers: 3
      requiresStorm: true
      cooldown: 1800               # 30 minutes
      mythicMobType: "TempestGuardian"  # MythicMobs mob name
      essenceReward: 200

    STORM_RIFT:
      enabled: true
      weight: 20
      minPlayers: 2
      requiresStorm: true
      cooldown: 900                # 15 minutes
      duration: 300                # 5 minutes
      waveCount: 5
      mobsPerWave: 10
      essenceReward: 150

    STORM_TITAN:
      enabled: true
      weight: 5
      minPlayers: 10
      requiresStorm: true
      stormIntensityMin: 80
      cooldown: 7200               # 2 hours
      mythicMobType: "StormTitan"
      essenceReward: 1000

    TOWN_SIEGE:
      enabled: true
      weight: 10
      minPlayers: 2                # Must be 2+ players in/near town
      requiresStorm: true          # Storm must be nearby
      stormProximity: 900          # Storm within 900 blocks (nearby storm)
      baseChance: 0.02             # 2% chance per check
      cooldown: 3600               # 1 hour per town
      duration: 600                # 10 minutes
      waveCount: 3
      mobsPerWave: 15
      allowOptOut: true            # Towns can disable via permission
      mobsCanBreakBlocks: true     # Allow mobs to break blocks during siege

# rewards.yml
rewards:
  participation:
    essence: 10                    # Essence per player who participated
    money: 50                      # Vault economy reward

  completion:
    essence: 50
    money: 200

  bosses:
    rewardType: "DAMAGE_SHARE"     # Split rewards based on damage dealt
    totalEssencePool: 1000         # Total essence to distribute
    minDamagePercent: 5.0          # Must deal 5% damage to get rewards

  mvp:                             # Top damage dealer
    essenceBonus: 100              # Extra essence for top contributor
    moneyBonus: 500
    broadcast: true

# towns.yml (Towny/TAN integration)
towns:
  enabled: true
  siegeSettings:
    announceToTown: true
    announceRadius: 500
    defenseBonus: 1.2              # 20% bonus rewards for defending home town
    allowNeutralHelp: true         # Non-town members can help defend

  mobTypes:                        # What spawns during sieges
    - ZOMBIE
    - SKELETON
    - CREEPER
    - SPIDER
    - WITCH

  blockBreaking:
    enabled: true                  # Allow mobs to break blocks
    cooldown: 5                    # Seconds between block breaks per mob
    breakableBlocks:               # What blocks can be broken
      - WOODEN_DOOR
      - OAK_DOOR
      - SPRUCE_DOOR
      - BIRCH_DOOR
      - JUNGLE_DOOR
      - ACACIA_DOOR
      - DARK_OAK_DOOR
      - IRON_DOOR
      - WOODEN_TRAPDOOR
      - IRON_TRAPDOOR
      - CHEST
      - BARREL
      - FURNACE
      - CRAFTING_TABLE
    protectedBlocks:               # What blocks cannot be broken
      - BEDROCK
      - BARRIER
      - OBSIDIAN
      - ENCHANTING_TABLE
      - ANVIL
    explosionDamage: true          # Creepers can damage blocks

  spawnLocations:                  # Where mobs spawn around town
    minDistance: 30                # Not too close to spawn
    maxDistance: 80                # Not too far
    spawnPoints: 4                 # 4 spawn points around town
```

## Integration Notes

### Stormcraft Integration
```java
// Get active storms
StormcraftPlugin stormcraft = getStormcraft();
List<Storm> activeStorms = stormcraft.getStormManager().getActiveStorms();

// Check storm intensity
for (Storm storm : activeStorms) {
    int intensity = storm.getIntensity();
    Location epicenter = storm.getEpicenter();
    double radius = storm.getRadius();
}
```

### MythicMobs Integration
```java
// Spawn a MythicMobs boss
MythicMob mob = MythicBukkit.inst().getMobManager().getMythicMob("StormTitan");
if (mob.isPresent()) {
    ActiveMob activeMob = mob.get().spawn(BukkitAdapter.adapt(location), level);
    // Track this boss in our event system
}

// IMPORTANT: Don't use MythicMobs' built-in boss bars if it conflicts
// We'll use our own boss bar system for event progress
```

### Towny Integration
```java
// Detect towns near storm
TownyAPI towny = TownyAPI.getInstance();
List<Town> nearbyTowns = new ArrayList<>();

for (Town town : towny.getTowns()) {
    Location townSpawn = town.getSpawn();
    if (townSpawn.distance(stormEpicenter) < stormProximity) {
        // Check if town has opt-out permission
        if (!town.hasPermission("stormcraft.events.siege.exempt")) {
            nearbyTowns.add(town);
        }
    }
}
```

### TownsAndNations Integration
```java
// Similar to Towny but different API
TerritoryUtil territoryUtil = TerritoryUtil.getTerritory(chunk);
if (territoryUtil.isOwned()) {
    ITerritoryData territory = territoryUtil.getTerritoryData();
    // Check distance to storm, spawn siege
}
```

## Boss Bar Usage

**For Boss Health:**
- Bosses use boss bars to show their health
- Use `BarColor.RED` for boss health (distinct from storm purple bars)
- Use `BarColor.PURPLE` for event progress (siege waves, rift defense)
- Keep titles distinct: `[BOSS]` or `[SIEGE]` prefix

```java
// Example: Boss health bar
BossBar bossBar = Bukkit.createBossBar(
    new NamespacedKey(plugin, "boss_" + boss.getUniqueId()),
    "§c[BOSS] §fStorm Titan §c❤ 100%",
    BarColor.RED,
    BarStyle.SOLID
);

// Example: Event progress bar
BossBar eventBar = Bukkit.createBossBar(
    new NamespacedKey(plugin, "event_" + event.getId()),
    "§5[SIEGE] §fWave 2/3",
    BarColor.PURPLE,
    BarStyle.SEGMENTED_10
);
```

## Action Bar Usage

**Multiple Action Bars:**
- Action bars can only show **one message at a time** per player
- **Priority System**:
  1. **Boss events** override Essence ability messages during active boss fights
  2. **Regular events** don't override (use chat/titles instead)
  3. After boss defeat, Essence resumes normal action bar usage

```java
// Boss event action bar (overrides Essence)
player.sendActionBar(Component.text("§c⚔ Storm Titan §7| §e45% Health §7| §a+250 Damage"));

// Regular event (use chat instead)
player.sendMessage("§6[Event] §fStorm Surge: §e3/5 §fcrystals collected");
```

## Chat Announcements

**Boss Events:**
```
§c§l⚡ STORM TITAN §r§chas spawned at §e-1234, 64, 5678§c!
§7A massive storm creature has emerged from the tempest...

§a§l⚔ STORM TITAN §r§ahas been defeated!
§7Top Contributors:
§e  1. PlayerName §7- §f1,245 damage §7(§6+450 Essence§7)
§e  2. Player2 §7- §f890 damage §7(§6+320 Essence§7)
§e  3. Player3 §7- §f654 damage §7(§6+235 Essence§7)
```

**Regular Events:**
```
§6[Storm Event] §fA §bStorm Rift §fhas opened near you! §7(120 blocks away)
§6[Storm Event] §fStorm Rift §aclosed§f! All participants rewarded.
```

## Damage Tracking System

**For Boss Events:**
```java
public class DamageTracker {
    // Track damage per player
    private Map<UUID, Double> damageDealt = new HashMap<>();

    // Record damage when player hits boss
    public void recordDamage(Player player, double damage) {
        damageDealt.merge(player.getUniqueId(), damage, Double::sum);
    }

    // Calculate rewards based on damage share
    public Map<UUID, Integer> calculateRewards(int totalEssencePool) {
        double totalDamage = damageDealt.values().stream()
            .mapToDouble(Double::doubleValue).sum();

        Map<UUID, Integer> rewards = new HashMap<>();
        for (Map.Entry<UUID, Double> entry : damageDealt.entrySet()) {
            double damagePercent = (entry.getValue() / totalDamage) * 100;

            // Must deal at least 5% damage to get rewards
            if (damagePercent >= 5.0) {
                int essence = (int) ((entry.getValue() / totalDamage) * totalEssencePool);
                rewards.put(entry.getKey(), essence);
            }
        }

        return rewards;
    }

    // Get top 3 contributors for announcements
    public List<Map.Entry<UUID, Double>> getTopContributors(int limit) {
        return damageDealt.entrySet().stream()
            .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
}
```

**EntityDamageByEntityEvent Listener:**
```java
@EventHandler
public void onEntityDamage(EntityDamageByEntityEvent event) {
    // Check if damaged entity is a boss
    if (isBossEntity(event.getEntity())) {
        Player damager = getDamager(event);
        if (damager != null) {
            double damage = event.getFinalDamage();
            damageTracker.recordDamage(damager, damage);

            // Update action bar with damage dealt
            damager.sendActionBar(Component.text(
                "§c⚔ " + getBossName() + " §7| §a+" +
                String.format("%.0f", damage) + " Damage"
            ));
        }
    }
}
```

## Event Lifecycle

1. **Density Check** → Calculate player clustering near storms
2. **Spawn Decision** → Weighted random selection based on conditions
3. **Event Spawn** → Create event, notify nearby players, spawn entities, announce in chat
4. **Active Phase** → Track objectives, damage tracking for bosses, action bar updates
5. **Boss Death** → Calculate damage shares, distribute essence, announce top contributors
6. **Completion** → Award rewards, cleanup entities, set cooldown
7. **Cooldown** → Prevent same event from spawning too soon

## Development Guidelines

### Event Creation Checklist
- [ ] Extend `Event.java` base class
- [ ] Define objectives in `onStart()`
- [ ] Implement `onPlayerInteract()` for custom interactions
- [ ] Set appropriate spawn zones (core/periphery/influence)
- [ ] Add config entries in `config.yml`
- [ ] Test with 1 player, 3 players, 10+ players
- [ ] Verify rewards are balanced
- [ ] Check for conflicts with existing plugins

### Performance Considerations
- **Async where possible**: Density calculations, spawn checks
- **Limit entity count**: Cap mobs per event to prevent lag
- **Cleanup on failure**: Remove entities if event fails or expires
- **Efficient tracking**: Use maps with UUIDs, not linear searches

### Storm Zone Calculation
```java
public SpawnZone getRandomSpawnZone(Storm storm) {
    Location epicenter = storm.getEpicenter();
    int random = ThreadLocalRandom.current().nextInt(100);

    if (random < 60) {
        // Storm Core (60% chance)
        return new SpawnZone(epicenter, 0, 50);
    } else if (random < 90) {
        // Storm Periphery (30% chance)
        return new SpawnZone(epicenter, 50, 150);
    } else {
        // Storm Influence (10% chance)
        return new SpawnZone(epicenter, 150, 300);
    }
}
```

## Testing Scenarios

1. **Solo Player**: Can they complete Storm Surge alone?
2. **Group (3-5)**: Does Tempest Guardian spawn appropriately?
3. **Large Group (10+)**: Storm Titan spawn + multiple events
4. **Town Siege**: Mobs spawn around town, not inside walls
5. **No Storm**: Events should not spawn (except testing mode)
6. **Cooldown**: Events respect cooldowns, don't spam
7. **Conflict Test**: Boss bars don't override storm bars
8. **Essence Rewards**: Integration with Stormcraft-Essence works

## Known Limitations

- Events tied to storms, so players must be near storms to participate
- MythicMobs is optional but required for boss events
- Towny/TAN integration is optional, siege events disabled without them
- Boss bars may conflict if too many events active simultaneously (limit 1-2 concurrent events)

## Future Expansion Ideas

- **Event Chains**: Complete Storm Surge → unlocks Tempest Guardian
- **Leaderboards**: Track event completions per player
- **Seasonal Events**: Special events during in-game seasons
- **Custom Loot Tables**: Unique drops from event mobs
- **Event Dungeons**: Multi-stage events with boss at the end
- **Cross-Server Events**: If you ever add a network

## Quick Start for Development

1. Check if storm is active: `stormcraft.getStormManager().hasActiveStorms()`
2. Get storm zones: `StormZoneCalculator.calculateZones(storm)`
3. Check player density: `DensityTracker.getPlayersInZone(zone)`
4. Spawn event: `EventSpawner.spawn(EventType.STORM_SURGE, zone)`
5. Track progress: `ObjectiveTracker.track(event, player, objective)`
6. Complete event: `event.complete()` → award rewards, cleanup

## References

- **Stormcraft API**: `/var/repos/Stormcraft/src/main/java/dev/ked/stormcraft/`
- **MythicMobs API**: https://git.lumine.io/mythiccraft/MythicMobs/-/wikis/home
- **Towny API**: https://github.com/TownyAdvanced/Towny/wiki/API
- **TownsAndNations API**: (check JAR decompilation or ask server owner)

---

**Last Updated**: 2025-10-01
**Author**: Claude Code
**Status**: Ready for implementation
