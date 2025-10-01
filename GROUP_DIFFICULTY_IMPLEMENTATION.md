# Group-Based Dynamic Difficulty System - Implementation Guide

**Version:** 1.0
**Status:** Not Implemented
**Estimated Time:** 8-12 hours
**Target Plugin:** Stormcraft-Events

---

## Overview

Implement a dynamic difficulty scaling system where event difficulty and boss spawn chances increase based on:
- Number of players in a party
- Player density in an area
- Whether players are in town claims (safe) or wilderness (dangerous)
- Proximity to active storms

**Goal:** Create risk/reward gameplay where groups in the wilderness attract harder bosses for better rewards.

---

## Architecture

### New Classes to Create:

```
dev.ked.stormcraft.events.difficulty/
├── DifficultyCalculator.java        # Main calculator for difficulty multipliers
├── DifficultyMultiplier.java        # Data class holding multiplier info
├── PlayerDensityTracker.java        # Tracks player proximity and groups
├── ThreatLevel.java                 # Enum: LOW, MEDIUM, HIGH, EXTREME
└── GroupRewardCalculator.java       # Calculates scaled rewards
```

### Classes to Modify:

```
dev.ked.stormcraft.events.event/
├── EventManager.java                # Use DifficultyCalculator for event selection
└── Event.java                       # Add difficulty multiplier field

dev.ked.stormcraft.events.spawn/
├── EventSpawner.java                # Apply difficulty scaling to spawn rates
└── DensityTracker.java              # Integrate with PlayerDensityTracker

dev.ked.stormcraft.events.integration/
├── TownyIntegration.java            # Check if location is in town claim
└── TownsAndNationsIntegration.java  # Check if location is in territory
```

---

## Implementation Phases

### **Phase 1: Core Difficulty System** (3-4 hours)

#### 1.1 Create ThreatLevel Enum
**File:** `ThreatLevel.java`

```java
public enum ThreatLevel {
    LOW(1.0, 1.5, "§aLow"),
    MEDIUM(1.5, 2.0, "§eMedium"),
    HIGH(2.0, 2.75, "§6High"),
    EXTREME(2.75, 10.0, "§cExtreme");

    private final double minMultiplier;
    private final double maxMultiplier;
    private final String displayName;

    // Constructor, getters, fromMultiplier() method
}
```

**Purpose:** Categorize difficulty for UI display

---

#### 1.2 Create DifficultyMultiplier Data Class
**File:** `DifficultyMultiplier.java`

```java
public class DifficultyMultiplier {
    private final double multiplier;
    private final int playerCount;
    private final int partyMembers;
    private final boolean inWilderness;
    private final boolean nearStorm;
    private final ThreatLevel threatLevel;

    // Constructor, getters, breakdown string for debug
}
```

**Purpose:** Hold all difficulty calculation results

---

#### 1.3 Create DifficultyCalculator
**File:** `DifficultyCalculator.java`

**Responsibilities:**
- Calculate base multiplier from player count
- Apply party bonus (+0.3 per party member, max 1.5x)
- Apply proximity bonus (+0.2 per nearby non-party player, max 1.0x)
- Apply wilderness bonus (+0.5x if not in town)
- Apply storm proximity bonus (+0.5x if within 300 blocks of storm)
- Determine threat level
- Select appropriate event type based on weighted chances

**Key Methods:**
```java
public DifficultyMultiplier calculate(Location location, List<Player> nearbyPlayers)
public EventType selectEventType(DifficultyMultiplier difficulty)
public double getRewardMultiplier(DifficultyMultiplier difficulty)
```

**Event Selection Weights:**

| Multiplier Range | Storm Surge | Storm Rift | Tempest Guardian | Storm Titan | Town Siege |
|------------------|-------------|------------|------------------|-------------|------------|
| 1.0 - 1.4 (LOW)  | 60%         | 30%        | 10%              | 0%          | 0%         |
| 1.5 - 1.9 (MED)  | 40%         | 35%        | 20%              | 5%          | 0%         |
| 2.0 - 2.7 (HIGH) | 20%         | 30%        | 35%              | 10%         | 5%         |
| 2.75+ (EXTREME)  | 10%         | 20%        | 40%              | 20%         | 10%        |

**Reward Scaling:**
```
Base Essence × (1 + (multiplier - 1.0) × 2.5)

Examples:
1.0x → 1.0x rewards (no change)
1.5x → 2.25x rewards
2.0x → 3.5x rewards
3.0x → 6.0x rewards
```

---

#### 1.4 Create PlayerDensityTracker
**File:** `PlayerDensityTracker.java`

**Responsibilities:**
- Scan for nearby players within configurable radius (default: 50 blocks)
- Detect party memberships
- Cache results for performance (refresh every 5 seconds)
- Provide helper methods for party detection

**Key Methods:**
```java
public List<Player> getNearbyPlayers(Location location, double radius)
public int getPartyMemberCount(Player player, List<Player> nearbyPlayers)
public boolean isInParty(Player player)
public Set<Player> getPartyMembers(Player player)
```

**Party Detection Strategy:**
- Check for Party plugin (if installed)
- Fallback: Check for players on same scoreboard team
- Fallback: Consider all nearby players as "loose group"

---

### **Phase 2: Integration with Existing Systems** (2-3 hours)

#### 2.1 Modify EventManager
**File:** `EventManager.java`

**Changes:**
1. Add DifficultyCalculator instance
2. When spawning events, calculate difficulty first:
   ```java
   Location spawnLoc = /* event location */;
   List<Player> nearby = playerDensityTracker.getNearbyPlayers(spawnLoc, 50);
   DifficultyMultiplier difficulty = difficultyCalculator.calculate(spawnLoc, nearby);
   EventType type = difficultyCalculator.selectEventType(difficulty);
   ```
3. Pass difficulty multiplier to event constructor
4. Log difficulty for debugging

---

#### 2.2 Modify Event Base Class
**File:** `Event.java`

**Changes:**
1. Add field: `protected DifficultyMultiplier difficulty;`
2. Add to constructor
3. Use multiplier for:
   - Boss HP scaling: `baseHP * difficulty.getMultiplier()`
   - Boss damage scaling: `baseDamage * (1 + (difficulty.getMultiplier() - 1) * 0.2)`
   - Reward scaling via GroupRewardCalculator

---

#### 2.3 Enhance TownyIntegration
**File:** `TownyIntegration.java`

**Add Method:**
```java
public boolean isInTownClaim(Location location) {
    if (!enabled) return false;
    // Use Towny API to check if location is in any town
    // Return true if claimed, false if wilderness
}
```

---

#### 2.4 Enhance TownsAndNationsIntegration
**File:** `TownsAndNationsIntegration.java`

**Add Method:**
```java
public boolean isInTerritory(Location location) {
    if (!enabled) return false;
    // Use TAN API to check if location is in territory
    // Return true if claimed, false if wilderness
}
```

---

### **Phase 3: Reward System** (2-3 hours)

#### 3.1 Create GroupRewardCalculator
**File:** `GroupRewardCalculator.java`

**Responsibilities:**
- Calculate scaled essence rewards
- Apply party completion bonus
- Split rewards among party members
- Track individual contributions (damage dealt)

**Key Methods:**
```java
public Map<Player, Double> calculateRewards(
    Event event,
    DifficultyMultiplier difficulty,
    Map<Player, Double> damageContributions
)
```

**Reward Formula:**
```
Step 1: Base Reward × Difficulty Multiplier
Step 2: + Party Bonus (10% per member, max 50%)
Step 3: Split among party (equal or damage-weighted)
Step 4: Individual bonuses (first completion, SEL multiplier)
```

**Example:**
- Event: Tempest Guardian
- Base Reward: 2000 essence
- Difficulty: 2.5x
- Party: 5 members
- Calculation:
  - Base scaled: 2000 × 5.75 = 11,500
  - Party bonus: 11,500 × 1.5 = 17,250
  - Per player: 17,250 / 5 = 3,450 essence each

---

#### 3.2 Modify Reward Distribution
**File:** Event implementations (TempestGuardianEvent, etc.)

**Changes:**
1. Use GroupRewardCalculator instead of direct essence awards
2. Send breakdown message to players:
   ```
   §a§l✓ Tempest Guardian Defeated!
   §7Difficulty: §6High (2.5x)
   §7Party: §f5 players
   §e+ 3,450 Essence §7(base: 2,000)
   §7  Difficulty: §e+5,750
   §7  Party Bonus: §e+5,750
   ```

---

### **Phase 4: UI & Feedback** (1-2 hours)

#### 4.1 Add Threat Level Display
**File:** `EventNotifier.java` or new `ThreatLevelHUD.java`

**Display on Action Bar:**
```
⚡ Storm Power: 73% | 👥 5 Players | 🌍 Wilderness | ⚠️ EXTREME
```

**Update every 5 seconds while near storm**

---

#### 4.2 Add Event Spawn Notifications
**When event spawns near players:**
```
[⚡ Storm Titan detected - 5 players nearby!]
[⚠️ Threat Level: EXTREME - Massive rewards available!]
```

---

#### 4.3 Add Debug Command
**File:** Add to `StormEventCommand.java`

```
/stormevent difficulty [radius]
```

**Output:**
```
§6[Storm Events] §fDifficulty Analysis:
§7Location: §fWilderness (X: 100, Z: -200)
§7Nearby Players: §f5 (Party: 3, Others: 2)
§7Base Multiplier: §e1.0x
§7  + Party Bonus: §e+0.6x (3 members)
§7  + Proximity: §e+0.4x (2 nearby)
§7  + Wilderness: §e+0.5x
§7  + Storm: §e+0.5x
§7Final Multiplier: §c3.0x
§7Threat Level: §c§lEXTREME
§7Event Chances:
§7  Storm Surge: §f10%
§7  Storm Rift: §f20%
§7  Tempest Guardian: §f40%
§7  Storm Titan: §c20%
§7  Town Siege: §c10%
```

---

### **Phase 5: Configuration** (1 hour)

#### 5.1 Add to config.yml

```yaml
difficulty:
  enabled: true

  # Player detection
  scan_radius: 50.0  # Blocks to scan for nearby players
  scan_interval: 100  # Ticks between scans (5 seconds)

  # Multiplier bonuses
  party_bonus_per_member: 0.3
  max_party_bonus: 1.5
  proximity_bonus_per_player: 0.2
  max_proximity_bonus: 1.0
  wilderness_bonus: 0.5
  storm_proximity_bonus: 0.5

  # Reward scaling
  reward_scaling_factor: 2.5  # How much multiplier affects rewards
  party_completion_bonus: 0.1  # 10% per party member
  max_party_completion_bonus: 0.5  # 50% max

  # Safe zones
  respect_town_claims: true  # Towny/TAN claims reduce difficulty
  town_claim_multiplier: 0.5  # Multiply final difficulty by this in towns

  # Event type weights by threat level
  weights:
    low:  # 1.0 - 1.4x
      storm_surge: 60
      storm_rift: 30
      tempest_guardian: 10
      storm_titan: 0
      town_siege: 0
    medium:  # 1.5 - 1.9x
      storm_surge: 40
      storm_rift: 35
      tempest_guardian: 20
      storm_titan: 5
      town_siege: 0
    high:  # 2.0 - 2.7x
      storm_surge: 20
      storm_rift: 30
      tempest_guardian: 35
      storm_titan: 10
      town_siege: 5
    extreme:  # 2.75+x
      storm_surge: 10
      storm_rift: 20
      tempest_guardian: 40
      storm_titan: 20
      town_siege: 10
```

---

### **Phase 6: Testing & Balance** (1-2 hours)

#### 6.1 Test Cases

**Solo Player in Town:**
- Expected: 1.0x multiplier, LOW threat
- Events: Mostly Storm Surge
- Verify: Town claim detection working

**Solo Player in Wilderness:**
- Expected: 1.5x multiplier, MEDIUM threat
- Events: Mixed Storm Surge/Rift
- Verify: Wilderness bonus applying

**3-Player Party in Wilderness:**
- Expected: 2.4x multiplier, HIGH threat
- Events: Mostly Tempest Guardian
- Verify: Party detection working

**5-Player Party in Storm Wilderness:**
- Expected: 3.5x multiplier, EXTREME threat
- Events: Storm Titan/Guardian heavy
- Verify: All bonuses stacking correctly

**Group at Town Border:**
- Expected: Reduced multiplier
- Verify: Town claim detection accurate

---

#### 6.2 Balance Testing

**Monitor:**
- Average essence gain per hour (solo vs group)
- Event difficulty feeling (too easy/hard?)
- Reward satisfaction
- Player behavior changes

**Adjust:**
- Multiplier bonus values
- Reward scaling factor
- Event weight distributions
- Boss HP/damage scaling

---

## Implementation Order

### Day 1 (4-5 hours):
1. ✅ Create ThreatLevel enum (15 min)
2. ✅ Create DifficultyMultiplier class (30 min)
3. ✅ Create PlayerDensityTracker (1.5 hours)
4. ✅ Create DifficultyCalculator (2 hours)

### Day 2 (3-4 hours):
5. ✅ Integrate with EventManager (1 hour)
6. ✅ Modify Event base class (30 min)
7. ✅ Enhance Towny/TAN integrations (1 hour)
8. ✅ Create GroupRewardCalculator (1 hour)

### Day 3 (2-3 hours):
9. ✅ Update event implementations (1 hour)
10. ✅ Add UI/HUD displays (1 hour)
11. ✅ Add debug command (30 min)

### Day 4 (1-2 hours):
12. ✅ Add configuration options (30 min)
13. ✅ Testing & balance (1.5 hours)

---

## Success Criteria

- [ ] Solo players in towns see LOW threat and easy events
- [ ] Groups in wilderness see increased difficulty
- [ ] Multiplier calculation is transparent (debug command shows breakdown)
- [ ] Rewards scale appropriately with difficulty
- [ ] Party members share rewards fairly
- [ ] Town claims properly reduce difficulty
- [ ] UI clearly communicates threat level
- [ ] No performance issues (async calculations where possible)
- [ ] Configuration allows server admins to tune balance
- [ ] Players form groups to hunt harder events

---

## Potential Issues & Solutions

### Issue: Performance with many players
**Solution:**
- Cache nearby player scans (5 second refresh)
- Async calculations for difficulty
- Only scan within render distance

### Issue: Party plugin not installed
**Solution:**
- Graceful fallback to scoreboard teams
- Or consider all nearby players as "loose group"

### Issue: Players exploit by standing in town borders
**Solution:**
- Check if majority of party is in wilderness
- Or use weighted average of player positions

### Issue: Rewards too high/low
**Solution:**
- Extensive config options for tuning
- Log reward amounts for analysis
- Monitor economy over time

### Issue: Boss too hard for calculated difficulty
**Solution:**
- Boss HP/damage scaling can be tuned separately
- Add "recommended players" to event notification
- Allow partial rewards for damage dealt even if boss not killed

---

## Future Enhancements

### Post-Launch Features:
1. **Guild Storm Alerts** - Notify guild when high-tier event spawns near members
2. **Seasonal Leaderboards** - Track guild/player performance
3. **Prestige Bonuses** - Higher SEL players get better rewards
4. **Weather-Based Scaling** - Thunder/rain increase difficulty
5. **Time-Based Events** - Harder events at night
6. **Achievement System** - "Defeated Storm Titan with 3 players"
7. **Challenge Modifiers** - Optional hard mode for even better rewards

---

## Notes for Implementation

- Use `@Async` where possible to avoid main thread lag
- Log all difficulty calculations to console (debug mode)
- Make config hot-reloadable
- Add metrics tracking (average multiplier, event distribution)
- Consider party plugin integration order (DungeonsXL, PartyAndFriends, etc.)
- Test with MythicMobs disabled (current state)
- Ensure works with future MythicMobs integration

---

**Status:** Ready for Implementation
**Next Step:** Begin Phase 1 - Core Difficulty System
