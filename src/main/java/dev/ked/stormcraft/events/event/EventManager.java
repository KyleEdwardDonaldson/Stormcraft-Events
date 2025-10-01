package dev.ked.stormcraft.events.event;

import dev.ked.stormcraft.events.StormcraftEventsPlugin;
import dev.ked.stormcraft.events.config.ConfigManager;
import dev.ked.stormcraft.events.difficulty.DifficultyCalculator;
import dev.ked.stormcraft.events.difficulty.DifficultyMultiplier;
import dev.ked.stormcraft.events.difficulty.GroupRewardCalculator;
import dev.ked.stormcraft.events.difficulty.PlayerDensityTracker;
import dev.ked.stormcraft.events.integration.*;
import dev.ked.stormcraft.events.spawn.DensityTracker;
import dev.ked.stormcraft.events.spawn.EventSpawner;
import dev.ked.stormcraft.events.ui.EventNotifier;
import dev.ked.stormcraft.model.TravelingStorm;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core coordinator for event spawning, tracking, and lifecycle management.
 */
public class EventManager {
    private final StormcraftEventsPlugin plugin;
    private final ConfigManager config;
    private final StormcraftIntegration stormcraft;
    private final EssenceIntegration essence;
    private final Economy economy;
    private final EventSpawner spawner;
    private final DensityTracker densityTracker;
    private final PlayerDensityTracker playerDensityTracker;
    private final DifficultyCalculator difficultyCalculator;
    private final GroupRewardCalculator rewardCalculator;

    // Active events tracking
    private final Map<UUID, Event> activeEvents = new ConcurrentHashMap<>();
    private final Map<EventType, Long> lastSpawnTimes = new ConcurrentHashMap<>();

    private BukkitTask spawnCheckTask;
    private long lastGlobalSpawn = 0;

    public EventManager(StormcraftEventsPlugin plugin, ConfigManager config,
                       StormcraftIntegration stormcraft, MythicMobsIntegration mythicMobs,
                       TownyIntegration towny, TownsAndNationsIntegration tan,
                       EssenceIntegration essence, DensityTracker densityTracker,
                       Economy economy) {
        this.plugin = plugin;
        this.config = config;
        this.stormcraft = stormcraft;
        this.essence = essence;
        this.economy = economy;
        this.densityTracker = densityTracker;

        // Initialize difficulty system
        this.playerDensityTracker = new PlayerDensityTracker(plugin, config.getDifficultyScanRadius());
        this.difficultyCalculator = new DifficultyCalculator(plugin, config, playerDensityTracker, stormcraft);
        this.rewardCalculator = new GroupRewardCalculator(plugin, config, essence, economy, playerDensityTracker);
        loadDifficultyConfig();

        this.spawner = new EventSpawner(plugin, config, stormcraft, mythicMobs,
                                       towny, tan, essence, economy, densityTracker);
    }

    /**
     * Load difficulty configuration from config.yml.
     */
    private void loadDifficultyConfig() {
        if (!config.isDifficultyEnabled()) {
            plugin.getLogger().info("Difficulty scaling disabled in config");
            return;
        }

        difficultyCalculator.setPartyBonusPerMember(config.getPartyBonusPerMember());
        difficultyCalculator.setMaxPartyBonus(config.getMaxPartyBonus());
        difficultyCalculator.setProximityBonusPerPlayer(config.getProximityBonusPerPlayer());
        difficultyCalculator.setMaxProximityBonus(config.getMaxProximityBonus());
        difficultyCalculator.setWildernessBonus(config.getWildernessBonus());
        difficultyCalculator.setStormProximityBonus(config.getStormProximityBonus());
        difficultyCalculator.setStormProximityRadius(config.getStormProximityRadius());
        difficultyCalculator.setTownClaimMultiplier(config.getTownClaimMultiplier());

        plugin.getLogger().info("Difficulty system initialized");
    }

    /**
     * Reload difficulty configuration (for hot-reload).
     */
    public void reloadDifficultyConfig() {
        if (!config.isDifficultyEnabled()) {
            return;
        }

        loadDifficultyConfig();
        difficultyCalculator.loadWeightsFromConfig();
        plugin.getLogger().info("Difficulty configuration reloaded");
    }

    /**
     * Start the event system.
     */
    public void start() {
        // Start periodic spawn checks
        int interval = config.getDensityCheckInterval() * 20;
        spawnCheckTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkSpawns, 100L, interval);

        plugin.getLogger().info("Event Manager started");
    }

    /**
     * Check for event spawn opportunities.
     */
    private void checkSpawns() {
        // Check global cooldown
        long now = System.currentTimeMillis();
        long globalCooldown = config.getGlobalCooldown() * 1000L;

        if (now - lastGlobalSpawn < globalCooldown) {
            return; // Still on global cooldown
        }

        // Get active storms
        List<TravelingStorm> storms = stormcraft.getActiveStorms();
        if (storms.isEmpty()) return;

        // Try to spawn event for each storm (with low probability)
        for (TravelingStorm storm : storms) {
            trySpawnEventNearStorm(storm);
        }
    }

    /**
     * Attempt to spawn an event near a storm.
     */
    private void trySpawnEventNearStorm(TravelingStorm storm) {
        Location stormCenter = storm.getCurrentLocation();

        // Calculate spawn chance based on player density
        double spawnChance = densityTracker.calculateSpawnChance(stormCenter, config.getBaseChance());

        // Roll for spawn
        if (Math.random() > spawnChance) {
            return; // No spawn this check
        }

        // Calculate difficulty if enabled
        DifficultyMultiplier difficulty = null;
        EventType selectedType = null;

        if (config.isDifficultyEnabled()) {
            // Get nearby players
            List<Player> nearbyPlayers = playerDensityTracker.getNearbyPlayers(stormCenter);

            if (!nearbyPlayers.isEmpty()) {
                // Calculate difficulty multiplier
                difficulty = difficultyCalculator.calculate(stormCenter, nearbyPlayers);

                // Select event type based on difficulty
                selectedType = difficultyCalculator.selectEventType(difficulty);

                plugin.getLogger().info("Spawning " + selectedType + " with " +
                        String.format("%.1fx", difficulty.getMultiplier()) + " difficulty (" +
                        difficulty.getThreatLevel() + ")");
            }
        }

        // Try to spawn event (with optional type override and difficulty)
        Event event = spawner.trySpawnEvent(storm, selectedType, difficulty);
        if (event != null) {
            startEvent(event);
        }
    }

    /**
     * Start an event.
     */
    public void startEvent(Event event) {
        // Add to active events
        activeEvents.put(event.getEventId(), event);
        lastSpawnTimes.put(event.getType(), System.currentTimeMillis());
        lastGlobalSpawn = System.currentTimeMillis();

        // Start the event
        event.onStart();

        // Notify nearby players
        List<Player> nearbyPlayers = getNearbyPlayers(event.getLocation(), config.getAnnounceRadius());
        EventNotifier.announceSpawn(event, nearbyPlayers);

        plugin.getLogger().info("Started event: " + event.getType() + " at " +
                event.getLocation().getBlockX() + ", " +
                event.getLocation().getBlockY() + ", " +
                event.getLocation().getBlockZ());
    }

    /**
     * End an event and distribute rewards.
     */
    public void endEvent(UUID eventId, boolean success) {
        Event event = activeEvents.remove(eventId);
        if (event == null) return;

        List<Player> participants = event.getParticipants();

        if (success) {
            event.onComplete();
            EventNotifier.announceCompletion(event, participants);
            distributeRewards(event, participants);
        } else {
            event.onFail();
            EventNotifier.announceFailed(event, participants);
        }

        plugin.getLogger().info("Ended event: " + event.getType() + " - " +
                (success ? "SUCCESS" : "FAILED"));
    }

    /**
     * Distribute rewards to participants.
     */
    private void distributeRewards(Event event, List<Player> participants) {
        // Use GroupRewardCalculator for scaled rewards if difficulty enabled
        if (config.isDifficultyEnabled() && event.getDifficulty() != null) {
            rewardCalculator.calculateAndDistributeRewards(event, participants);
        } else {
            // Fallback to simple reward distribution
            int essenceReward = config.getEssenceReward(event.getType());

            for (Player player : participants) {
                // Award essence via economy
                if (economy != null && essence != null && essence.isEnabled()) {
                    essence.awardEssence(player, essenceReward, economy);
                }

                player.sendMessage("§a+ " + essenceReward + " Essence");
            }
        }
    }

    /**
     * Get all active events.
     */
    public Collection<Event> getActiveEvents() {
        return new ArrayList<>(activeEvents.values());
    }

    /**
     * Get event by ID.
     */
    public Event getEvent(UUID eventId) {
        return activeEvents.get(eventId);
    }

    /**
     * Get nearby players within radius.
     */
    private List<Player> getNearbyPlayers(Location location, double radius) {
        List<Player> nearby = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() != location.getWorld()) continue;

            if (player.getLocation().distance(location) <= radius) {
                nearby.add(player);
            }
        }

        return nearby;
    }

    /**
     * Shutdown the event manager.
     */
    public void shutdown() {
        // Cancel spawn check task
        if (spawnCheckTask != null) {
            spawnCheckTask.cancel();
            spawnCheckTask = null;
        }

        // End all active events
        for (Event event : new ArrayList<>(activeEvents.values())) {
            event.cleanup();
        }
        activeEvents.clear();

        plugin.getLogger().info("Event Manager shutdown");
    }

    /**
     * Check if event type is on cooldown.
     */
    public boolean isOnCooldown(EventType type) {
        Long lastSpawn = lastSpawnTimes.get(type);
        if (lastSpawn == null) return false;

        long cooldown = config.getEventCooldown(type) * 1000L;
        return (System.currentTimeMillis() - lastSpawn) < cooldown;
    }

    /**
     * Get remaining cooldown for event type (in seconds).
     */
    public int getRemainingCooldown(EventType type) {
        Long lastSpawn = lastSpawnTimes.get(type);
        if (lastSpawn == null) return 0;

        long cooldown = config.getEventCooldown(type) * 1000L;
        long elapsed = System.currentTimeMillis() - lastSpawn;

        return (int) Math.max(0, (cooldown - elapsed) / 1000);
    }

    // Getters for difficulty system
    public PlayerDensityTracker getPlayerDensityTracker() {
        return playerDensityTracker;
    }

    public DifficultyCalculator getDifficultyCalculator() {
        return difficultyCalculator;
    }
}
