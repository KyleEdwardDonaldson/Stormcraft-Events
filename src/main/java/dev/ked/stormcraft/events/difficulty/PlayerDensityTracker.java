package dev.ked.stormcraft.events.difficulty;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Tracks player density and party relationships for difficulty calculations.
 * Uses caching to minimize performance impact.
 */
public class PlayerDensityTracker {
    private final JavaPlugin plugin;
    private final double defaultScanRadius;

    // Cache for nearby players (location hash -> player list)
    private final Map<String, CachedPlayerList> nearbyPlayersCache = new ConcurrentHashMap<>();
    private final long cacheExpiryMs = 5000; // 5 seconds

    // Cache for party memberships (player UUID -> party members set)
    private final Map<UUID, CachedPartyMembers> partyCache = new ConcurrentHashMap<>();

    // Party plugin integration (if available)
    private Object partyPlugin = null;
    private boolean hasPartyPlugin = false;

    public PlayerDensityTracker(JavaPlugin plugin, double scanRadius) {
        this.plugin = plugin;
        this.defaultScanRadius = scanRadius;
        checkForPartyPlugin();
    }

    /**
     * Get all players within a certain radius of a location.
     *
     * @param location The center location
     * @param radius   The search radius
     * @return List of nearby players
     */
    public List<Player> getNearbyPlayers(Location location, double radius) {
        String cacheKey = getCacheKey(location, radius);
        CachedPlayerList cached = nearbyPlayersCache.get(cacheKey);

        if (cached != null && !cached.isExpired()) {
            return new ArrayList<>(cached.players);
        }

        // Scan for nearby players
        List<Player> nearbyPlayers = location.getWorld().getPlayers().stream()
            .filter(p -> p.getLocation().distance(location) <= radius)
            .collect(Collectors.toList());

        // Cache the result
        nearbyPlayersCache.put(cacheKey, new CachedPlayerList(nearbyPlayers));

        return nearbyPlayers;
    }

    /**
     * Get nearby players using default scan radius.
     *
     * @param location The center location
     * @return List of nearby players
     */
    public List<Player> getNearbyPlayers(Location location) {
        return getNearbyPlayers(location, defaultScanRadius);
    }

    /**
     * Get the number of party members a player has in the nearby player list.
     *
     * @param player         The player to check
     * @param nearbyPlayers The list of nearby players to check against
     * @return Number of party members (not including the player themselves)
     */
    public int getPartyMemberCount(Player player, List<Player> nearbyPlayers) {
        Set<Player> partyMembers = getPartyMembers(player);
        if (partyMembers.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (Player nearby : nearbyPlayers) {
            if (!nearby.equals(player) && partyMembers.contains(nearby)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Check if a player is in a party.
     *
     * @param player The player to check
     * @return true if player is in a party with others
     */
    public boolean isInParty(Player player) {
        return !getPartyMembers(player).isEmpty();
    }

    /**
     * Get all party members for a player (not including the player themselves).
     *
     * @param player The player to check
     * @return Set of party members, or empty set if not in a party
     */
    public Set<Player> getPartyMembers(Player player) {
        UUID uuid = player.getUniqueId();
        CachedPartyMembers cached = partyCache.get(uuid);

        if (cached != null && !cached.isExpired()) {
            return new HashSet<>(cached.members);
        }

        // Calculate party members
        Set<Player> members = new HashSet<>();

        // Try party plugin first
        if (hasPartyPlugin) {
            members = getPartyMembersFromPlugin(player);
        }

        // Fallback to scoreboard teams
        if (members.isEmpty()) {
            members = getPartyMembersFromScoreboard(player);
        }

        // Cache the result
        partyCache.put(uuid, new CachedPartyMembers(members));

        return members;
    }

    /**
     * Get non-party players in the nearby list.
     *
     * @param player         The player to check
     * @param nearbyPlayers The list of nearby players
     * @return Count of nearby players who are NOT in the player's party
     */
    public int getNonPartyPlayerCount(Player player, List<Player> nearbyPlayers) {
        Set<Player> partyMembers = getPartyMembers(player);
        int count = 0;

        for (Player nearby : nearbyPlayers) {
            if (!nearby.equals(player) && !partyMembers.contains(nearby)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Clear all caches. Useful for testing or manual refresh.
     */
    public void clearCache() {
        nearbyPlayersCache.clear();
        partyCache.clear();
    }

    /**
     * Clear expired cache entries. Called periodically.
     */
    public void cleanupCache() {
        nearbyPlayersCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        partyCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private void checkForPartyPlugin() {
        // Check for common party plugins
        // TODO: Add integration with popular party plugins if needed
        // Examples: PartyAndFriends, DungeonsXL, etc.
        hasPartyPlugin = false;
    }

    private Set<Player> getPartyMembersFromPlugin(Player player) {
        // TODO: Implement party plugin integration if needed
        return new HashSet<>();
    }

    private Set<Player> getPartyMembersFromScoreboard(Player player) {
        Set<Player> members = new HashSet<>();
        Team team = player.getScoreboard().getPlayerTeam(player);

        if (team == null) {
            return members;
        }

        // Get all online players on the same team
        for (String entry : team.getEntries()) {
            Player member = plugin.getServer().getPlayer(entry);
            if (member != null && member.isOnline() && !member.equals(player)) {
                members.add(member);
            }
        }

        return members;
    }

    private String getCacheKey(Location location, double radius) {
        return String.format("%s_%d_%d_%d_%.0f",
            location.getWorld().getName(),
            location.getBlockX() / 16, // Chunk X
            location.getBlockY() / 16, // Chunk Y
            location.getBlockZ() / 16, // Chunk Z
            radius);
    }

    // Cache data structures
    private class CachedPlayerList {
        final List<Player> players;
        final long timestamp;

        CachedPlayerList(List<Player> players) {
            this.players = players;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > cacheExpiryMs;
        }
    }

    private class CachedPartyMembers {
        final Set<Player> members;
        final long timestamp;

        CachedPartyMembers(Set<Player> members) {
            this.members = members;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > cacheExpiryMs;
        }
    }
}
