package dev.ked.stormcraft.events.ui;

import dev.ked.stormcraft.events.config.ConfigManager;
import dev.ked.stormcraft.events.difficulty.DifficultyCalculator;
import dev.ked.stormcraft.events.difficulty.DifficultyMultiplier;
import dev.ked.stormcraft.events.difficulty.PlayerDensityTracker;
import dev.ked.stormcraft.events.integration.StormcraftIntegration;
import dev.ked.stormcraft.model.TravelingStorm;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * Displays real-time threat level information on player action bars.
 * Updates every 5 seconds while players are near active storms.
 */
public class ThreatLevelHUD extends BukkitRunnable {
    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final StormcraftIntegration stormcraft;
    private final PlayerDensityTracker densityTracker;
    private final DifficultyCalculator difficultyCalculator;

    public ThreatLevelHUD(JavaPlugin plugin, ConfigManager config,
                         StormcraftIntegration stormcraft,
                         PlayerDensityTracker densityTracker,
                         DifficultyCalculator difficultyCalculator) {
        this.plugin = plugin;
        this.config = config;
        this.stormcraft = stormcraft;
        this.densityTracker = densityTracker;
        this.difficultyCalculator = difficultyCalculator;
    }

    @Override
    public void run() {
        // Only update if difficulty system is enabled
        if (!config.isDifficultyEnabled()) {
            return;
        }

        // Check if any storms are active
        List<TravelingStorm> activeStorms = stormcraft.getActiveStorms();
        if (activeStorms.isEmpty()) {
            return;
        }

        // Update HUD for all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerHUD(player, activeStorms);
        }
    }

    /**
     * Update HUD for a specific player.
     */
    private void updatePlayerHUD(Player player, List<TravelingStorm> storms) {
        // Check if player is near any storm
        TravelingStorm nearestStorm = findNearestStorm(player, storms);
        if (nearestStorm == null) {
            return; // Not near any storm
        }

        // Get nearby players
        List<Player> nearbyPlayers = densityTracker.getNearbyPlayers(player.getLocation());
        if (nearbyPlayers.isEmpty()) {
            return; // No players nearby (shouldn't happen since player is in the list)
        }

        // Calculate difficulty
        DifficultyMultiplier difficulty = difficultyCalculator.calculate(player.getLocation(), nearbyPlayers);

        // Send action bar
        String message = difficulty.getCompactDisplay();
        player.sendActionBar(Component.text(message));
    }

    /**
     * Find the nearest storm to a player within detection range.
     */
    private TravelingStorm findNearestStorm(Player player, List<TravelingStorm> storms) {
        TravelingStorm nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        double detectionRange = config.getStormProximityRadius(); // Use storm proximity radius

        for (TravelingStorm storm : storms) {
            if (!storm.getCurrentLocation().getWorld().equals(player.getWorld())) {
                continue;
            }

            double distance = storm.getCurrentLocation().distance(player.getLocation());
            if (distance <= detectionRange && distance < nearestDistance) {
                nearestDistance = distance;
                nearest = storm;
            }
        }

        return nearest;
    }

    /**
     * Start the HUD updater task.
     * Updates every 5 seconds (100 ticks).
     */
    public void start() {
        int interval = config.getConfig().getInt("difficulty.scan_interval", 100);
        this.runTaskTimer(plugin, 100L, interval);
    }
}
