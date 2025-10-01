package dev.ked.stormcraft.events.config;

import dev.ked.stormcraft.events.StormcraftEventsPlugin;
import dev.ked.stormcraft.events.event.EventType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages plugin configuration files.
 */
public class ConfigManager {
    private final StormcraftEventsPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration rewards;
    private FileConfiguration towns;

    private final Map<EventType, EventConfig> eventConfigs = new HashMap<>();

    public ConfigManager(StormcraftEventsPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        // Create default configs if not exist
        plugin.saveDefaultConfig();
        saveResource("rewards.yml");
        saveResource("towns.yml");

        // Load configs
        config = plugin.getConfig();
        rewards = loadConfig("rewards.yml");
        towns = loadConfig("towns.yml");

        // Load event configs
        loadEventConfigs();

        plugin.getLogger().info("Configuration loaded");
    }

    private void loadEventConfigs() {
        ConfigurationSection eventsSection = config.getConfigurationSection("events.types");
        if (eventsSection == null) {
            plugin.getLogger().warning("No event configurations found!");
            return;
        }

        for (EventType type : EventType.values()) {
            String key = type.name();
            if (eventsSection.contains(key)) {
                EventConfig eventConfig = new EventConfig(eventsSection.getConfigurationSection(key));
                eventConfigs.put(type, eventConfig);
            }
        }
    }

    private void saveResource(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
    }

    private FileConfiguration loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        return YamlConfiguration.loadConfiguration(file);
    }

    // Getters for event properties
    public boolean isEventEnabled(EventType type) {
        EventConfig cfg = eventConfigs.get(type);
        return cfg != null && cfg.enabled;
    }

    public int getEventWeight(EventType type) {
        EventConfig cfg = eventConfigs.get(type);
        return cfg != null ? cfg.weight : 10;
    }

    public int getMinPlayers(EventType type) {
        EventConfig cfg = eventConfigs.get(type);
        return cfg != null ? cfg.minPlayers : 1;
    }

    public int getEventDuration(EventType type) {
        EventConfig cfg = eventConfigs.get(type);
        return cfg != null ? cfg.duration : 120;
    }

    public int getEventCooldown(EventType type) {
        EventConfig cfg = eventConfigs.get(type);
        return cfg != null ? cfg.cooldown : 300;
    }

    public int getEssenceReward(EventType type) {
        EventConfig cfg = eventConfigs.get(type);
        return cfg != null ? cfg.essenceReward : 50;
    }

    public int getMinStormIntensity(EventType type) {
        EventConfig cfg = eventConfigs.get(type);
        return cfg != null ? cfg.minIntensity : 0;
    }

    public String getMythicMobType(EventType type) {
        EventConfig cfg = eventConfigs.get(type);
        return cfg != null ? cfg.mythicMobType : "";
    }

    // General settings
    public int getGlobalCooldown() {
        return config.getInt("events.globalCooldown", 60);
    }

    public int getAnnounceRadius() {
        return config.getInt("events.announceRadius", 900);
    }

    public boolean useActionBar() {
        return config.getBoolean("events.useActionBar", true);
    }

    public boolean useBossBar() {
        return config.getBoolean("events.useBossBar", true);
    }

    public boolean chatAnnouncements() {
        return config.getBoolean("events.chatAnnouncements", true);
    }

    // Density settings
    public int getDensityCheckInterval() {
        return config.getInt("events.density.checkInterval", 30);
    }

    public double getBaseChance() {
        return config.getDouble("events.density.baseChance", 0.05);
    }

    public double getPlayerMultiplier() {
        return config.getDouble("events.density.playerMultiplier", 0.1);
    }

    public double getMaxSpawnChance() {
        return config.getDouble("events.density.maxChance", 0.5);
    }

    // Siege settings
    public boolean isSiegeEnabled() {
        return config.getBoolean("events.types.TOWN_SIEGE.enabled", true);
    }

    public int getSiegeWaveCount() {
        return config.getInt("events.types.TOWN_SIEGE.waveCount", 3);
    }

    public int getSiegeMobsPerWave() {
        return config.getInt("events.types.TOWN_SIEGE.mobsPerWave", 15);
    }

    // World settings
    public boolean isWorldEnabled(String worldName) {
        // For now, enable all worlds
        return true;
    }

    // Rewards
    public int getParticipationEssence() {
        return rewards.getInt("rewards.participation.essence", 10);
    }

    public int getCompletionEssence() {
        return rewards.getInt("rewards.completion.essence", 50);
    }

    public int getBossTotalEssencePool() {
        return rewards.getInt("rewards.bosses.totalEssencePool", 1000);
    }

    public double getMinDamagePercent() {
        return rewards.getDouble("rewards.bosses.minDamagePercent", 5.0);
    }

    // Difficulty settings
    public boolean isDifficultyEnabled() {
        return config.getBoolean("difficulty.enabled", true);
    }

    public double getDifficultyScanRadius() {
        return config.getDouble("difficulty.scan_radius", 50.0);
    }

    public double getPartyBonusPerMember() {
        return config.getDouble("difficulty.party_bonus_per_member", 0.3);
    }

    public double getMaxPartyBonus() {
        return config.getDouble("difficulty.max_party_bonus", 1.5);
    }

    public double getProximityBonusPerPlayer() {
        return config.getDouble("difficulty.proximity_bonus_per_player", 0.2);
    }

    public double getMaxProximityBonus() {
        return config.getDouble("difficulty.max_proximity_bonus", 1.0);
    }

    public double getWildernessBonus() {
        return config.getDouble("difficulty.wilderness_bonus", 0.5);
    }

    public double getStormProximityBonus() {
        return config.getDouble("difficulty.storm_proximity_bonus", 0.5);
    }

    public double getStormProximityRadius() {
        return config.getDouble("difficulty.storm_proximity_radius", 300.0);
    }

    public double getTownClaimMultiplier() {
        return config.getDouble("difficulty.town_claim_multiplier", 0.5);
    }

    /**
     * Get event weight for a specific threat level and event type.
     * Returns the configured weight or a default value.
     */
    public int getDifficultyEventWeight(String threatLevel, EventType eventType) {
        String path = "difficulty.weights." + threatLevel.toLowerCase() + "." +
                     eventTypeToConfigKey(eventType);
        return config.getInt(path, getDefaultWeight(threatLevel, eventType));
    }

    /**
     * Convert EventType to config key (e.g., STORM_SURGE -> storm_surge).
     */
    private String eventTypeToConfigKey(EventType type) {
        return type.name().toLowerCase();
    }

    /**
     * Get default weights if not configured.
     */
    private int getDefaultWeight(String threatLevel, EventType type) {
        return switch (threatLevel.toLowerCase()) {
            case "low" -> switch (type) {
                case STORM_SURGE -> 60;
                case STORM_RIFT -> 30;
                case TEMPEST_GUARDIAN -> 10;
                case STORM_TITAN, TOWN_SIEGE -> 0;
            };
            case "medium" -> switch (type) {
                case STORM_SURGE -> 40;
                case STORM_RIFT -> 35;
                case TEMPEST_GUARDIAN -> 20;
                case STORM_TITAN -> 5;
                case TOWN_SIEGE -> 0;
            };
            case "high" -> switch (type) {
                case STORM_SURGE -> 20;
                case STORM_RIFT -> 30;
                case TEMPEST_GUARDIAN -> 35;
                case STORM_TITAN -> 10;
                case TOWN_SIEGE -> 5;
            };
            case "extreme" -> switch (type) {
                case STORM_SURGE -> 10;
                case STORM_RIFT -> 20;
                case TEMPEST_GUARDIAN -> 40;
                case STORM_TITAN -> 20;
                case TOWN_SIEGE -> 10;
            };
            default -> 10;
        };
    }

    /**
     * Get raw config for direct access.
     */
    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * Inner class to hold event-specific config.
     */
    private static class EventConfig {
        boolean enabled;
        int weight;
        int minPlayers;
        int duration;
        int cooldown;
        int essenceReward;
        int minIntensity;
        String mythicMobType;

        EventConfig(ConfigurationSection section) {
            if (section == null) return;

            this.enabled = section.getBoolean("enabled", true);
            this.weight = section.getInt("weight", 10);
            this.minPlayers = section.getInt("minPlayers", 1);
            this.duration = section.getInt("duration", 120);
            this.cooldown = section.getInt("cooldown", 300);
            this.essenceReward = section.getInt("essenceReward", 50);
            this.minIntensity = section.getInt("stormIntensityMin", 0);
            this.mythicMobType = section.getString("mythicMobType", "");
        }
    }
}
