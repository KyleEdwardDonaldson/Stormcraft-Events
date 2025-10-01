package dev.ked.stormcraft.events.difficulty;

import dev.ked.stormcraft.events.config.ConfigManager;
import dev.ked.stormcraft.events.event.Event;
import dev.ked.stormcraft.events.event.EventType;
import dev.ked.stormcraft.events.integration.EssenceIntegration;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Calculates scaled essence rewards based on difficulty multipliers,
 * party composition, and individual contributions.
 */
public class GroupRewardCalculator {
    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final EssenceIntegration essence;
    private final Economy economy;
    private final PlayerDensityTracker densityTracker;

    public GroupRewardCalculator(JavaPlugin plugin, ConfigManager config,
                                EssenceIntegration essence, Economy economy,
                                PlayerDensityTracker densityTracker) {
        this.plugin = plugin;
        this.config = config;
        this.essence = essence;
        this.economy = economy;
        this.densityTracker = densityTracker;
    }

    /**
     * Calculate and distribute rewards for event completion.
     *
     * @param event The completed event
     * @param participants List of participating players
     * @return Map of player to essence amount awarded
     */
    public Map<Player, Double> calculateAndDistributeRewards(Event event, List<Player> participants) {
        Map<Player, Double> rewards = new HashMap<>();

        if (participants.isEmpty()) {
            return rewards;
        }

        // Get base reward from config
        int baseReward = config.getEssenceReward(event.getType());

        // Get difficulty multiplier
        DifficultyMultiplier difficulty = event.getDifficulty();
        double difficultyMult = difficulty != null ? difficulty.getMultiplier() : 1.0;

        // Calculate reward scaling based on difficulty
        double rewardScaling = calculateRewardScaling(difficultyMult);

        // Calculate scaled base reward
        double scaledBaseReward = baseReward * rewardScaling;

        // Apply party bonus
        int partySize = participants.size();
        double partyBonus = calculatePartyBonus(partySize);
        double totalReward = scaledBaseReward * (1.0 + partyBonus);

        // Split equally among participants (can be weighted by damage later)
        double rewardPerPlayer = totalReward / partySize;

        // Award to each participant
        for (Player player : participants) {
            rewards.put(player, rewardPerPlayer);

            // Actually award the essence
            if (essence != null && essence.isEnabled() && economy != null) {
                essence.awardEssence(player, (int) Math.round(rewardPerPlayer), economy);
            }

            // Send breakdown message
            sendRewardBreakdown(player, event, baseReward, rewardPerPlayer,
                               difficultyMult, partyBonus, partySize);
        }

        return rewards;
    }

    /**
     * Calculate reward scaling based on difficulty multiplier.
     * Formula: 1 + (multiplier - 1) × scalingFactor
     *
     * @param difficultyMultiplier The difficulty multiplier
     * @return The reward scaling factor
     */
    private double calculateRewardScaling(double difficultyMultiplier) {
        double scalingFactor = config.getConfig().getDouble("difficulty.reward_scaling_factor", 2.5);
        return 1.0 + (difficultyMultiplier - 1.0) * scalingFactor;
    }

    /**
     * Calculate party completion bonus based on party size.
     * Formula: min(partySize × bonusPerMember, maxBonus)
     *
     * @param partySize Number of players in party
     * @return Bonus multiplier (0.0 to max_party_completion_bonus)
     */
    private double calculatePartyBonus(int partySize) {
        if (partySize <= 1) {
            return 0.0;
        }

        double bonusPerMember = config.getConfig()
            .getDouble("difficulty.party_completion_bonus", 0.1);
        double maxBonus = config.getConfig()
            .getDouble("difficulty.max_party_completion_bonus", 0.5);

        return Math.min(partySize * bonusPerMember, maxBonus);
    }

    /**
     * Send detailed reward breakdown to player.
     */
    private void sendRewardBreakdown(Player player, Event event, int baseReward,
                                     double finalReward, double difficultyMult,
                                     double partyBonus, int partySize) {
        EventType type = event.getType();
        DifficultyMultiplier difficulty = event.getDifficulty();

        player.sendMessage("§a§l✓ " + type.getDisplayName() + " Defeated!");

        // Show difficulty if present
        if (difficulty != null) {
            player.sendMessage("§7Difficulty: " + difficulty.getThreatLevel().getDisplayName() +
                             String.format(" §f(%.1fx)", difficultyMult));
        }

        // Show party info if in group
        if (partySize > 1) {
            player.sendMessage(String.format("§7Party: §f%d players", partySize));
        }

        // Calculate component breakdowns
        double rewardScaling = calculateRewardScaling(difficultyMult);
        double scaledBase = baseReward * rewardScaling;
        double difficultyBonus = scaledBase - baseReward;
        double partyBonusAmount = scaledBase * partyBonus;

        // Show final reward
        player.sendMessage(String.format("§e+ %d Essence §7(base: %d)",
            (int) Math.round(finalReward), baseReward));

        // Show breakdown if any bonuses applied
        if (difficultyBonus > 0 || partyBonusAmount > 0) {
            if (difficultyBonus > 0) {
                player.sendMessage(String.format("§7  Difficulty: §e+%d",
                    (int) Math.round(difficultyBonus / partySize)));
            }
            if (partyBonusAmount > 0) {
                player.sendMessage(String.format("§7  Party Bonus: §e+%d",
                    (int) Math.round(partyBonusAmount / partySize)));
            }
        }
    }

    /**
     * Calculate damage-weighted rewards for boss events.
     * This allows partial rewards based on damage contribution.
     *
     * @param event The event
     * @param damageContributions Map of player to damage dealt
     * @return Map of player to essence reward
     */
    public Map<Player, Double> calculateDamageWeightedRewards(Event event,
                                                              Map<Player, Double> damageContributions) {
        Map<Player, Double> rewards = new HashMap<>();

        if (damageContributions.isEmpty()) {
            return rewards;
        }

        // Get total damage
        double totalDamage = damageContributions.values().stream()
            .mapToDouble(Double::doubleValue).sum();

        if (totalDamage <= 0) {
            return rewards;
        }

        // Get base essence pool
        int basePool = config.getBossTotalEssencePool();

        // Scale by difficulty
        DifficultyMultiplier difficulty = event.getDifficulty();
        double difficultyMult = difficulty != null ? difficulty.getMultiplier() : 1.0;
        double rewardScaling = calculateRewardScaling(difficultyMult);
        double scaledPool = basePool * rewardScaling;

        // Distribute based on damage percentage
        double minDamagePercent = config.getMinDamagePercent();

        for (Map.Entry<Player, Double> entry : damageContributions.entrySet()) {
            Player player = entry.getKey();
            double damage = entry.getValue();
            double damagePercent = (damage / totalDamage) * 100.0;

            // Check minimum damage threshold
            if (damagePercent < minDamagePercent) {
                continue;
            }

            // Calculate reward
            double playerReward = scaledPool * (damage / totalDamage);
            rewards.put(player, playerReward);

            // Award essence
            if (essence != null && essence.isEnabled() && economy != null) {
                essence.awardEssence(player, (int) Math.round(playerReward), economy);
            }

            // Send message
            player.sendMessage(String.format("§a+ %d Essence §7(%.1f%% damage)",
                (int) Math.round(playerReward), damagePercent));

            if (difficulty != null) {
                player.sendMessage("§7Difficulty: " + difficulty.getThreatLevel().getDisplayName() +
                                 String.format(" §f(%.1fx)", difficultyMult));
            }
        }

        return rewards;
    }

    /**
     * Award participation rewards (partial completion).
     *
     * @param player The player
     * @param event The event
     */
    public void awardParticipationReward(Player player, Event event) {
        int participationReward = config.getParticipationEssence();

        if (essence != null && essence.isEnabled() && economy != null) {
            essence.awardEssence(player, participationReward, economy);
        }

        player.sendMessage(String.format("§7+ %d Essence §8(participation)", participationReward));
    }
}
