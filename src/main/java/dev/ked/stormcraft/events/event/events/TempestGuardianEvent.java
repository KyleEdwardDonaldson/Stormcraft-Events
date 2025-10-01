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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Ravager;
import org.bukkit.scheduler.BukkitTask;

/**
 * Tempest Guardian Event: Mini-boss fight with 3+ players.
 */
public class TempestGuardianEvent extends Event {
    private final StormcraftEventsPlugin plugin;
    private final ConfigManager config;
    private final MythicMobsIntegration mythicMobs;
    private final TravelingStorm storm;
    private final DamageTracker damageTracker;

    private Entity bossEntity;
    private BukkitTask tickTask;
    private KillObjective objective;

    public TempestGuardianEvent(StormcraftEventsPlugin plugin, ConfigManager config,
                               MythicMobsIntegration mythicMobs, Location location,
                               TravelingStorm storm) {
        super(EventType.TEMPEST_GUARDIAN, location, config.getEventDuration(EventType.TEMPEST_GUARDIAN));
        this.plugin = plugin;
        this.config = config;
        this.mythicMobs = mythicMobs;
        this.storm = storm;
        this.damageTracker = new DamageTracker(config.getMinDamagePercent());
    }

    @Override
    public void onStart() {
        this.state = EventState.ACTIVE;

        // Spawn boss
        spawnBoss();

        // Create objective
        objective = new KillObjective("Tempest Guardian", 1, true);
        objectives.add(objective);

        // Start tick task
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::onTick, 0L, 20L);
    }

    private void spawnBoss() {
        String mythicMobType = config.getMythicMobType(EventType.TEMPEST_GUARDIAN);

        if (mythicMobs != null && mythicMobs.isEnabled() && !mythicMobType.isEmpty()) {
            // Try to spawn MythicMobs boss
            bossEntity = mythicMobs.spawnMythicMob(mythicMobType, location, 1);
        }

        // Fallback to vanilla mob if MythicMobs fails
        if (bossEntity == null) {
            bossEntity = location.getWorld().spawnEntity(location, EntityType.RAVAGER);
            if (bossEntity instanceof Ravager ravager) {
                ravager.setCustomName("§c§lTempest Guardian");
                ravager.setCustomNameVisible(true);
                if (ravager.getAttribute(Attribute.MAX_HEALTH) != null) {
                    ravager.getAttribute(Attribute.MAX_HEALTH).setBaseValue(200.0);
                    ravager.setHealth(200.0);
                }
            }
        }
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
        cleanup();
    }

    @Override
    public void onFail() {
        this.state = EventState.FAILED;
        cleanup();
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
