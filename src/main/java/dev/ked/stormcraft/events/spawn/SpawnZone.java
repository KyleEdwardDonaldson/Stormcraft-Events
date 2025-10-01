package dev.ked.stormcraft.events.spawn;

import org.bukkit.Location;

/**
 * Represents a spawn zone around a storm.
 */
public class SpawnZone {
    private final Location center;
    private final double minDistance;
    private final double maxDistance;
    private final ZoneType type;

    public SpawnZone(Location center, double minDistance, double maxDistance, ZoneType type) {
        this.center = center;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.type = type;
    }

    /**
     * Get a random location within this zone.
     */
    public Location getRandomLocation() {
        double angle = Math.random() * 2 * Math.PI;
        double distance = minDistance + (Math.random() * (maxDistance - minDistance));

        double x = center.getX() + (distance * Math.cos(angle));
        double z = center.getZ() + (distance * Math.sin(angle));

        // Get safe Y coordinate (highest solid block)
        Location loc = new Location(center.getWorld(), x, center.getY(), z);
        loc.setY(loc.getWorld().getHighestBlockYAt(loc) + 1);

        return loc;
    }

    /**
     * Check if a location is within this zone.
     */
    public boolean contains(Location location) {
        if (location.getWorld() != center.getWorld()) return false;

        double distance = location.distance(center);
        return distance >= minDistance && distance <= maxDistance;
    }

    public Location getCenter() {
        return center;
    }

    public double getMinDistance() {
        return minDistance;
    }

    public double getMaxDistance() {
        return maxDistance;
    }

    public ZoneType getType() {
        return type;
    }

    public enum ZoneType {
        STORM_CORE,       // 0-50 blocks
        STORM_PERIPHERY,  // 50-150 blocks
        STORM_INFLUENCE   // 150-300 blocks
    }
}
