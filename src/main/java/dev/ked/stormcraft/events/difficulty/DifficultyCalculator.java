package dev.ked.stormcraft.events.difficulty;

import dev.ked.stormcraft.events.event.EventType;
import dev.ked.stormcraft.events.integration.StormcraftIntegration;
import dev.ked.stormcraft.model.TravelingStorm;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Calculates difficulty multipliers based on player density, party size,
 * wilderness/town status, and storm proximity.
 */
public class DifficultyCalculator {
    private final JavaPlugin plugin;
    private final PlayerDensityTracker densityTracker;
    private final StormcraftIntegration stormcraftIntegration;
    private final Random random = new Random();

    // Configuration values (loaded from config)
    private double partyBonusPerMember = 0.3;
    private double maxPartyBonus = 1.5;
    private double proximityBonusPerPlayer = 0.2;
    private double maxProximityBonus = 1.0;
    private double wildernessBonus = 0.5;
    private double stormProximityBonus = 0.5;
    private double stormProximityRadius = 300.0;
    private double townClaimMultiplier = 0.5;

    // Event type weights by threat level
    private final Map<ThreatLevel, Map<EventType, Integer>> eventWeights = new HashMap<>();

    public DifficultyCalculator(JavaPlugin plugin, PlayerDensityTracker densityTracker,
                               StormcraftIntegration stormcraftIntegration) {
        this.plugin = plugin;
        this.densityTracker = densityTracker;
        this.stormcraftIntegration = stormcraftIntegration;
        initializeDefaultWeights();
    }

    /**
     * Calculate the difficulty multiplier for a given location and nearby players.
     *
     * @param location       The location to calculate difficulty for
     * @param nearbyPlayers List of nearby players (from density tracker)
     * @return DifficultyMultiplier object with all calculation details
     */
    public DifficultyMultiplier calculate(Location location, List<Player> nearbyPlayers) {
        if (nearbyPlayers.isEmpty()) {
            // No players nearby, return base difficulty
            return DifficultyMultiplier.builder()
                .multiplier(1.0)
                .playerCount(0)
                .build();
        }

        // Get the primary player (closest to location)
        Player primaryPlayer = getNearestPlayer(location, nearbyPlayers);
        if (primaryPlayer == null) {
            return DifficultyMultiplier.builder()
                .multiplier(1.0)
                .playerCount(0)
                .build();
        }

        // Calculate party bonus
        int partyMembers = densityTracker.getPartyMemberCount(primaryPlayer, nearbyPlayers);
        double partyBonus = Math.min(partyMembers * partyBonusPerMember, maxPartyBonus);

        // Calculate proximity bonus (non-party players nearby)
        int proximityPlayers = densityTracker.getNonPartyPlayerCount(primaryPlayer, nearbyPlayers);
        double proximityBonus = Math.min(proximityPlayers * proximityBonusPerPlayer, maxProximityBonus);

        // Check wilderness status
        boolean inWilderness = isInWilderness(location);
        double wildernessMultiplier = inWilderness ? wildernessBonus : 0.0;

        // Check storm proximity
        boolean nearStorm = isNearStorm(location);
        double stormMultiplier = nearStorm ? stormProximityBonus : 0.0;

        // Calculate total multiplier
        double totalMultiplier = 1.0 + partyBonus + proximityBonus + wildernessMultiplier + stormMultiplier;

        // Apply town claim reduction if in claimed land
        if (!inWilderness) {
            totalMultiplier *= townClaimMultiplier;
        }

        return DifficultyMultiplier.builder()
            .multiplier(totalMultiplier)
            .playerCount(nearbyPlayers.size())
            .partyMembers(partyMembers)
            .proximityPlayers(proximityPlayers)
            .inWilderness(inWilderness)
            .nearStorm(nearStorm)
            .partyBonus(partyBonus)
            .proximityBonus(proximityBonus)
            .wildernessBonus(wildernessMultiplier)
            .stormBonus(stormMultiplier)
            .build();
    }

    /**
     * Select an appropriate event type based on the difficulty multiplier.
     * Uses weighted random selection based on threat level.
     *
     * @param difficulty The calculated difficulty multiplier
     * @return Selected event type
     */
    public EventType selectEventType(DifficultyMultiplier difficulty) {
        ThreatLevel threatLevel = difficulty.getThreatLevel();
        Map<EventType, Integer> weights = eventWeights.get(threatLevel);

        if (weights == null || weights.isEmpty()) {
            // Fallback to storm surge
            return EventType.STORM_SURGE;
        }

        // Calculate total weight
        int totalWeight = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (totalWeight <= 0) {
            return EventType.STORM_SURGE;
        }

        // Weighted random selection
        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;

        for (Map.Entry<EventType, Integer> entry : weights.entrySet()) {
            currentWeight += entry.getValue();
            if (randomValue < currentWeight) {
                return entry.getKey();
            }
        }

        // Fallback
        return EventType.STORM_SURGE;
    }

    /**
     * Calculate the reward multiplier based on difficulty.
     * Formula: Base × (1 + (multiplier - 1) × scalingFactor)
     *
     * @param difficulty The difficulty multiplier
     * @return Reward scaling multiplier
     */
    public double getRewardMultiplier(DifficultyMultiplier difficulty) {
        double scalingFactor = 2.5; // Can be made configurable
        return 1.0 + (difficulty.getMultiplier() - 1.0) * scalingFactor;
    }

    /**
     * Get the nearest player to a location.
     */
    private Player getNearestPlayer(Location location, List<Player> players) {
        Player nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Player player : players) {
            double distance = player.getLocation().distance(location);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = player;
            }
        }

        return nearest;
    }

    /**
     * Check if a location is in wilderness (not in a town claim).
     * Currently returns true - will be integrated with Towny/TAN in Phase 2.
     */
    private boolean isInWilderness(Location location) {
        // TODO: Integrate with TownyIntegration and TownsAndNationsIntegration
        // For now, assume all locations are wilderness
        return true;
    }

    /**
     * Check if a location is near an active storm.
     */
    private boolean isNearStorm(Location location) {
        if (stormcraftIntegration == null || !stormcraftIntegration.isEnabled()) {
            return false;
        }

        for (TravelingStorm storm : stormcraftIntegration.getActiveStorms()) {
            Location epicenter = storm.getCurrentLocation();
            if (epicenter.getWorld().equals(location.getWorld())) {
                double distance = epicenter.distance(location);
                if (distance <= stormProximityRadius) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Initialize default event weights by threat level.
     */
    private void initializeDefaultWeights() {
        // LOW threat (1.0 - 1.49x)
        Map<EventType, Integer> lowWeights = new HashMap<>();
        lowWeights.put(EventType.STORM_SURGE, 60);
        lowWeights.put(EventType.STORM_RIFT, 30);
        lowWeights.put(EventType.TEMPEST_GUARDIAN, 10);
        lowWeights.put(EventType.STORM_TITAN, 0);
        lowWeights.put(EventType.TOWN_SIEGE, 0);
        eventWeights.put(ThreatLevel.LOW, lowWeights);

        // MEDIUM threat (1.5 - 1.99x)
        Map<EventType, Integer> mediumWeights = new HashMap<>();
        mediumWeights.put(EventType.STORM_SURGE, 40);
        mediumWeights.put(EventType.STORM_RIFT, 35);
        mediumWeights.put(EventType.TEMPEST_GUARDIAN, 20);
        mediumWeights.put(EventType.STORM_TITAN, 5);
        mediumWeights.put(EventType.TOWN_SIEGE, 0);
        eventWeights.put(ThreatLevel.MEDIUM, mediumWeights);

        // HIGH threat (2.0 - 2.74x)
        Map<EventType, Integer> highWeights = new HashMap<>();
        highWeights.put(EventType.STORM_SURGE, 20);
        highWeights.put(EventType.STORM_RIFT, 30);
        highWeights.put(EventType.TEMPEST_GUARDIAN, 35);
        highWeights.put(EventType.STORM_TITAN, 10);
        highWeights.put(EventType.TOWN_SIEGE, 5);
        eventWeights.put(ThreatLevel.HIGH, highWeights);

        // EXTREME threat (2.75+x)
        Map<EventType, Integer> extremeWeights = new HashMap<>();
        extremeWeights.put(EventType.STORM_SURGE, 10);
        extremeWeights.put(EventType.STORM_RIFT, 20);
        extremeWeights.put(EventType.TEMPEST_GUARDIAN, 40);
        extremeWeights.put(EventType.STORM_TITAN, 20);
        extremeWeights.put(EventType.TOWN_SIEGE, 10);
        eventWeights.put(ThreatLevel.EXTREME, extremeWeights);
    }

    // Configuration setters
    public void setPartyBonusPerMember(double partyBonusPerMember) {
        this.partyBonusPerMember = partyBonusPerMember;
    }

    public void setMaxPartyBonus(double maxPartyBonus) {
        this.maxPartyBonus = maxPartyBonus;
    }

    public void setProximityBonusPerPlayer(double proximityBonusPerPlayer) {
        this.proximityBonusPerPlayer = proximityBonusPerPlayer;
    }

    public void setMaxProximityBonus(double maxProximityBonus) {
        this.maxProximityBonus = maxProximityBonus;
    }

    public void setWildernessBonus(double wildernessBonus) {
        this.wildernessBonus = wildernessBonus;
    }

    public void setStormProximityBonus(double stormProximityBonus) {
        this.stormProximityBonus = stormProximityBonus;
    }

    public void setStormProximityRadius(double stormProximityRadius) {
        this.stormProximityRadius = stormProximityRadius;
    }

    public void setTownClaimMultiplier(double townClaimMultiplier) {
        this.townClaimMultiplier = townClaimMultiplier;
    }

    public void setEventWeights(ThreatLevel level, Map<EventType, Integer> weights) {
        eventWeights.put(level, weights);
    }

    public Map<ThreatLevel, Map<EventType, Integer>> getEventWeights() {
        return new HashMap<>(eventWeights);
    }
}
