package dev.ked.stormcraft.events.difficulty;

/**
 * Holds the results of a difficulty calculation including the final multiplier
 * and all contributing factors.
 */
public class DifficultyMultiplier {
    private final double multiplier;
    private final int playerCount;
    private final int partyMembers;
    private final int proximityPlayers;
    private final boolean inWilderness;
    private final boolean nearStorm;
    private final ThreatLevel threatLevel;

    // Bonus breakdown
    private final double partyBonus;
    private final double proximityBonus;
    private final double wildernessBonus;
    private final double stormBonus;

    private DifficultyMultiplier(Builder builder) {
        this.multiplier = builder.multiplier;
        this.playerCount = builder.playerCount;
        this.partyMembers = builder.partyMembers;
        this.proximityPlayers = builder.proximityPlayers;
        this.inWilderness = builder.inWilderness;
        this.nearStorm = builder.nearStorm;
        this.partyBonus = builder.partyBonus;
        this.proximityBonus = builder.proximityBonus;
        this.wildernessBonus = builder.wildernessBonus;
        this.stormBonus = builder.stormBonus;
        this.threatLevel = ThreatLevel.fromMultiplier(multiplier);
    }

    public double getMultiplier() {
        return multiplier;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public int getPartyMembers() {
        return partyMembers;
    }

    public int getProximityPlayers() {
        return proximityPlayers;
    }

    public boolean isInWilderness() {
        return inWilderness;
    }

    public boolean isNearStorm() {
        return nearStorm;
    }

    public ThreatLevel getThreatLevel() {
        return threatLevel;
    }

    public double getPartyBonus() {
        return partyBonus;
    }

    public double getProximityBonus() {
        return proximityBonus;
    }

    public double getWildernessBonus() {
        return wildernessBonus;
    }

    public double getStormBonus() {
        return stormBonus;
    }

    /**
     * Get a detailed breakdown of the difficulty calculation for debug/display purposes.
     *
     * @return Multi-line string showing calculation breakdown
     */
    public String getBreakdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("§6Difficulty Analysis:\n");
        sb.append("§7Base Multiplier: §f1.0x\n");

        if (partyBonus > 0) {
            sb.append(String.format("§7  + Party Bonus: §e+%.1fx §7(%d members)\n",
                partyBonus, partyMembers));
        }

        if (proximityBonus > 0) {
            sb.append(String.format("§7  + Proximity: §e+%.1fx §7(%d nearby)\n",
                proximityBonus, proximityPlayers));
        }

        if (wildernessBonus > 0) {
            sb.append(String.format("§7  + Wilderness: §e+%.1fx\n", wildernessBonus));
        }

        if (stormBonus > 0) {
            sb.append(String.format("§7  + Storm: §e+%.1fx\n", stormBonus));
        }

        sb.append(String.format("§7Final Multiplier: §6%.1fx\n", multiplier));
        sb.append("§7Threat Level: ").append(threatLevel.getDisplayName());

        return sb.toString();
    }

    /**
     * Get a compact one-line display of the difficulty.
     *
     * @return Compact string for action bar display
     */
    public String getCompactDisplay() {
        return String.format("⚡ %.1fx | 👥 %d | %s | %s",
            multiplier,
            playerCount,
            inWilderness ? "🌍 Wild" : "🏘️ Town",
            threatLevel.getDisplayName());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private double multiplier = 1.0;
        private int playerCount = 1;
        private int partyMembers = 0;
        private int proximityPlayers = 0;
        private boolean inWilderness = false;
        private boolean nearStorm = false;
        private double partyBonus = 0.0;
        private double proximityBonus = 0.0;
        private double wildernessBonus = 0.0;
        private double stormBonus = 0.0;

        public Builder multiplier(double multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        public Builder playerCount(int playerCount) {
            this.playerCount = playerCount;
            return this;
        }

        public Builder partyMembers(int partyMembers) {
            this.partyMembers = partyMembers;
            return this;
        }

        public Builder proximityPlayers(int proximityPlayers) {
            this.proximityPlayers = proximityPlayers;
            return this;
        }

        public Builder inWilderness(boolean inWilderness) {
            this.inWilderness = inWilderness;
            return this;
        }

        public Builder nearStorm(boolean nearStorm) {
            this.nearStorm = nearStorm;
            return this;
        }

        public Builder partyBonus(double partyBonus) {
            this.partyBonus = partyBonus;
            return this;
        }

        public Builder proximityBonus(double proximityBonus) {
            this.proximityBonus = proximityBonus;
            return this;
        }

        public Builder wildernessBonus(double wildernessBonus) {
            this.wildernessBonus = wildernessBonus;
            return this;
        }

        public Builder stormBonus(double stormBonus) {
            this.stormBonus = stormBonus;
            return this;
        }

        public DifficultyMultiplier build() {
            return new DifficultyMultiplier(this);
        }
    }
}
