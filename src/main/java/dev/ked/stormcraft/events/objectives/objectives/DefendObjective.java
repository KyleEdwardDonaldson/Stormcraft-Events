package dev.ked.stormcraft.events.objectives.objectives;

import dev.ked.stormcraft.events.objectives.Objective;

/**
 * Objective to survive a certain number of waves.
 */
public class DefendObjective extends Objective {
    private int currentWave = 0;

    public DefendObjective(int totalWaves) {
        super("Survive Waves", totalWaves);
    }

    /**
     * Advance to next wave.
     */
    public void nextWave() {
        currentWave++;
        // Update all players' progress
        for (java.util.UUID playerId : progress.keySet()) {
            progress.put(playerId, currentWave);
        }

        if (currentWave >= target) {
            completed = true;
        }
    }

    public int getCurrentWave() {
        return currentWave;
    }

    @Override
    public String getProgressString(java.util.UUID playerId) {
        return "§5Wave: §e" + currentWave + "§7/§e" + target;
    }
}
