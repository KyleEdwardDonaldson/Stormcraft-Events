package dev.ked.stormcraft.events.ui;

import dev.ked.stormcraft.events.difficulty.DifficultyMultiplier;
import dev.ked.stormcraft.events.difficulty.ThreatLevel;
import dev.ked.stormcraft.events.event.Event;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;

/**
 * Handles event notifications to players.
 */
public class EventNotifier {

    /**
     * Announce event spawn to nearby players.
     */
    public static void announceSpawn(Event event, List<Player> nearbyPlayers) {
        String eventName = event.getType().getDisplayName();
        Location loc = event.getLocation();
        DifficultyMultiplier difficulty = event.getDifficulty();

        for (Player player : nearbyPlayers) {
            // Chat message with player count
            int playerCount = nearbyPlayers.size();
            player.sendMessage(Component.text("§6[Storm Event] §f⚡ §b" + eventName +
                    " §fdetected - §e" + playerCount + " player" + (playerCount > 1 ? "s" : "") + " nearby!"));

            // Show threat level if difficulty is set
            if (difficulty != null) {
                ThreatLevel threatLevel = difficulty.getThreatLevel();
                String threatMessage = getThreatMessage(threatLevel, difficulty.getMultiplier());
                player.sendMessage(Component.text(threatMessage));
            }

            player.sendMessage(Component.text("§7Location: " + getDistance(player, loc) + " blocks away"));

            // Title
            player.showTitle(Title.title(
                    Component.text("§c⚡ " + eventName.toUpperCase() + " ⚡"),
                    Component.text("§7" + event.getType().getDescription()),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofSeconds(1))
            ));
        }
    }

    /**
     * Get a formatted threat level message with rewards hint.
     */
    private static String getThreatMessage(ThreatLevel level, double multiplier) {
        return switch (level) {
            case LOW -> String.format("§a⚠ Threat Level: %s §7(%.1fx) - Standard rewards",
                level.getDisplayName(), multiplier);
            case MEDIUM -> String.format("§e⚠ Threat Level: %s §7(%.1fx) - Increased rewards",
                level.getDisplayName(), multiplier);
            case HIGH -> String.format("§6⚠ Threat Level: %s §7(%.1fx) - High rewards!",
                level.getDisplayName(), multiplier);
            case EXTREME -> String.format("§c⚠ Threat Level: %s §7(%.1fx) - Massive rewards!",
                level.getDisplayName(), multiplier);
        };
    }

    /**
     * Announce event completion.
     */
    public static void announceCompletion(Event event, List<Player> participants) {
        String eventName = event.getType().getDisplayName();

        for (Player player : participants) {
            player.sendMessage(Component.text("§6[Storm Event] §a" + eventName +
                    " §fcompleted! Rewards distributed."));
        }
    }

    /**
     * Announce event failure.
     */
    public static void announceFailed(Event event, List<Player> participants) {
        String eventName = event.getType().getDisplayName();

        for (Player player : participants) {
            player.sendMessage(Component.text("§6[Storm Event] §c" + eventName + " §ffailed!"));
        }
    }

    /**
     * Send action bar update to player.
     */
    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(Component.text(message));
    }

    private static int getDistance(Player player, Location location) {
        if (player.getWorld() != location.getWorld()) return -1;
        return (int) player.getLocation().distance(location);
    }
}
