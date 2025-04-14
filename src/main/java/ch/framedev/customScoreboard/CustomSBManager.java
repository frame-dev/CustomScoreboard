package ch.framedev.customScoreboard;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class CustomSBManager implements Listener {

    private final ScoreboardFileManager fileManager;
    private final CustomScoreboard plugin;
    private Objective objective;

    private int onlinePlayers = 0;

    private BukkitTask task;
    private BukkitTask joinTask;

    public CustomSBManager(CustomScoreboard plugin, ScoreboardFileManager fileManager) {
        this.plugin = plugin;
        this.fileManager = fileManager;
    }

    public BukkitTask getTask() {
        return task;
    }

    public void setTask(BukkitTask task) {
        this.task = task;
    }

    public void createScoreboard(String name, Scoreboard scoreboard) {
        if (name == null || name.isEmpty()) {
            plugin.getLogger().warning("Cannot create scoreboard with null or empty name");
            return;
        }
        
        String displayName = plugin.getConfig().getString("scoreboard.displayName", "&6Scoreboard");
        displayName = displayName.replace("&", "§");
        if (scoreboard == null) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        }
        try {
            if (scoreboard.getObjective(name) == null || objective == null && scoreboard.getObjectives().isEmpty()) {
                objective = scoreboard.registerNewObjective(name, Criteria.DUMMY, displayName);
            } else {
                objective = scoreboard.getObjective(name);
            }
            
            if (objective == null) {
                plugin.getLogger().severe("Failed to create or get objective for scoreboard: " + name);
                return;
            }
            
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            objective.setDisplayName(displayName);

            Map<String, Object> objectives = new CustomSerializer(name, scoreboard, objective).serialize();
            fileManager.setScoreboard(name, objectives);
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                Set<String> usedEntries = new HashSet<>();
                Map<Regex, Object> replacements = loadCustomRegexReplacements(player);
                
                // Clear previous entries before adding new ones
                for (String entry : scoreboard.getEntries()) {
                    scoreboard.resetScores(entry);
                }
                
                if (plugin.getConfig().getConfigurationSection("scoreboard") == null) {
                    plugin.getLogger().warning("No scoreboard configuration found in config.yml");
                    continue;
                }
                
                for (String keys : plugin.getConfig().getConfigurationSection("scoreboard").getKeys(false)) {
                    try {
                        String scoreName = plugin.getConfig().getString("scoreboard." + keys + ".name");
                        String value = plugin.getConfig().getString("scoreboard." + keys + ".value");
                        
                        if (scoreName != null && value != null) {
                            scoreName = scoreName.replace("&", "§");
                            value = Regex.replaceRegex(value, replacements);
                            value = value.replace("&", "§");
                            String fullEntry = value != null ? scoreName + ": " + value : scoreName + ": Not Set";

                            // Ensure uniqueness by appending spaces if necessary
                            while (usedEntries.contains(fullEntry)) {
                                fullEntry += " ";
                            }
                            usedEntries.add(fullEntry);
                            
                            try {
                                objective.getScore(fullEntry).setScore(Integer.parseInt(keys));
                            } catch (NumberFormatException e) {
                                plugin.getLogger().warning("Invalid score value: " + keys + " for entry: " + fullEntry);
                            }
                        }
                    } catch (Exception ignored) {}
                }
                
                try {
                    player.setScoreboard(scoreboard);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to set scoreboard for player: " + player.getName(), e);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating scoreboard: " + name, e);
        }
    }

    public void setScoreboard() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            createScoreboard("custom_scoreboard", player.getScoreboard());
        }
    }

    private Map<Regex, Object> loadCustomRegexReplacements(Player player) {
        Map<Regex, Object> replacements = new HashMap<>();
        
        if (player == null) {
            plugin.getLogger().warning("Cannot load regex replacements for null player");
            return replacements;
        }
        
        for (Regex regex : Regex.values()) {
            try {
                if (regex == Regex.PLAYER)
                    replacements.put(regex, player.getName());
                else if (regex == Regex.WORLD)
                    replacements.put(regex, player.getWorld().getName());
                else if (regex == Regex.TIME) {
                    String timeFormat = plugin.getConfig().getString("formats.time", "HH:mm:ss");
                    SimpleDateFormat timeFormatter = new SimpleDateFormat(timeFormat);
                    Date time = new Date();
                    replacements.put(regex, timeFormatter.format(time));
                } else if (regex == Regex.PLAYER_WORLD_TIME) {
                    // Get player's world time
                    long worldTime = player.getWorld().getTime();
                    // Convert ticks to hours, minutes, seconds
                    int hours = (int) ((worldTime / 1000 + 6) % 24);
                    int minutes = (int) ((worldTime % 1000) / 1000 * 60);
                    
                    // Format according to config or default
                    String formattedTime = String.format("%02d:%02d", hours, minutes);
                    replacements.put(regex, formattedTime);
                } else if (regex == Regex.DATE) {
                    String dateFormat = plugin.getConfig().getString("formats.date", "yyyy-MM-dd");
                    SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat);
                    Date date = new Date();
                    replacements.put(regex, dateFormatter.format(date));
                } else if (regex == Regex.IP) {
                    if (player.getAddress() == null) {
                        replacements.put(regex, "Unknown");
                        continue;
                    }
                    InetSocketAddress inetAddress = player.getAddress();
                    if (inetAddress != null) {
                        replacements.put(regex, inetAddress.getHostName());
                    } else {
                        replacements.put(regex, "Unknown");
                    }
                } else if (regex == Regex.VERSION)
                    replacements.put(regex, Bukkit.getVersion());
                else if (regex == Regex.PLAYER_VERSION) {
                    // Use a simpler approach for player version
                    String playerVersion = "Unknown";
                    
                    // Try to get server version to determine which approach to use
                    String serverVersion = Bukkit.getVersion();
                    
                    if (serverVersion.contains("1.20") || serverVersion.contains("1.21")) {
                        playerVersion = "1.20+";
                    } else if (serverVersion.contains("1.19")) {
                        playerVersion = "1.19.x";
                    } else if (serverVersion.contains("1.18")) {
                        playerVersion = "1.18.x";
                    } else if (serverVersion.contains("1.17")) {
                        playerVersion = "1.17.x";
                    } else if (serverVersion.contains("1.16")) {
                        playerVersion = "1.16.x";
                    } else if (serverVersion.contains("1.15")) {
                        playerVersion = "1.15.x";
                    } else if (serverVersion.contains("1.14")) {
                        playerVersion = "1.14.x";
                    } else if (serverVersion.contains("1.13")) {
                        playerVersion = "1.13.x";
                    } else if (serverVersion.contains("1.12")) {
                        playerVersion = "1.12.x";
                    } else if (serverVersion.contains("1.11")) {
                        playerVersion = "1.11.x";
                    } else if (serverVersion.contains("1.10")) {
                        playerVersion = "1.10.x";
                    } else if (serverVersion.contains("1.9")) {
                        playerVersion = "1.9.x";
                    } else if (serverVersion.contains("1.8")) {
                        playerVersion = "1.8.x";
                    } else {
                        playerVersion = "Unknown";
                    }
                    
                    replacements.put(regex, playerVersion);
                } else if (regex == Regex.ONLINE) {
                    if (onlinePlayers != Bukkit.getOnlinePlayers().size()) {
                        onlinePlayers = Bukkit.getOnlinePlayers().size();
                    }
                    replacements.put(regex, onlinePlayers);
                } else if (regex == Regex.MAX_PLAYERS)
                    replacements.put(regex, Bukkit.getMaxPlayers());
                else if (regex == Regex.MONEY) {
                    if (plugin.getVaultManager() != null) {
                        try {
                            replacements.put(regex, plugin.getVaultManager().getEconomy().format(plugin.getVaultManager().getEconomy().getBalance(player)));
                        } catch (Exception e) {
                            plugin.getLogger().log(Level.WARNING, "Failed to get player balance from Vault", e);
                            replacements.put(regex, "Vault error");
                        }
                    } else {
                        replacements.put(regex, "Vault not found");
                    }
                } else if (regex == Regex.PING)
                    replacements.put(regex, player.getPing() + "ms");
                else if (regex == Regex.COORDINATES)
                    replacements.put(regex, "X" + player.getLocation().getBlockX() + " Y" + player.getLocation().getBlockY() + " Z" + player.getLocation().getBlockZ());
                else if (regex == Regex.HEALTH)
                    replacements.put(regex, player.getHealth());
                else if (regex == Regex.FOOD)
                    replacements.put(regex, player.getFoodLevel());
                else if (regex == Regex.LEVEL)
                    replacements.put(regex, player.getLevel());
                else if (regex == Regex.TPS) {
                    replacements.put(regex, String.format("%.2f", Lag.getTPS()));
                } else if (regex == Regex.EXP)
                    replacements.put(regex, String.format("%.2f", player.getExp()));
                else if (regex == Regex.TIME_AS_TICKS)
                    replacements.put(regex, player.getWorld().getTime());
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error processing regex replacement: " + regex, e);
                replacements.put(regex, "Error");
            }
        }
        return replacements;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (player == null) {
            plugin.getLogger().warning("Received PlayerJoinEvent with null player");
            return;
        }
        
        // Cancel existing task if it's still running
        if (joinTask != null && !joinTask.isCancelled()) {
            joinTask.cancel();
        }
        
        joinTask = Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    createScoreboard("custom_scoreboard", player.getScoreboard());
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error creating scoreboard for player: " + player.getName(), e);
                }
            }
        }, 120);
    }
}