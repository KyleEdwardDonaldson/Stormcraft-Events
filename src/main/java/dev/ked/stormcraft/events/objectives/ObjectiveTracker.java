package dev.ked.stormcraft.events.objectives;

import dev.ked.stormcraft.events.event.Event;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Tracks objectives for active events.
 */
public class ObjectiveTracker {
    private final Map<UUID, List<Objective>> eventObjectives = new HashMap<>();

    /**
     * Register objectives for an event.
     */
    public void registerObjectives(Event event, List<Objective> objectives) {
        eventObjectives.put(event.getEventId(), objectives);
    }

    /**
     * Get objectives for an event.
     */
    public List<Objective> getObjectives(UUID eventId) {
        return eventObjectives.getOrDefault(eventId, new ArrayList<>());
    }

    /**
     * Update progress for an objective.
     */
    public boolean updateProgress(UUID eventId, Player player, Objective objective, int amount) {
        List<Objective> objectives = getObjectives(eventId);
        if (!objectives.contains(objective)) return false;

        return objective.updateProgress(player, amount);
    }

    /**
     * Check if all objectives are complete for an event.
     */
    public boolean areAllComplete(UUID eventId) {
        List<Objective> objectives = getObjectives(eventId);
        if (objectives.isEmpty()) return false;

        return objectives.stream().allMatch(Objective::isComplete);
    }

    /**
     * Get completion percentage for an event (average of all objectives).
     */
    public double getCompletionPercent(UUID eventId, UUID playerId) {
        List<Objective> objectives = getObjectives(eventId);
        if (objectives.isEmpty()) return 0;

        double total = objectives.stream()
                .mapToDouble(obj -> obj.getProgressPercent(playerId))
                .sum();

        return total / objectives.size();
    }

    /**
     * Clear objectives for an event.
     */
    public void clearObjectives(UUID eventId) {
        eventObjectives.remove(eventId);
    }

    /**
     * Clear all objectives.
     */
    public void clearAll() {
        eventObjectives.clear();
    }
}
