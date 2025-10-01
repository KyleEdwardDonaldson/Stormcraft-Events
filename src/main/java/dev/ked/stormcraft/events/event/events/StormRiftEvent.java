package dev.ked.stormcraft.events.event.events;

import dev.ked.stormcraft.events.StormcraftEventsPlugin;
import dev.ked.stormcraft.events.config.ConfigManager;
import dev.ked.stormcraft.events.event.Event;
import dev.ked.stormcraft.events.event.EventState;
import dev.ked.stormcraft.events.event.EventType;
import dev.ked.stormcraft.events.objectives.objectives.DefendObjective;
import dev.ked.stormcraft.model.TravelingStorm;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Storm Rift Event: Wave defense against storm-corrupted mobs.
 */
public class StormRiftEvent extends Event {
    private final StormcraftEventsPlugin plugin;
    private final ConfigManager config;
    private final TravelingStorm storm;
    private final List<Entity> spawnedMobs = new ArrayList<>();

    private DefendObjective objective;
    private BukkitTask tickTask;
    private BukkitTask waveTask;
    private int currentWave = 0;
    private int totalWaves;
    private int mobsPerWave;

    public StormRiftEvent(StormcraftEventsPlugin plugin, ConfigManager config,
                         Location location, TravelingStorm storm) {
        super(EventType.STORM_RIFT, location, config.getEventDuration(EventType.STORM_RIFT));
        this.plugin = plugin;
        this.config = config;
        this.storm = storm;

        this.totalWaves = config.getConfig().getInt("events.types.STORM_RIFT.waveCount", 5);
        this.mobsPerWave = config.getConfig().getInt("events.types.STORM_RIFT.mobsPerWave", 10);
    }

    @Override
    public void onStart() {
        this.state = EventState.ACTIVE;

        // Create objective
        objective = new DefendObjective(totalWaves);
        objectives.add(objective);

        // Start tick task
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::onTick, 0L, 20L);

        // Start first wave
        scheduleNextWave(2); // 2 second delay for first wave
    }

    private void scheduleNextWave(int delaySeconds) {
        waveTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            currentWave++;
            objective.nextWave();
            spawnWave();

            // Schedule next wave if not complete
            if (currentWave < totalWaves) {
                scheduleNextWave(30); // 30 seconds between waves
            }
        }, delaySeconds * 20L);
    }

    private void spawnWave() {
        // Spawn rift particle effect
        location.getWorld().spawnParticle(Particle.PORTAL, location, 100, 2, 2, 2, 0.5);

        EntityType[] mobTypes = {
                EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER,
                EntityType.CREEPER, EntityType.WITCH
        };

        for (int i = 0; i < mobsPerWave; i++) {
            Location spawnLoc = getRandomLocationNear(location, 15);
            EntityType type = mobTypes[ThreadLocalRandom.current().nextInt(mobTypes.length)];

            Entity mob = location.getWorld().spawnEntity(spawnLoc, type);
            mob.setCustomName("§5Storm-Corrupted " + type.name());
            spawnedMobs.add(mob);
        }
    }

    @Override
    public void onTick() {
        // Remove dead mobs from list
        spawnedMobs.removeIf(mob -> !mob.isValid() || mob.isDead());

        // Check if all waves complete
        if (objective.isComplete()) {
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
        // Remove remaining mobs
        for (Entity mob : spawnedMobs) {
            if (mob.isValid() && !mob.isDead()) {
                mob.remove();
            }
        }
        spawnedMobs.clear();

        // Cancel tasks
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (waveTask != null) {
            waveTask.cancel();
            waveTask = null;
        }
    }

    private Location getRandomLocationNear(Location center, double radius) {
        double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
        double distance = ThreadLocalRandom.current().nextDouble() * radius;

        double x = center.getX() + (distance * Math.cos(angle));
        double z = center.getZ() + (distance * Math.sin(angle));

        Location loc = new Location(center.getWorld(), x, center.getY(), z);
        loc.setY(loc.getWorld().getHighestBlockYAt(loc) + 1);
        return loc;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public int getTotalWaves() {
        return totalWaves;
    }
}
