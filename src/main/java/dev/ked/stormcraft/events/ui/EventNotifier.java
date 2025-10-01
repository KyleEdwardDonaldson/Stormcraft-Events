package dev.ked.stormcraft.events.ui;

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

        for (Player player : nearbyPlayers) {
            // Chat message
            player.sendMessage(Component.text("§6[Storm Event] §fA §b" + eventName +
                    " §fhas spawned near you! §7(" + getDistance(player, loc) + " blocks away)"));

            // Title
            player.showTitle(Title.title(
                    Component.text("§c⚡ " + eventName.toUpperCase() + " ⚡"),
                    Component.text("§7" + event.getType().getDescription()),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofSeconds(1))
            ));
        }
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
