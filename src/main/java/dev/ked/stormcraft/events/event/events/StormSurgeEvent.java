package dev.ked.stormcraft.events.event.events;

import dev.ked.stormcraft.events.StormcraftEventsPlugin;
import dev.ked.stormcraft.events.config.ConfigManager;
import dev.ked.stormcraft.events.event.Event;
import dev.ked.stormcraft.events.event.EventState;
import dev.ked.stormcraft.events.event.EventType;
import dev.ked.stormcraft.events.objectives.objectives.CollectObjective;
import dev.ked.stormcraft.model.TravelingStorm;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Storm Surge Event: Collect storm crystals before they disappear.
 */
public class StormSurgeEvent extends Event {
    private final StormcraftEventsPlugin plugin;
    private final ConfigManager config;
    private final TravelingStorm storm;
    private final List<Item> crystals = new ArrayList<>();
    private BukkitTask tickTask;
    private CollectObjective objective;

    public StormSurgeEvent(StormcraftEventsPlugin plugin, ConfigManager config,
                          Location location, TravelingStorm storm) {
        super(EventType.STORM_SURGE, location, config.getEventDuration(EventType.STORM_SURGE));
        this.plugin = plugin;
        this.config = config;
        this.storm = storm;
    }

    @Override
    public void onStart() {
        this.state = EventState.ACTIVE;

        // Create objective
        int crystalCount = config.getConfig().getInt("events.types.STORM_SURGE.crystalCount", 5);
        objective = new CollectObjective(crystalCount);
        objectives.add(objective);

        // Spawn crystals
        spawnCrystals(crystalCount);

        // Start tick task
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::onTick, 0L, 20L);
    }

    private void spawnCrystals(int count) {
        for (int i = 0; i < count; i++) {
            Location spawnLoc = getRandomLocationNear(location, 30);
            spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);

            ItemStack crystal = createStormCrystal();
            Item item = spawnLoc.getWorld().dropItem(spawnLoc, crystal);
            item.setPickupDelay(20); // 1 second delay
            item.setGlowing(true);
            crystals.add(item);
        }
    }

    private ItemStack createStormCrystal() {
        ItemStack crystal = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = crystal.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text("§b§lStorm Crystal"));
        meta.lore(List.of(
                net.kyori.adventure.text.Component.text("§7Crackling with storm energy"),
                net.kyori.adventure.text.Component.text("§7Collect these during Storm Surge events!")
        ));
        crystal.setItemMeta(meta);
        return crystal;
    }

    @Override
    public void onTick() {
        // Check if expired
        if (isExpired()) {
            onFail();
            return;
        }

        // Remove dead crystals
        crystals.removeIf(item -> !item.isValid() || item.isDead());

        // Check completion
        if (objective.isComplete()) {
            onComplete();
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
        // Remove remaining crystals
        for (Item item : crystals) {
            if (item.isValid()) {
                item.remove();
            }
        }
        crystals.clear();

        // Cancel tick task
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    private Location getRandomLocationNear(Location center, double radius) {
        double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
        double distance = ThreadLocalRandom.current().nextDouble() * radius;

        double x = center.getX() + (distance * Math.cos(angle));
        double z = center.getZ() + (distance * Math.sin(angle));

        return new Location(center.getWorld(), x, center.getY(), z);
    }

    public CollectObjective getObjective() {
        return objective;
    }
}
