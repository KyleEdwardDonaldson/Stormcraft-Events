package dev.ked.stormcraft.events;

import dev.ked.stormcraft.StormcraftPlugin;
import dev.ked.stormcraft.events.command.StormEventCommand;
import dev.ked.stormcraft.events.config.ConfigManager;
import dev.ked.stormcraft.events.event.EventManager;
import dev.ked.stormcraft.events.integration.*;
import dev.ked.stormcraft.events.spawn.DensityTracker;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for Stormcraft Events.
 * Manages dynamic storm-themed events with density-based spawning.
 */
public class StormcraftEventsPlugin extends JavaPlugin {
    private ConfigManager configManager;
    private StormcraftIntegration stormcraftIntegration;
    private MythicMobsIntegration mythicMobsIntegration;
    private TownyIntegration townyIntegration;
    private TownsAndNationsIntegration tanIntegration;
    private EssenceIntegration essenceIntegration;
    private EventManager eventManager;
    private DensityTracker densityTracker;
    private Economy economy;

    @Override
    public void onEnable() {
        getLogger().info("Starting Stormcraft-Events...");

        // Check for required dependency
        if (!setupStormcraft()) {
            getLogger().severe("Stormcraft plugin not found! This plugin requires Stormcraft to function.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize configuration
        configManager = new ConfigManager(this);
        configManager.loadConfigs();

        // Setup economy (Vault)
        if (!setupEconomy()) {
            getLogger().warning("Vault not found - economy rewards disabled");
        }

        // Setup integrations
        setupIntegrations();

        // Initialize core systems
        densityTracker = new DensityTracker(this, configManager);
        eventManager = new EventManager(this, configManager, stormcraftIntegration,
                                       mythicMobsIntegration, townyIntegration,
                                       tanIntegration, essenceIntegration,
                                       densityTracker, economy);

        // Start systems
        densityTracker.start();
        eventManager.start();

        // Register commands
        registerCommands();

        getLogger().info("Stormcraft-Events enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Stopping Stormcraft-Events...");

        // Stop systems
        if (eventManager != null) {
            eventManager.shutdown();
        }
        if (densityTracker != null) {
            densityTracker.stop();
        }

        getLogger().info("Stormcraft-Events disabled.");
    }

    private boolean setupStormcraft() {
        if (getServer().getPluginManager().getPlugin("Stormcraft") == null) {
            return false;
        }
        stormcraftIntegration = new StormcraftIntegration(this);
        return stormcraftIntegration.isEnabled();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void setupIntegrations() {
        // MythicMobs (optional)
        if (Bukkit.getPluginManager().getPlugin("MythicMobs") != null) {
            mythicMobsIntegration = new MythicMobsIntegration(this);
            if (mythicMobsIntegration.isEnabled()) {
                getLogger().info("MythicMobs integration enabled");
            }
        } else {
            getLogger().info("MythicMobs not found - boss events will use vanilla mobs");
        }

        // Towny (optional)
        if (Bukkit.getPluginManager().getPlugin("Towny") != null) {
            townyIntegration = new TownyIntegration(this, configManager);
            if (townyIntegration.isEnabled()) {
                getLogger().info("Towny integration enabled");
            }
        }

        // TownsAndNations (optional, mutually exclusive with Towny)
        if (Bukkit.getPluginManager().getPlugin("TownsAndNations") != null && townyIntegration == null) {
            tanIntegration = new TownsAndNationsIntegration(this, configManager);
            if (tanIntegration.isEnabled()) {
                getLogger().info("TownsAndNations integration enabled");
            }
        }

        // Stormcraft-Essence (optional)
        if (Bukkit.getPluginManager().getPlugin("Stormcraft-Essence") != null ||
            Bukkit.getPluginManager().getPlugin("StormcraftEssence") != null) {
            essenceIntegration = new EssenceIntegration(this);
            if (essenceIntegration.isEnabled()) {
                getLogger().info("Stormcraft-Essence integration enabled");
            }
        }
    }

    private void registerCommands() {
        StormEventCommand command = new StormEventCommand(this, eventManager, configManager);
        getCommand("stormevent").setExecutor(command);
        getCommand("stormevent").setTabCompleter(command);
    }

    // Getters
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public StormcraftIntegration getStormcraftIntegration() {
        return stormcraftIntegration;
    }

    public MythicMobsIntegration getMythicMobsIntegration() {
        return mythicMobsIntegration;
    }

    public TownyIntegration getTownyIntegration() {
        return townyIntegration;
    }

    public TownsAndNationsIntegration getTanIntegration() {
        return tanIntegration;
    }

    public EssenceIntegration getEssenceIntegration() {
        return essenceIntegration;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public Economy getEconomy() {
        return economy;
    }
}
