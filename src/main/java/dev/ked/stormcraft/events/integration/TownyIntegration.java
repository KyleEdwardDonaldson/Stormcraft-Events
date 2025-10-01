package dev.ked.stormcraft.events.integration;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import dev.ked.stormcraft.events.config.ConfigManager;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Integration with Towny for town siege events.
 */
public class TownyIntegration {
    private final Plugin plugin;
    private final ConfigManager config;
    private boolean enabled = false;

    public TownyIntegration(Plugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        try {
            // Test if Towny API is available
            TownyAPI.getInstance();
            this.enabled = true;
            plugin.getLogger().info("Towny integration initialized");
        } catch (Exception e) {
            plugin.getLogger().warning("Towny found but API unavailable: " + e.getMessage());
        }
    }

    /**
     * Get all towns near a location within a certain distance.
     */
    public List<Town> getTownsNear(Location location, double maxDistance) {
        if (!enabled) return new ArrayList<>();

        List<Town> nearbyTowns = new ArrayList<>();
        TownyAPI towny = TownyAPI.getInstance();

        Collection<Town> allTowns = towny.getTowns();
        for (Town town : allTowns) {
            try {
                Location townSpawn = town.getSpawn();
                if (townSpawn == null) continue;
                if (townSpawn.getWorld() != location.getWorld()) continue;

                double distance = townSpawn.distance(location);
                if (distance <= maxDistance) {
                    nearbyTowns.add(town);
                }
            } catch (Exception e) {
                // Town might not have spawn set
                continue;
            }
        }

        return nearbyTowns;
    }

    /**
     * Check if a town has opted out of siege events.
     */
    public boolean hasOptedOut(Town town) {
        if (!enabled) return false;

        // Check if town has the opt-out permission
        // This would typically be set via a permission plugin or Towny metadata
        // For now, we'll check if sieges are disabled in config
        return !config.isSiegeEnabled();
    }

    /**
     * Get the town at a specific location.
     */
    public Town getTownAt(Location location) {
        if (!enabled) return null;

        try {
            TownyAPI towny = TownyAPI.getInstance();
            return towny.getTown(location);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get number of online residents in a town.
     */
    public int getOnlineResidents(Town town) {
        if (!enabled) return 0;

        try {
            return (int) town.getResidents().stream()
                    .filter(resident -> resident.isOnline())
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Check if a town exists.
     */
    public boolean townExists(String townName) {
        if (!enabled) return false;

        try {
            TownyAPI towny = TownyAPI.getInstance();
            return towny.getTown(townName) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}
