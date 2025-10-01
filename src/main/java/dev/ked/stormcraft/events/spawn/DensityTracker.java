package dev.ked.stormcraft.events.spawn;

import dev.ked.stormcraft.events.StormcraftEventsPlugin;
import dev.ked.stormcraft.events.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Tracks player density in different areas to determine event spawn chances.
 * Uses logarithmic scaling to prevent spam with many players.
 */
public class DensityTracker {
    private final StormcraftEventsPlugin plugin;
    private final ConfigManager config;
    private final Map<Location, Integer> densityMap = new HashMap<>();
    private BukkitTask trackingTask;

    public DensityTracker(StormcraftEventsPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Start tracking player density.
     */
    public void start() {
        int interval = config.getDensityCheckInterval() * 20; // Convert to ticks

        trackingTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateDensity, 0L, interval);
        plugin.getLogger().info("Density tracker started (interval: " + config.getDensityCheckInterval() + "s)");
    }

    /**
     * Stop tracking.
     */
    public void stop() {
        if (trackingTask != null) {
            trackingTask.cancel();
            trackingTask = null;
        }
    }

    /**
     * Update density map based on current player positions.
     */
    private void updateDensity() {
        densityMap.clear();

        // Grid size for grouping players (in blocks)
        final int GRID_SIZE = 50;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!config.isWorldEnabled(player.getWorld().getName())) continue;

            Location loc = player.getLocation();

            // Round to grid
            int gridX = (int) (Math.floor(loc.getX() / GRID_SIZE) * GRID_SIZE);
            int gridZ = (int) (Math.floor(loc.getZ() / GRID_SIZE) * GRID_SIZE);

            Location gridLoc = new Location(loc.getWorld(), gridX, 0, gridZ);

            densityMap.merge(gridLoc, 1, Integer::sum);
        }
    }

    /**
     * Get player count near a location (within 100 blocks).
     */
    public int getPlayersNear(Location location, double radius) {
        int count = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() != location.getWorld()) continue;

            if (player.getLocation().distance(location) <= radius) {
                count++;
            }
        }

        return count;
    }

    /**
     * Calculate spawn chance based on player density.
     * Uses logarithmic scaling to prevent spam.
     */
    public double calculateSpawnChance(Location location, double baseChance) {
        int playerCount = getPlayersNear(location, 100);

        if (playerCount == 0) return 0;

        // Logarithmic scaling: log10(players + 1) * multiplier
        double multiplier = config.getPlayerMultiplier();
        double bonus = Math.log10(playerCount + 1) * multiplier;

        double totalChance = baseChance + bonus;

        // Cap at max chance
        return Math.min(totalChance, config.getMaxSpawnChance());
    }

    /**
     * Get players in a specific zone.
     */
    public List<Player> getPlayersInZone(SpawnZone zone) {
        List<Player> players = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (zone.contains(player.getLocation())) {
                players.add(player);
            }
        }

        return players;
    }

    /**
     * Check if enough players are near for an event to spawn.
     */
    public boolean hasMinPlayers(Location location, int minPlayers, double radius) {
        return getPlayersNear(location, radius) >= minPlayers;
    }

    /**
     * Get all high-density areas (for debug/admin commands).
     */
    public Map<Location, Integer> getDensityMap() {
        return new HashMap<>(densityMap);
    }
}
