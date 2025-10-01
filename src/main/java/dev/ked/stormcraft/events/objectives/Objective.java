package dev.ked.stormcraft.events.objectives;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for event objectives.
 * Tracks progress for each participant.
 */
public abstract class Objective {
    protected final String description;
    protected final Map<UUID, Integer> progress = new HashMap<>();
    protected final int target;
    protected boolean completed = false;

    public Objective(String description, int target) {
        this.description = description;
        this.target = target;
    }

    /**
     * Update progress for a player.
     * @return true if objective is now complete
     */
    public boolean updateProgress(Player player, int amount) {
        UUID playerId = player.getUniqueId();
        int current = progress.getOrDefault(playerId, 0);
        int newProgress = current + amount;
        progress.put(playerId, newProgress);

        // Check if objective is complete
        if (newProgress >= target && !completed) {
            completed = true;
            return true;
        }

        return false;
    }

    /**
     * Get current progress for a player.
     */
    public int getProgress(UUID playerId) {
        return progress.getOrDefault(playerId, 0);
    }

    /**
     * Get progress percentage for a player (0-100).
     */
    public double getProgressPercent(UUID playerId) {
        if (target == 0) return 100.0;
        return Math.min(100.0, (getProgress(playerId) / (double) target) * 100.0);
    }

    /**
     * Check if objective is complete for a specific player.
     */
    public boolean isComplete(UUID playerId) {
        return getProgress(playerId) >= target;
    }

    /**
     * Check if objective is globally complete.
     */
    public boolean isComplete() {
        return completed;
    }

    /**
     * Get description with progress.
     */
    public String getProgressString(UUID playerId) {
        return description + ": " + getProgress(playerId) + "/" + target;
    }

    public String getDescription() {
        return description;
    }

    public int getTarget() {
        return target;
    }

    /**
     * Reset objective.
     */
    public void reset() {
        progress.clear();
        completed = false;
    }
}
