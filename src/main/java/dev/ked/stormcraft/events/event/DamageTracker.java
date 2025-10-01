package dev.ked.stormcraft.events.event;

import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tracks damage dealt by players to boss entities.
 * Used for damage-based reward distribution.
 */
public class DamageTracker {
    private final Map<UUID, Double> damageDealt = new HashMap<>();
    private final double minDamagePercent;

    public DamageTracker(double minDamagePercent) {
        this.minDamagePercent = minDamagePercent;
    }

    /**
     * Record damage dealt by a player.
     */
    public void recordDamage(Player player, double damage) {
        damageDealt.merge(player.getUniqueId(), damage, Double::sum);
    }

    /**
     * Record damage by UUID (for offline tracking).
     */
    public void recordDamage(UUID playerId, double damage) {
        damageDealt.merge(playerId, damage, Double::sum);
    }

    /**
     * Get total damage dealt by all players.
     */
    public double getTotalDamage() {
        return damageDealt.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    /**
     * Get damage dealt by a specific player.
     */
    public double getDamage(UUID playerId) {
        return damageDealt.getOrDefault(playerId, 0.0);
    }

    /**
     * Get damage percentage for a player.
     */
    public double getDamagePercent(UUID playerId) {
        double total = getTotalDamage();
        if (total == 0) return 0;
        return (getDamage(playerId) / total) * 100;
    }

    /**
     * Calculate reward distribution based on damage share.
     * Only players who dealt >= minDamagePercent get rewards.
     */
    public Map<UUID, Integer> calculateRewards(int totalRewardPool) {
        double totalDamage = getTotalDamage();
        if (totalDamage == 0) return Collections.emptyMap();

        Map<UUID, Integer> rewards = new HashMap<>();

        for (Map.Entry<UUID, Double> entry : damageDealt.entrySet()) {
            double damagePercent = (entry.getValue() / totalDamage) * 100;

            if (damagePercent >= minDamagePercent) {
                int reward = (int) ((entry.getValue() / totalDamage) * totalRewardPool);
                rewards.put(entry.getKey(), Math.max(1, reward)); // Minimum 1 reward
            }
        }

        return rewards;
    }

    /**
     * Get top contributors sorted by damage (descending).
     */
    public List<Map.Entry<UUID, Double>> getTopContributors(int limit) {
        return damageDealt.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get all players who contributed.
     */
    public Set<UUID> getAllContributors() {
        return new HashSet<>(damageDealt.keySet());
    }

    /**
     * Check if a player has contributed.
     */
    public boolean hasContributed(UUID playerId) {
        return damageDealt.containsKey(playerId) && damageDealt.get(playerId) > 0;
    }

    /**
     * Clear all damage records.
     */
    public void clear() {
        damageDealt.clear();
    }
}
