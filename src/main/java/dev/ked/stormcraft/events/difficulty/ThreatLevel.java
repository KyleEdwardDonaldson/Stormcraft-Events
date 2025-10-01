package dev.ked.stormcraft.events.difficulty;

/**
 * Represents the threat level of an area based on difficulty multiplier.
 * Used for UI display and categorizing danger levels.
 */
public enum ThreatLevel {
    LOW(1.0, 1.49, "§aLow"),
    MEDIUM(1.5, 1.99, "§eMedium"),
    HIGH(2.0, 2.74, "§6High"),
    EXTREME(2.75, 10.0, "§cExtreme");

    private final double minMultiplier;
    private final double maxMultiplier;
    private final String displayName;

    ThreatLevel(double minMultiplier, double maxMultiplier, String displayName) {
        this.minMultiplier = minMultiplier;
        this.maxMultiplier = maxMultiplier;
        this.displayName = displayName;
    }

    public double getMinMultiplier() {
        return minMultiplier;
    }

    public double getMaxMultiplier() {
        return maxMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the threat level corresponding to a difficulty multiplier.
     *
     * @param multiplier The difficulty multiplier
     * @return The corresponding threat level
     */
    public static ThreatLevel fromMultiplier(double multiplier) {
        for (ThreatLevel level : values()) {
            if (multiplier >= level.minMultiplier && multiplier <= level.maxMultiplier) {
                return level;
            }
        }
        // Default to EXTREME if somehow above max
        return EXTREME;
    }

    /**
     * Check if this threat level is at least as dangerous as another.
     *
     * @param other The other threat level
     * @return true if this is more dangerous or equal
     */
    public boolean isAtLeast(ThreatLevel other) {
        return this.ordinal() >= other.ordinal();
    }

    @Override
    public String toString() {
        return displayName;
    }
}
