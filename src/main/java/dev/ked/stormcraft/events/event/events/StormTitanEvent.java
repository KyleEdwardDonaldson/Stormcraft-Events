package dev.ked.stormcraft.events.event.events;

import dev.ked.stormcraft.events.StormcraftEventsPlugin;
import dev.ked.stormcraft.events.config.ConfigManager;
import dev.ked.stormcraft.events.event.DamageTracker;
import dev.ked.stormcraft.events.event.Event;
import dev.ked.stormcraft.events.event.EventState;
import dev.ked.stormcraft.events.event.EventType;
import dev.ked.stormcraft.events.integration.MythicMobsIntegration;
import dev.ked.stormcraft.events.objectives.objectives.KillObjective;
import dev.ked.stormcraft.model.TravelingStorm;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Giant;
import org.bukkit.scheduler.BukkitTask;

/**
 * Storm Titan Event: World boss for 10+ players.
 */
public class StormTitanEvent extends Event {
    private final StormcraftEventsPlugin plugin;
    private final ConfigManager config;
    private final MythicMobsIntegration mythicMobs;
    private final TravelingStorm storm;
    private final DamageTracker damageTracker;

    private Entity bossEntity;
    private BukkitTask tickTask;
    private KillObjective objective;

    public StormTitanEvent(StormcraftEventsPlugin plugin, ConfigManager config,
                          MythicMobsIntegration mythicMobs, Location location,
                          TravelingStorm storm) {
        super(EventType.STORM_TITAN, location, config.getEventDuration(EventType.STORM_TITAN));
        this.plugin = plugin;
        this.config = config;
        this.mythicMobs = mythicMobs;
        this.storm = storm;
        this.damageTracker = new DamageTracker(config.getMinDamagePercent());
    }

    @Override
    public void onStart() {
        this.state = EventState.ACTIVE;

        // Spawn world boss
        spawnBoss();

        // Create objective
        objective = new KillObjective("Storm Titan", 1, true);
        objectives.add(objective);

        // Start tick task
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::onTick, 0L, 20L);

        // Announce spawn (broadcast to entire server)
        broadcastSpawn();
    }

    private void spawnBoss() {
        String mythicMobType = config.getMythicMobType(EventType.STORM_TITAN);

        if (mythicMobs != null && mythicMobs.isEnabled() && !mythicMobType.isEmpty()) {
            // Try to spawn MythicMobs boss
            bossEntity = mythicMobs.spawnMythicMob(mythicMobType, location, 2); // Level 2 for scaling
        }

        // Fallback to vanilla giant
        if (bossEntity == null) {
            bossEntity = location.getWorld().spawnEntity(location, EntityType.GIANT);
            if (bossEntity instanceof Giant giant) {
                giant.setCustomName("§c§l⚡ STORM TITAN ⚡");
                giant.setCustomNameVisible(true);
                if (giant.getAttribute(Attribute.MAX_HEALTH) != null) {
                    giant.getAttribute(Attribute.MAX_HEALTH).setBaseValue(1000.0);
                    giant.setHealth(1000.0);
                }
                if (giant.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
                    giant.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(20.0);
                }
            }
        }
    }

    private void broadcastSpawn() {
        String message = "§c§l⚡ STORM TITAN §r§chas spawned at §e" +
                (int)location.getX() + ", " + (int)location.getY() + ", " + (int)location.getZ() + "§c!";
        String subtitle = "§7A massive storm creature has emerged from the tempest...";

        Bukkit.broadcast(net.kyori.adventure.text.Component.text(message));
        Bukkit.broadcast(net.kyori.adventure.text.Component.text(subtitle));
    }

    @Override
    public void onTick() {
        // Check if boss is dead
        if (bossEntity == null || !bossEntity.isValid() || bossEntity.isDead()) {
            onComplete();
            return;
        }

        // Check if expired
        if (isExpired()) {
            onFail();
        }
    }

    @Override
    public void onComplete() {
        this.state = EventState.COMPLETED;

        // Broadcast defeat
        broadcastDefeat();

        cleanup();
    }

    @Override
    public void onFail() {
        this.state = EventState.FAILED;
        cleanup();
    }

    private void broadcastDefeat() {
        Bukkit.broadcast(net.kyori.adventure.text.Component.text(
                "§a§l⚔ STORM TITAN §r§ahas been defeated!"));

        // Show top 3 contributors
        var topContributors = damageTracker.getTopContributors(3);
        if (!topContributors.isEmpty()) {
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§7Top Contributors:"));

            int rank = 1;
            for (var entry : topContributors) {
                org.bukkit.entity.Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    double damage = entry.getValue();
                    double percent = damageTracker.getDamagePercent(entry.getKey());

                    String msg = String.format("§e  %d. %s §7- §f%.0f damage §7(§6%.1f%%§7)",
                            rank, player.getName(), damage, percent);
                    Bukkit.broadcast(net.kyori.adventure.text.Component.text(msg));
                    rank++;
                }
            }
        }
    }

    @Override
    public void cleanup() {
        // Remove boss if still alive
        if (bossEntity != null && bossEntity.isValid() && !bossEntity.isDead()) {
            bossEntity.remove();
        }

        // Cancel tick task
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    public Entity getBossEntity() {
        return bossEntity;
    }

    public DamageTracker getDamageTracker() {
        return damageTracker;
    }
}
