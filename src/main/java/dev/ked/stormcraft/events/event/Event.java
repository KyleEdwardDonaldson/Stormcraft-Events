package dev.ked.stormcraft.events.event;

import dev.ked.stormcraft.events.objectives.Objective;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Base class for all storm events.
 * Handles lifecycle, participant tracking, and objective management.
 */
public abstract class Event {
    protected final UUID eventId;
    protected final EventType type;
    protected final Location location;
    protected final long startTime;
    protected EventState state;
    protected int durationSeconds;
    protected Set<UUID> participants;
    protected List<Objective> objectives;

    public Event(EventType type, Location location, int durationSeconds) {
        this.eventId = UUID.randomUUID();
        this.type = type;
        this.location = location;
        this.durationSeconds = durationSeconds;
        this.startTime = System.currentTimeMillis();
        this.state = EventState.SPAWNING;
        this.participants = new HashSet<>();
        this.objectives = new ArrayList<>();
    }

    /**
     * Called when the event starts.
     * Override to spawn entities, initialize objectives, etc.
     */
    public abstract void onStart();

    /**
     * Called every tick while the event is active.
     */
    public abstract void onTick();

    /**
     * Called when the event completes successfully.
     */
    public abstract void onComplete();

    /**
     * Called when the event fails or expires.
     */
    public abstract void onFail();

    /**
     * Called when a player interacts with the event.
     */
    public void onPlayerInteract(Player player) {
        // Override in subclasses for custom interactions
    }

    /**
     * Add a player as a participant.
     */
    public void addParticipant(Player player) {
        participants.add(player.getUniqueId());
    }

    /**
     * Check if player is participating.
     */
    public boolean isParticipant(UUID playerId) {
        return participants.contains(playerId);
    }

    /**
     * Get all participants as player objects.
     */
    public List<Player> getParticipants() {
        List<Player> players = new ArrayList<>();
        for (UUID uuid : participants) {
            Player player = org.bukkit.Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }
        return players;
    }

    /**
     * Check if the event has expired based on duration.
     */
    public boolean isExpired() {
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        return elapsed >= durationSeconds;
    }

    /**
     * Get remaining time in seconds.
     */
    public int getRemainingSeconds() {
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        return Math.max(0, durationSeconds - (int) elapsed);
    }

    /**
     * Cleanup method called when event ends (success or failure).
     */
    public abstract void cleanup();

    // Getters
    public UUID getEventId() {
        return eventId;
    }

    public EventType getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    public EventState getState() {
        return state;
    }

    public void setState(EventState state) {
        this.state = state;
    }

    public Set<UUID> getParticipantIds() {
        return new HashSet<>(participants);
    }

    public List<Objective> getObjectives() {
        return objectives;
    }

    public long getStartTime() {
        return startTime;
    }
}
