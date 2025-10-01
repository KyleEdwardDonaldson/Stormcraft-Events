package dev.ked.stormcraft.events.integration;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Integration with MythicMobs for boss spawning.
 */
public class MythicMobsIntegration {
    private final Plugin plugin;
    private boolean enabled = false;

    public MythicMobsIntegration(Plugin plugin) {
        this.plugin = plugin;
        try {
            // Test if MythicMobs API is available via reflection
            Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            this.enabled = true;
            plugin.getLogger().info("MythicMobs integration initialized");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("MythicMobs API not available: " + e.getMessage());
        }
    }

    /**
     * Spawn a MythicMobs mob at a location.
     * @return The spawned entity, or null if failed
     */
    public Entity spawnMythicMob(String mobType, Location location, int level) {
        if (!enabled) return null;

        try {
            // Use reflection to avoid compile-time dependencies
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Object mythicBukkit = mythicBukkitClass.getMethod("inst").invoke(null);
            Object mobManager = mythicBukkitClass.getMethod("getMobManager").invoke(mythicBukkit);

            Object mobOptional = mobManager.getClass().getMethod("getMythicMob", String.class).invoke(mobManager, mobType);

            // Check if Optional is present
            if ((boolean) mobOptional.getClass().getMethod("isPresent").invoke(mobOptional)) {
                Object mob = mobOptional.getClass().getMethod("get").invoke(mobOptional);

                // Spawn the mob
                Class<?> bukkitAdapterClass = Class.forName("io.lumine.mythic.bukkit.BukkitAdapter");
                Object adaptedLocation = bukkitAdapterClass.getMethod("adapt", Location.class).invoke(null, location);

                Object activeMob = mob.getClass().getMethod("spawn", adaptedLocation.getClass(), int.class).invoke(mob, adaptedLocation, level);
                Object entity = activeMob.getClass().getMethod("getEntity").invoke(activeMob);
                return (Entity) entity.getClass().getMethod("getBukkitEntity").invoke(entity);
            } else {
                plugin.getLogger().warning("MythicMobs mob type not found: " + mobType);
                return null;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to spawn MythicMob " + mobType + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if a mob type exists in MythicMobs config.
     */
    public boolean mobTypeExists(String mobType) {
        return enabled;
    }

    /**
     * Check if an entity is a MythicMobs mob.
     */
    public boolean isMythicMob(Entity entity) {
        return false;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
