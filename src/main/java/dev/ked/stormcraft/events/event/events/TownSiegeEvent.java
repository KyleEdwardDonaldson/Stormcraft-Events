package dev.ked.stormcraft.events.event.events;

import com.palmergames.bukkit.towny.object.Town;
import dev.ked.stormcraft.events.StormcraftEventsPlugin;
import dev.ked.stormcraft.events.config.ConfigManager;
import dev.ked.stormcraft.events.event.Event;
import dev.ked.stormcraft.events.event.EventState;
import dev.ked.stormcraft.events.event.EventType;
import dev.ked.stormcraft.events.integration.TownsAndNationsIntegration;
import dev.ked.stormcraft.events.integration.TownyIntegration;
import dev.ked.stormcraft.events.objectives.objectives.DefendObjective;
import dev.ked.stormcraft.model.TravelingStorm;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Town Siege Event: Defend a town from waves of storm-corrupted mobs.
 */
public class TownSiegeEvent extends Event {
    private final StormcraftEventsPlugin plugin;
    private final ConfigManager config;
    private final TownyIntegration towny;
    private final TownsAndNationsIntegration tan;
    private final TravelingStorm storm;
    private final List<Entity> spawnedMobs = new ArrayList<>();

    private DefendObjective objective;
    private BukkitTask tickTask;
    private BukkitTask waveTask;
    private int currentWave = 0;
    private int totalWaves;
    private int mobsPerWave;
    private String townName;

    public TownSiegeEvent(StormcraftEventsPlugin plugin, ConfigManager config,
                         TownyIntegration towny, TownsAndNationsIntegration tan,
                         Location location, TravelingStorm storm) {
        super(EventType.TOWN_SIEGE, location, config.getEventDuration(EventType.TOWN_SIEGE));
        this.plugin = plugin;
        this.config = config;
        this.towny = towny;
        this.tan = tan;
        this.storm = storm;

        this.totalWaves = config.getSiegeWaveCount();
        this.mobsPerWave = config.getSiegeMobsPerWave();

        // Determine town name
        determineTownName();
    }

    private void determineTownName() {
        if (towny != null && towny.isEnabled()) {
            Town town = towny.getTownAt(location);
            if (town != null) {
                townName = town.getName();
            }
        } else if (tan != null && tan.isEnabled()) {
            String territoryName = tan.getTerritoryNameAt(location);
            if (territoryName != null) {
                townName = territoryName;
            }
        }

        if (townName == null) {
            townName = "Unknown Town";
        }
    }

    @Override
    public void onStart() {
        this.state = EventState.ACTIVE;

        // Create objective
        objective = new DefendObjective(totalWaves);
        objectives.add(objective);

        // Announce to town
        announceSiege();

        // Start tick task
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::onTick, 0L, 20L);

        // Start first wave
        scheduleNextWave(5); // 5 second delay for first wave
    }

    private void announceSiege() {
        String message = "§c§l[SIEGE] §c" + townName + " §fis under attack by storm-corrupted forces!";

        // Notify players near the town
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() == location.getWorld()) {
                double distance = player.getLocation().distance(location);
                if (distance <= config.getAnnounceRadius()) {
                    player.sendMessage(net.kyori.adventure.text.Component.text(message));
                }
            }
        }
    }

    private void scheduleNextWave(int delaySeconds) {
        waveTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            currentWave++;
            objective.nextWave();
            spawnWave();

            // Schedule next wave if not complete
            if (currentWave < totalWaves) {
                scheduleNextWave(60); // 60 seconds between waves
            }
        }, delaySeconds * 20L);
    }

    private void spawnWave() {
        // Spawn mobs around town perimeter
        int spawnPoints = 4;
        EntityType[] mobTypes = {
                EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER,
                EntityType.SPIDER, EntityType.WITCH
        };

        for (int i = 0; i < spawnPoints; i++) {
            double angle = (i / (double) spawnPoints) * 2 * Math.PI;
            double distance = 50; // 50 blocks from town center

            double x = location.getX() + (distance * Math.cos(angle));
            double z = location.getZ() + (distance * Math.sin(angle));

            Location spawnLoc = new Location(location.getWorld(), x, location.getY(), z);
            spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);

            // Spawn mobs at this point
            int mobsPerPoint = mobsPerWave / spawnPoints;
            for (int j = 0; j < mobsPerPoint; j++) {
                EntityType type = mobTypes[ThreadLocalRandom.current().nextInt(mobTypes.length)];
                Entity mob = spawnLoc.getWorld().spawnEntity(spawnLoc, type);
                mob.setCustomName("§5Storm-Corrupted " + type.name());
                spawnedMobs.add(mob);
            }
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

        // Announce victory
        String message = "§a§l[SIEGE] §a" + townName + " §fhas successfully defended against the siege!";
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() == location.getWorld()) {
                double distance = player.getLocation().distance(location);
                if (distance <= config.getAnnounceRadius()) {
                    player.sendMessage(net.kyori.adventure.text.Component.text(message));
                }
            }
        }

        cleanup();
    }

    @Override
    public void onFail() {
        this.state = EventState.FAILED;

        // Announce failure
        String message = "§c§l[SIEGE] §c" + townName + " §ffailed to defend against the siege!";
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() == location.getWorld()) {
                double distance = player.getLocation().distance(location);
                if (distance <= config.getAnnounceRadius()) {
                    player.sendMessage(net.kyori.adventure.text.Component.text(message));
                }
            }
        }

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

    public String getTownName() {
        return townName;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public int getTotalWaves() {
        return totalWaves;
    }
}
