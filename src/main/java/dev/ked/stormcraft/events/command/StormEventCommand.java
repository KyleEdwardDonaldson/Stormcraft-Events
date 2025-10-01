package dev.ked.stormcraft.events.command;

import dev.ked.stormcraft.events.StormcraftEventsPlugin;
import dev.ked.stormcraft.events.config.ConfigManager;
import dev.ked.stormcraft.events.difficulty.DifficultyCalculator;
import dev.ked.stormcraft.events.difficulty.DifficultyMultiplier;
import dev.ked.stormcraft.events.difficulty.PlayerDensityTracker;
import dev.ked.stormcraft.events.difficulty.ThreatLevel;
import dev.ked.stormcraft.events.event.Event;
import dev.ked.stormcraft.events.event.EventManager;
import dev.ked.stormcraft.events.event.EventType;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Command handler for /stormevent
 */
public class StormEventCommand implements CommandExecutor, TabCompleter {
    private final StormcraftEventsPlugin plugin;
    private final EventManager eventManager;
    private final ConfigManager config;
    private PlayerDensityTracker densityTracker;
    private DifficultyCalculator difficultyCalculator;

    public StormEventCommand(StormcraftEventsPlugin plugin, EventManager eventManager, ConfigManager config) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.config = config;
    }

    /**
     * Set difficulty system components (called after EventManager initialization).
     */
    public void setDifficultySystem(PlayerDensityTracker densityTracker, DifficultyCalculator difficultyCalculator) {
        this.densityTracker = densityTracker;
        this.difficultyCalculator = difficultyCalculator;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                            @NotNull String label, String[] args) {

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            case "info" -> handleInfo(sender);
            case "cooldowns" -> handleCooldowns(sender);
            case "difficulty" -> handleDifficulty(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleList(CommandSender sender) {
        var events = eventManager.getActiveEvents();

        if (events.isEmpty()) {
            sender.sendMessage(Component.text("§6[Events] §fNo active events"));
            return;
        }

        sender.sendMessage(Component.text("§6[Events] §fActive Events:"));
        for (Event event : events) {
            String loc = String.format("%d, %d, %d",
                    event.getLocation().getBlockX(),
                    event.getLocation().getBlockY(),
                    event.getLocation().getBlockZ());

            sender.sendMessage(Component.text(String.format("§e- %s §7at §f%s §7(§e%d§7s remaining)",
                    event.getType().getDisplayName(), loc, event.getRemainingSeconds())));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("stormcraft.events.admin")) {
            sender.sendMessage(Component.text("§cYou don't have permission to use this command"));
            return;
        }

        config.loadConfigs();
        eventManager.reloadDifficultyConfig();
        sender.sendMessage(Component.text("§a[Events] Configuration reloaded"));
        sender.sendMessage(Component.text("§7Difficulty system reloaded with new weights"));
    }

    private void handleInfo(CommandSender sender) {
        sender.sendMessage(Component.text("§6§l=== Stormcraft Events ==="));
        sender.sendMessage(Component.text("§fVersion: §e" + plugin.getDescription().getVersion()));
        sender.sendMessage(Component.text("§fActive Events: §e" + eventManager.getActiveEvents().size()));

        // List event types
        sender.sendMessage(Component.text("§fEvent Types:"));
        for (EventType type : EventType.values()) {
            boolean enabled = config.isEventEnabled(type);
            String status = enabled ? "§a✓" : "§c✗";
            sender.sendMessage(Component.text(String.format("  %s §f%s §7- %s",
                    status, type.getDisplayName(), type.getDescription())));
        }
    }

    private void handleCooldowns(CommandSender sender) {
        sender.sendMessage(Component.text("§6[Events] §fEvent Cooldowns:"));

        for (EventType type : EventType.values()) {
            int remaining = eventManager.getRemainingCooldown(type);

            if (remaining > 0) {
                int minutes = remaining / 60;
                int seconds = remaining % 60;
                sender.sendMessage(Component.text(String.format("§e- %s: §c%dm %ds",
                        type.getDisplayName(), minutes, seconds)));
            } else {
                sender.sendMessage(Component.text(String.format("§e- %s: §aReady",
                        type.getDisplayName())));
            }
        }
    }

    private void handleDifficulty(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("§cThis command can only be used by players"));
            return;
        }

        if (densityTracker == null || difficultyCalculator == null) {
            sender.sendMessage(Component.text("§c[Events] Difficulty system not initialized"));
            return;
        }

        if (!config.isDifficultyEnabled()) {
            sender.sendMessage(Component.text("§c[Events] Difficulty system is disabled in config"));
            return;
        }

        // Parse radius (default 50)
        double radius = config.getDifficultyScanRadius();
        if (args.length > 1) {
            try {
                radius = Double.parseDouble(args[1]);
                if (radius <= 0 || radius > 200) {
                    sender.sendMessage(Component.text("§c[Events] Radius must be between 1 and 200"));
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("§c[Events] Invalid radius. Usage: /stormevent difficulty [radius]"));
                return;
            }
        }

        // Get nearby players
        List<Player> nearbyPlayers = densityTracker.getNearbyPlayers(player.getLocation(), radius);

        if (nearbyPlayers.isEmpty()) {
            sender.sendMessage(Component.text("§6[Storm Events] §cNo players within " + (int)radius + " blocks"));
            return;
        }

        // Calculate difficulty
        DifficultyMultiplier difficulty = difficultyCalculator.calculate(player.getLocation(), nearbyPlayers);

        // Display results
        sender.sendMessage(Component.text("§6[Storm Events] §fDifficulty Analysis:"));
        sender.sendMessage(Component.text(String.format("§7Location: §f%s §7(X: %d, Z: %d)",
            difficulty.isInWilderness() ? "Wilderness" : "Town Claim",
            player.getLocation().getBlockX(),
            player.getLocation().getBlockZ())));

        sender.sendMessage(Component.text(String.format("§7Nearby Players: §f%d §7(Party: %d, Others: %d)",
            nearbyPlayers.size(),
            difficulty.getPartyMembers(),
            difficulty.getProximityPlayers())));

        sender.sendMessage(Component.text("§7Base Multiplier: §e1.0x"));

        if (difficulty.getPartyBonus() > 0) {
            sender.sendMessage(Component.text(String.format("§7  + Party Bonus: §e+%.1fx §7(%d members)",
                difficulty.getPartyBonus(), difficulty.getPartyMembers())));
        }

        if (difficulty.getProximityBonus() > 0) {
            sender.sendMessage(Component.text(String.format("§7  + Proximity: §e+%.1fx §7(%d nearby)",
                difficulty.getProximityBonus(), difficulty.getProximityPlayers())));
        }

        if (difficulty.getWildernessBonus() > 0) {
            sender.sendMessage(Component.text(String.format("§7  + Wilderness: §e+%.1fx",
                difficulty.getWildernessBonus())));
        }

        if (difficulty.getStormBonus() > 0) {
            sender.sendMessage(Component.text(String.format("§7  + Storm: §e+%.1fx",
                difficulty.getStormBonus())));
        }

        sender.sendMessage(Component.text(String.format("§7Final Multiplier: §6%.1fx",
            difficulty.getMultiplier())));

        sender.sendMessage(Component.text("§7Threat Level: " + difficulty.getThreatLevel().getDisplayName()));

        // Show event chances
        Map<ThreatLevel, Map<EventType, Integer>> weights = difficultyCalculator.getEventWeights();
        Map<EventType, Integer> currentWeights = weights.get(difficulty.getThreatLevel());

        if (currentWeights != null) {
            int totalWeight = currentWeights.values().stream().mapToInt(Integer::intValue).sum();
            sender.sendMessage(Component.text("§7Event Chances:"));

            for (Map.Entry<EventType, Integer> entry : currentWeights.entrySet()) {
                int weight = entry.getValue();
                int chance = totalWeight > 0 ? (weight * 100 / totalWeight) : 0;
                String color = chance >= 30 ? "§f" : (chance >= 15 ? "§e" : (chance > 0 ? "§7" : "§8"));

                sender.sendMessage(Component.text(String.format("§7  %s: %s%d%%",
                    entry.getKey().getDisplayName(), color, chance)));
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("§6§l=== Storm Events Commands ==="));
        sender.sendMessage(Component.text("§e/stormevent list §7- List active events"));
        sender.sendMessage(Component.text("§e/stormevent info §7- Show plugin information"));
        sender.sendMessage(Component.text("§e/stormevent cooldowns §7- View event cooldowns"));
        sender.sendMessage(Component.text("§e/stormevent difficulty [radius] §7- Show difficulty analysis"));

        if (sender.hasPermission("stormcraft.events.admin")) {
            sender.sendMessage(Component.text("§e/stormevent reload §7- Reload configuration"));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                     @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("list", "info", "cooldowns", "difficulty"));

            if (sender.hasPermission("stormcraft.events.admin")) {
                completions.add("reload");
            }

            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("difficulty")) {
            return Arrays.asList("25", "50", "75", "100");
        }

        return new ArrayList<>();
    }
}
