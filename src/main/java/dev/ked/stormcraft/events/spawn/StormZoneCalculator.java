package dev.ked.stormcraft.events.spawn;

import dev.ked.stormcraft.events.config.ConfigManager;
import dev.ked.stormcraft.model.TravelingStorm;
import org.bukkit.Location;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Calculates spawn zones around storms based on configuration.
 */
public class StormZoneCalculator {
    private final ConfigManager config;

    public StormZoneCalculator(ConfigManager config) {
        this.config = config;
    }

    /**
     * Get a random spawn zone based on configured weights.
     */
    public SpawnZone getRandomSpawnZone(TravelingStorm storm) {
        Location epicenter = storm.getCurrentLocation();
        int random = ThreadLocalRandom.current().nextInt(100);

        // Storm Core: 60% chance (0-50 blocks)
        if (random < 60) {
            return new SpawnZone(epicenter, 0, 50, SpawnZone.ZoneType.STORM_CORE);
        }
        // Storm Periphery: 30% chance (50-150 blocks)
        else if (random < 90) {
            return new SpawnZone(epicenter, 50, 150, SpawnZone.ZoneType.STORM_PERIPHERY);
        }
        // Storm Influence: 10% chance (150-300 blocks)
        else {
            return new SpawnZone(epicenter, 150, 300, SpawnZone.ZoneType.STORM_INFLUENCE);
        }
    }

    /**
     * Get a specific zone type.
     */
    public SpawnZone getZone(TravelingStorm storm, SpawnZone.ZoneType type) {
        Location epicenter = storm.getCurrentLocation();

        return switch (type) {
            case STORM_CORE -> new SpawnZone(epicenter, 0, 50, SpawnZone.ZoneType.STORM_CORE);
            case STORM_PERIPHERY -> new SpawnZone(epicenter, 50, 150, SpawnZone.ZoneType.STORM_PERIPHERY);
            case STORM_INFLUENCE -> new SpawnZone(epicenter, 150, 300, SpawnZone.ZoneType.STORM_INFLUENCE);
        };
    }

    /**
     * Get all zones for a storm.
     */
    public SpawnZone[] getAllZones(TravelingStorm storm) {
        return new SpawnZone[] {
            getZone(storm, SpawnZone.ZoneType.STORM_CORE),
            getZone(storm, SpawnZone.ZoneType.STORM_PERIPHERY),
            getZone(storm, SpawnZone.ZoneType.STORM_INFLUENCE)
        };
    }

    /**
     * Check if a location is near a storm (within any zone).
     */
    public boolean isNearStorm(Location location, TravelingStorm storm) {
        Location epicenter = storm.getCurrentLocation();
        if (epicenter.getWorld() != location.getWorld()) return false;

        double distance = epicenter.distance(location);
        return distance <= 300; // Max influence range
    }
}
