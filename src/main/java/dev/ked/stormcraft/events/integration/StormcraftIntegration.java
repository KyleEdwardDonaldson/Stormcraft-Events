package dev.ked.stormcraft.events.integration;

import dev.ked.stormcraft.StormcraftPlugin;
import dev.ked.stormcraft.model.TravelingStorm;
import dev.ked.stormcraft.schedule.StormManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Integration with the core Stormcraft plugin.
 * Provides access to active storms and storm data.
 */
public class StormcraftIntegration {
    private final Plugin eventsPlugin;
    private StormcraftPlugin stormcraft;
    private boolean enabled = false;

    public StormcraftIntegration(Plugin eventsPlugin) {
        this.eventsPlugin = eventsPlugin;
        initialize();
    }

    private void initialize() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Stormcraft");
        if (plugin instanceof StormcraftPlugin) {
            this.stormcraft = (StormcraftPlugin) plugin;
            this.enabled = true;
            eventsPlugin.getLogger().info("Stormcraft integration initialized");
        }
    }

    /**
     * Get all active traveling storms.
     */
    public List<TravelingStorm> getActiveStorms() {
        if (!enabled || stormcraft == null) {
            return new ArrayList<>();
        }

        StormManager manager = stormcraft.getStormManager();
        if (manager != null) {
            return manager.getActiveStorms();
        }
        return new ArrayList<>();
    }

    /**
     * Check if any storms are currently active.
     */
    public boolean hasActiveStorms() {
        return !getActiveStorms().isEmpty();
    }

    /**
     * Get the nearest storm to a location.
     */
    public TravelingStorm getNearestStorm(Location location) {
        List<TravelingStorm> storms = getActiveStorms();
        if (storms.isEmpty()) return null;

        TravelingStorm nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (TravelingStorm storm : storms) {
            Location epicenter = storm.getCurrentLocation();
            if (epicenter.getWorld() != location.getWorld()) continue;

            double distance = epicenter.distance(location);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = storm;
            }
        }

        return nearest;
    }

    /**
     * Get storms within a certain distance of a location.
     */
    public List<TravelingStorm> getStormsNear(Location location, double maxDistance) {
        List<TravelingStorm> nearbyStorms = new ArrayList<>();

        for (TravelingStorm storm : getActiveStorms()) {
            Location epicenter = storm.getCurrentLocation();
            if (epicenter.getWorld() != location.getWorld()) continue;

            if (epicenter.distance(location) <= maxDistance) {
                nearbyStorms.add(storm);
            }
        }

        return nearbyStorms;
    }

    /**
     * Get storm intensity (0-100).
     */
    public int getStormIntensity(TravelingStorm storm) {
        // Storm intensity could be based on remaining lifetime, size, etc.
        // For now, return a value based on damage radius
        double radius = storm.getDamageRadius();
        return (int) Math.min(100, (radius / 3.0) * 100);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public StormcraftPlugin getStormcraft() {
        return stormcraft;
    }
}
