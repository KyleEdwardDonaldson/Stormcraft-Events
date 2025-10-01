package dev.ked.stormcraft.events.objectives.objectives;

import dev.ked.stormcraft.events.objectives.Objective;

/**
 * Objective to collect a certain number of items.
 */
public class CollectObjective extends Objective {
    public CollectObjective(int target) {
        super("Collect Storm Crystals", target);
    }

    @Override
    public String getProgressString(java.util.UUID playerId) {
        return "§bStorm Crystals: §e" + getProgress(playerId) + "§7/§e" + target;
    }
}
