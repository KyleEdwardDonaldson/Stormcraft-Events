package dev.ked.stormcraft.events.ui;

import dev.ked.stormcraft.events.event.Event;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages boss bars for event progress and boss health.
 */
public class EventHUD {
    private final Map<UUID, BossBar> eventBossBars = new HashMap<>();

    /**
     * Create and show boss bar for an event.
     */
    public void showEventBar(Event event, Player player, String title, float progress) {
        UUID eventId = event.getEventId();

        BossBar bar = eventBossBars.get(eventId);
        if (bar == null) {
            bar = BossBar.bossBar(
                    Component.text(title),
                    progress,
                    BossBar.Color.PURPLE,
                    BossBar.Overlay.PROGRESS
            );
            eventBossBars.put(eventId, bar);
        }

        player.showBossBar(bar);
    }

    /**
     * Update boss bar progress.
     */
    public void updateEventBar(UUID eventId, String title, float progress) {
        BossBar bar = eventBossBars.get(eventId);
        if (bar != null) {
            bar.name(Component.text(title));
            bar.progress(Math.max(0f, Math.min(1f, progress)));
        }
    }

    /**
     * Remove and hide boss bar.
     */
    public void removeEventBar(UUID eventId, Player player) {
        BossBar bar = eventBossBars.get(eventId);
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    /**
     * Create boss health bar (red, for boss mobs).
     */
    public BossBar createBossHealthBar(String bossName, float health) {
        return BossBar.bossBar(
                Component.text("§c[BOSS] §f" + bossName + " §c❤ " + (int)(health * 100) + "%"),
                health,
                BossBar.Color.RED,
                BossBar.Overlay.NOTCHED_20
        );
    }

    /**
     * Clear all boss bars.
     */
    public void clearAll() {
        eventBossBars.clear();
    }
}
