package dev.ked.stormcraft.events.command;

import dev.ked.stormcraft.events.StormcraftEventsPlugin;
import dev.ked.stormcraft.events.config.ConfigManager;
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
import java.util.stream.Collectors;

/**
 * Command handler for /stormevent
 */
public class StormEventCommand implements CommandExecutor, TabCompleter {
    private final StormcraftEventsPlugin plugin;
    private final EventManager eventManager;
    private final ConfigManager config;

    public StormEventCommand(StormcraftEventsPlugin plugin, EventManager eventManager, ConfigManager config) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.config = config;
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
        sender.sendMessage(Component.text("§a[Events] Configuration reloaded"));
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

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("§6§l=== Storm Events Commands ==="));
        sender.sendMessage(Component.text("§e/stormevent list §7- List active events"));
        sender.sendMessage(Component.text("§e/stormevent info §7- Show plugin information"));
        sender.sendMessage(Component.text("§e/stormevent cooldowns §7- View event cooldowns"));

        if (sender.hasPermission("stormcraft.events.admin")) {
            sender.sendMessage(Component.text("§e/stormevent reload §7- Reload configuration"));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                     @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("list", "info", "cooldowns"));

            if (sender.hasPermission("stormcraft.events.admin")) {
                completions.add("reload");
            }

            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
