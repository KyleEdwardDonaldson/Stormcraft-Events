# Stormcraft-Events

A dynamic event system for Minecraft Paper 1.21.3 - 1.21.9 that spawns storm-themed objectives and encounters based on player density, storm activity, and town presence.

## Features

### Event Types

- **Storm Surge Retrieval** - Collect storm crystals before they disappear
- **Tempest Guardian** - Mini-boss spawns near storms with 3+ players
- **Storm Rift Defense** - Defend against waves of storm-corrupted mobs
- **Storm Titan** - World boss requiring 10+ coordinated players
- **Town Siege** - Mob waves attack towns during/after storms

### Smart Spawn System

Events spawn in three zones around active storms:
- **Storm Core** (0-50 blocks) - 60% spawn weight, highest difficulty
- **Storm Periphery** (50-150 blocks) - 30% spawn weight, moderate difficulty
- **Storm Influence** (150-300 blocks) - 10% spawn weight, accessible to lower-level players

### Density-Based Spawning

- Higher spawn chance with more players nearby
- Logarithmic scaling prevents event spam
- Per-event-type cooldowns ensure variety

### Town Integration

- Supports Towny and TownsAndNations
- Storm sieges spawn near towns during storms
- Low frequency with cooldowns
- Towns can opt-out via permissions

## Requirements

- **Java**: 21+
- **Minecraft**: Paper 1.21.3 - 1.21.9
- **Dependencies**:
  - Stormcraft (required)
  - MythicMobs (optional, for boss events)
  - Towny or TownsAndNations (optional, for town sieges)

## Installation

1. Download the latest release from [Releases](https://github.com/yourusername/Stormcraft-Events/releases)
2. Place `Stormcraft-Events.jar` in your server's `plugins/` folder
3. Install required dependency: Stormcraft
4. (Optional) Install MythicMobs for boss events
5. (Optional) Install Towny/TownsAndNations for town sieges
6. Restart your server
7. Configure `plugins/StormcraftEvents/config.yml` to your preferences

## Configuration

### Main Config (`config.yml`)

```yaml
events:
  globalCooldown: 60              # Seconds between any events
  announceRadius: 900             # Announce events within this radius

  density:
    checkInterval: 30             # Check for events every 30 seconds
    baseChance: 0.05              # 5% base spawn chance
    playerMultiplier: 0.1         # Bonus chance per player
    maxChance: 0.5                # Maximum 50% spawn chance

  types:
    STORM_SURGE:
      enabled: true
      weight: 30
      minPlayers: 1
      cooldown: 300               # 5 minutes
      duration: 120               # 2 minutes
      essenceReward: 50
```

### Rewards Config (`rewards.yml`)

```yaml
rewards:
  bosses:
    rewardType: "DAMAGE_SHARE"    # Rewards based on damage dealt
    totalEssencePool: 1000
    minDamagePercent: 5.0         # Must deal 5% damage for rewards

  mvp:
    essenceBonus: 100             # Extra for top contributor
    broadcast: true
```

### Town Config (`towns.yml`)

```yaml
towns:
  enabled: true
  siegeSettings:
    announceToTown: true
    defenseBonus: 1.2             # 20% bonus for defending home town

  blockBreaking:
    enabled: true                 # Mobs can break certain blocks
    explosionDamage: true         # Creepers damage blocks
```

## Permissions

- `stormcraft.events.siege.exempt` - Exempt town from sieges
- `stormcraft.events.admin` - Access to admin commands
- `stormcraft.events.notify` - Receive event notifications

## Commands

- `/events list` - List active events
- `/events info <event>` - Get details about an event
- `/events reload` - Reload configuration
- `/events spawn <type>` - Manually spawn an event (admin)
- `/events stats` - View your event statistics

## Integration

### MythicMobs Setup

Create mob configurations in `MythicMobs/Mobs/`:

```yaml
TempestGuardian:
  Type: ZOMBIE
  Display: '&5Tempest Guardian'
  Health: 500
  Damage: 15
  Skills:
    - lightning @target ~onAttack

StormTitan:
  Type: GIANT
  Display: '&c&lSTORM TITAN'
  Health: 10000
  Damage: 30
  Skills:
    - lightning @PIR{r=10} ~onTimer:100
    - throw @target ~onAttack
```

### Towny/TownsAndNations

Events automatically detect nearby towns. To exempt a town:

```bash
/town set perm stormcraft.events.siege.exempt on
```

## Development

### Building from Source

```bash
git clone https://github.com/yourusername/Stormcraft-Events.git
cd Stormcraft-Events
mvn clean package
```

The compiled JAR will be in `target/`.

### Project Structure

```
src/main/java/dev/ked/stormcraft/events/
├── event/          # Event types and management
├── spawn/          # Zone calculation and spawning
├── integration/    # Plugin integrations
├── objectives/     # Event objective tracking
├── ui/             # Player feedback (action bars, boss bars)
└── config/         # Configuration management
```

### Creating Custom Events

Extend the `Event` base class:

```java
public class CustomEvent extends Event {
    @Override
    public void onStart() {
        // Spawn entities, set objectives
    }

    @Override
    public void onComplete() {
        // Award rewards, cleanup
    }
}
```

Register in `EventType.java` and add configuration.

## Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/Stormcraft-Events/issues)
- **Wiki**: [Documentation](https://github.com/yourusername/Stormcraft-Events/wiki)
- **Discord**: [Join our community](#)

## License

This project is licensed under the MIT License - see [LICENSE](LICENSE) file for details.

## Credits

- **Author**: Kyle Edward Donaldson
- **Built with**: Stormcraft API, MythicMobs API, Towny API
- **Special thanks**: Paper team, MythicMobs developers

## Roadmap

- [ ] Event chains (completing one unlocks another)
- [ ] Player leaderboards
- [ ] Seasonal events
- [ ] Custom loot tables
- [ ] Multi-stage event dungeons
- [ ] Cross-server events

---

**Version**: 0.1.0
**Last Updated**: 2025-10-02
