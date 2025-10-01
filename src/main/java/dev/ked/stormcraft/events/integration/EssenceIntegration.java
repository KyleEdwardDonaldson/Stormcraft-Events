package dev.ked.stormcraft.events.integration;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * Integration with Stormcraft-Essence for essence rewards.
 * Uses Vault economy system to award essence.
 */
public class EssenceIntegration {
    private final Plugin plugin;
    private boolean enabled = false;
    private Plugin essencePlugin;

    public EssenceIntegration(Plugin plugin) {
        this.plugin = plugin;
        initialize();
    }

    private void initialize() {
        // Check if Stormcraft-Essence is loaded
        essencePlugin = Bukkit.getPluginManager().getPlugin("Stormcraft-Essence");
        if (essencePlugin == null) {
            essencePlugin = Bukkit.getPluginManager().getPlugin("StormcraftEssence");
        }

        if (essencePlugin != null) {
            this.enabled = true;
            plugin.getLogger().info("Stormcraft-Essence integration initialized");
        }
    }

    /**
     * Award essence to a player via Vault economy.
     * Essence is stored as economy balance in Stormcraft-Essence.
     */
    public void awardEssence(Player player, int amount, Economy economy) {
        if (!enabled || economy == null) return;

        try {
            economy.depositPlayer(player, amount);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to award essence to " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Award essence to an offline player.
     */
    public void awardEssence(UUID playerId, int amount, Economy economy) {
        if (!enabled || economy == null) return;

        try {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
            economy.depositPlayer(player, amount);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to award essence to " + playerId + ": " + e.getMessage());
        }
    }

    /**
     * Get player's essence balance.
     */
    public double getBalance(Player player, Economy economy) {
        if (!enabled || economy == null) return 0;

        try {
            return economy.getBalance(player);
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Plugin getEssencePlugin() {
        return essencePlugin;
    }
}
