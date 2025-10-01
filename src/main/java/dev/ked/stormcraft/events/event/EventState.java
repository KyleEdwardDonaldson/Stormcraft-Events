package dev.ked.stormcraft.events.event;

/**
 * Represents the current state of an event.
 */
public enum EventState {
    SPAWNING,   // Event is being set up
    ACTIVE,     // Event is running
    COMPLETED,  // Event completed successfully
    FAILED      // Event failed or expired
}
