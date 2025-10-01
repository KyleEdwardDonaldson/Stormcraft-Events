package dev.ked.stormcraft.events.spawn;

import dev.ked.stormcraft.events.StormcraftEventsPlugin;
import dev.ked.stormcraft.events.config.ConfigManager;
import dev.ked.stormcraft.events.event.Event;
import dev.ked.stormcraft.events.event.EventType;
import dev.ked.stormcraft.events.event.events.*;
import dev.ked.stormcraft.events.integration.*;
import dev.ked.stormcraft.model.TravelingStorm;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles weighted random spawning of events.
 */
public class EventSpawner {
    private final StormcraftEventsPlugin plugin;
    private final ConfigManager config;
    private final StormcraftIntegration stormcraft;
    private final MythicMobsIntegration mythicMobs;
    private final TownyIntegration towny;
    private final TownsAndNationsIntegration tan;
    private final EssenceIntegration essence;
    private final Economy economy;
    private final StormZoneCalculator zoneCalculator;
    private final DensityTracker densityTracker;

    public EventSpawner(StormcraftEventsPlugin plugin, ConfigManager config,
                       StormcraftIntegration stormcraft, MythicMobsIntegration mythicMobs,
                       TownyIntegration towny, TownsAndNationsIntegration tan,
                       EssenceIntegration essence, Economy economy,
                       DensityTracker densityTracker) {
        this.plugin = plugin;
        this.config = config;
        this.stormcraft = stormcraft;
        this.mythicMobs = mythicMobs;
        this.towny = towny;
        this.tan = tan;
        this.essence = essence;
        this.economy = economy;
        this.zoneCalculator = new StormZoneCalculator(config);
        this.densityTracker = densityTracker;
    }

    /**
     * Attempt to spawn an event based on conditions and weights.
     * @return The spawned event, or null if spawn failed
     */
    public Event trySpawnEvent(TravelingStorm storm) {
        // Get random spawn zone
        SpawnZone zone = zoneCalculator.getRandomSpawnZone(storm);
        Location spawnLoc = zone.getRandomLocation();

        // Select event type based on weights
        EventType type = selectEventType(spawnLoc, storm);
        if (type == null) return null;

        // Check if event can spawn
        if (!canSpawnEvent(type, spawnLoc, storm)) return null;

        // Create event instance
        return createEvent(type, spawnLoc, storm);
    }

    /**
     * Select an event type based on weighted random selection.
     */
    private EventType selectEventType(Location location, TravelingStorm storm) {
        int totalWeight = 0;

        // Calculate total weight of eligible events
        for (EventType type : EventType.values()) {
            if (config.isEventEnabled(type) && meetsRequirements(type, location, storm)) {
                totalWeight += config.getEventWeight(type);
            }
        }

        if (totalWeight == 0) return null;

        // Random selection
        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        int current = 0;

        for (EventType type : EventType.values()) {
            if (!config.isEventEnabled(type) || !meetsRequirements(type, location, storm)) continue;

            current += config.getEventWeight(type);
            if (random < current) {
                return type;
            }
        }

        return null;
    }

    /**
     * Check if an event meets basic requirements.
     */
    private boolean meetsRequirements(EventType type, Location location, TravelingStorm storm) {
        // Check min players
        int minPlayers = config.getMinPlayers(type);
        if (!densityTracker.hasMinPlayers(location, minPlayers, 100)) {
            return false;
        }

        // Check storm intensity for certain events
        if (type == EventType.STORM_TITAN) {
            int minIntensity = config.getMinStormIntensity(type);
            int intensity = stormcraft.getStormIntensity(storm);
            if (intensity < minIntensity) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if an event can spawn (cooldowns, etc).
     */
    private boolean canSpawnEvent(EventType type, Location location, TravelingStorm storm) {
        // Check requirements
        if (!meetsRequirements(type, location, storm)) return false;

        // Additional checks can be added here (cooldowns, etc)

        return true;
    }

    /**
     * Create an event instance.
     */
    private Event createEvent(EventType type, Location location, TravelingStorm storm) {
        return switch (type) {
            case STORM_SURGE -> new StormSurgeEvent(plugin, config, location, storm);
            case TEMPEST_GUARDIAN -> new TempestGuardianEvent(plugin, config, mythicMobs, location, storm);
            case STORM_RIFT -> new StormRiftEvent(plugin, config, location, storm);
            case STORM_TITAN -> new StormTitanEvent(plugin, config, mythicMobs, location, storm);
            case TOWN_SIEGE -> new TownSiegeEvent(plugin, config, towny, tan, location, storm);
        };
    }

    public StormZoneCalculator getZoneCalculator() {
        return zoneCalculator;
    }
}
