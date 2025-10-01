package dev.ked.stormcraft.events.objectives.objectives;

import dev.ked.stormcraft.events.objectives.Objective;

/**
 * Objective to kill a certain number of entities or a boss.
 */
public class KillObjective extends Objective {
    private final boolean isBoss;

    public KillObjective(String targetName, int count, boolean isBoss) {
        super("Defeat " + targetName, count);
        this.isBoss = isBoss;
    }

    public boolean isBoss() {
        return isBoss;
    }

    @Override
    public String getProgressString(java.util.UUID playerId) {
        if (isBoss) {
            return "§c" + description;
        }
        return "§cKills: §e" + getProgress(playerId) + "§7/§e" + target;
    }
}
