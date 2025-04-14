package ch.framedev.customScoreboard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
import org.jetbrains.annotations.NotNull;

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
        
        Component component = PlainTextComponentSerializer.plainText().deserialize(displayName);
        try {
            if (scoreboard.getObjective(name) == null || objective == null && scoreboard.getObjectives().isEmpty()) {
                objective = scoreboard.registerNewObjective(name, Criteria.DUMMY, component);
            } else {
                objective = scoreboard.getObjective(name);
            }
            
            if (objective == null) {
                plugin.getLogger().severe("Failed to create or get objective for scoreboard: " + name);
                return;
            }
            
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            objective.displayName(component);

            @NotNull Map<String, Object> objectives = new CustomSerializer(name, scoreboard, objective).serialize();
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
                    // Get player version using a more accurate method
                    String playerVersion = "Unknown";
                    try {
                        // Try to get version using reflection
                        Object handle = player.getClass().getMethod("getHandle").invoke(player);
                        Object connection = handle.getClass().getField("b").get(handle);
                        Object networkManager = connection.getClass().getField("h").get(connection);
                        Object version = networkManager.getClass().getMethod("getVersion").invoke(networkManager);
                        if (version != null) {
                            playerVersion = version.toString();
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.FINE, "Failed to get player version via reflection, falling back to protocol version", e);
                        // If reflection fails, use a more accurate protocol version mapping
                        int protocolVersion = player.getProtocolVersion();
                        playerVersion = getVersionFromProtocol(protocolVersion);
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
                else if (regex == Regex.TPS)
                    replacements.put(regex, String.format("%.2f", Bukkit.getTPS()[0]));
                else if (regex == Regex.EXP)
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
    
    /**
     * Convert protocol version to Minecraft version
     * @param protocolVersion The protocol version
     * @return The Minecraft version string
     */
    private String getVersionFromProtocol(int protocolVersion) {
        // Protocol version to Minecraft version mapping
        // This mapping needs to be updated with each new Minecraft version
        if (protocolVersion >= 766) return "1.21.4";
        if (protocolVersion >= 765) return "1.21.3";
        if (protocolVersion >= 764) return "1.21.2";
        if (protocolVersion >= 763) return "1.21.1";
        if (protocolVersion >= 762) return "1.21";
        if (protocolVersion >= 761) return "1.20.6";
        if (protocolVersion >= 760) return "1.20.5";
        if (protocolVersion >= 759) return "1.20.4";
        if (protocolVersion >= 758) return "1.20.3";
        if (protocolVersion >= 757) return "1.20.2";
        if (protocolVersion >= 756) return "1.20.1";
        if (protocolVersion >= 755) return "1.20";
        if (protocolVersion >= 754) return "1.19.4";
        if (protocolVersion >= 753) return "1.19.3";
        if (protocolVersion >= 752) return "1.19.2";
        if (protocolVersion >= 751) return "1.19.1";
        if (protocolVersion >= 750) return "1.19";
        if (protocolVersion >= 749) return "1.18.2";
        if (protocolVersion >= 748) return "1.18.1";
        if (protocolVersion >= 747) return "1.18";
        if (protocolVersion >= 746) return "1.17.1";
        if (protocolVersion >= 745) return "1.17";
        if (protocolVersion >= 736) return "1.16.5";
        if (protocolVersion >= 735) return "1.16.4";
        if (protocolVersion >= 734) return "1.16.3";
        if (protocolVersion >= 733) return "1.16.2";
        if (protocolVersion >= 732) return "1.16.1";
        if (protocolVersion >= 731) return "1.16";
        if (protocolVersion >= 578) return "1.15.2";
        if (protocolVersion >= 577) return "1.15.1";
        if (protocolVersion >= 576) return "1.15";
        if (protocolVersion >= 498) return "1.14.4";
        if (protocolVersion >= 490) return "1.14.3";
        if (protocolVersion >= 485) return "1.14.2";
        if (protocolVersion >= 480) return "1.14.1";
        if (protocolVersion >= 477) return "1.14";
        if (protocolVersion >= 404) return "1.13.2";
        if (protocolVersion >= 401) return "1.13.1";
        if (protocolVersion >= 393) return "1.13";
        if (protocolVersion >= 340) return "1.12.2";
        if (protocolVersion >= 338) return "1.12.1";
        if (protocolVersion >= 335) return "1.12";
        if (protocolVersion >= 316) return "1.11.2";
        if (protocolVersion >= 315) return "1.11.1";
        if (protocolVersion >= 210) return "1.10.2";
        if (protocolVersion >= 110) return "1.9.4";
        if (protocolVersion >= 109) return "1.9.2";
        if (protocolVersion >= 47) return "1.8.9";
        if (protocolVersion >= 5) return "1.7.10";
        if (protocolVersion >= 4) return "1.7.5";
        if (protocolVersion >= 2) return "1.6.4";
        if (protocolVersion >= 1) return "1.6.2";
        return "Unknown";
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