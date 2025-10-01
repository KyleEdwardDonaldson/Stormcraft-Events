package dev.ked.stormcraft.events.event;

/**
 * Enum defining all available event types.
 */
public enum EventType {
    STORM_SURGE("Storm Surge", "Collect storm crystals before they disappear"),
    TEMPEST_GUARDIAN("Tempest Guardian", "Defeat a powerful mini-boss near the storm"),
    STORM_RIFT("Storm Rift", "Defend against waves of storm-corrupted mobs"),
    STORM_TITAN("Storm Titan", "Face a world boss spawned from the tempest"),
    TOWN_SIEGE("Town Siege", "Defend your town from storm-corrupted invaders");

    private final String displayName;
    private final String description;

    EventType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
