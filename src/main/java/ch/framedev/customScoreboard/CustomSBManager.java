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

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.protocol.player.User;
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
        
        String displayName = plugin.getConfig().getString("scoreboard-settings.displayName", "&6Scoreboard");
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

                for(int i = 0; i < plugin.getConfig().getConfigurationSection("scoreboard").getKeys(false).size(); i++) {
                    try {
                        String scoreName = plugin.getConfig().getString("scoreboard." + i + ".name");
                        String value = plugin.getConfig().getString("scoreboard." + i + ".value");
                        
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
                                objective.getScore(fullEntry).setScore(i);
                            } catch (NumberFormatException e) {
                                plugin.getLogger().warning("Invalid score value: " + i + " for entry: " + fullEntry);
                            }
                        }
                    } catch (Exception ignored) {}
                
                // for (String keys : plugin.getConfig().getConfigurationSection("scoreboard").getKeys(false)) {
                //     try {
                //         String scoreName = plugin.getConfig().getString("scoreboard." + keys + ".name");
                //         String value = plugin.getConfig().getString("scoreboard." + keys + ".value");
                        
                //         if (scoreName != null && value != null) {
                //             scoreName = scoreName.replace("&", "§");
                //             value = Regex.replaceRegex(value, replacements);
                //             value = value.replace("&", "§");
                //             String fullEntry = value != null ? scoreName + ": " + value : scoreName + ": Not Set";

                //             // Ensure uniqueness by appending spaces if necessary
                //             while (usedEntries.contains(fullEntry)) {
                //                 fullEntry += " ";
                //             }
                //             usedEntries.add(fullEntry);
                            
                //             try {
                //                 objective.getScore(fullEntry).setScore(Integer.parseInt(keys));
                //             } catch (NumberFormatException e) {
                //                 plugin.getLogger().warning("Invalid score value: " + keys + " for entry: " + fullEntry);
                //             }
                //         }
                //     } catch (Exception ignored) {}
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
                    // Use PacketEvents to get player version
                    String playerVersion = "Unknown";
                    
                    try {
                        int protocolVersion = 0;
                        PlayerManager playerManager = PacketEvents.getAPI().getPlayerManager();
                        User user = playerManager.getUser(player);
                        protocolVersion = user.getClientVersion().getProtocolVersion();
                        
                        // Map protocol version to Minecraft version
                        if (protocolVersion >= 765) {
                            playerVersion = "1.21.x";
                        } else if (protocolVersion >= 762) {
                            playerVersion = "1.20.4";
                        } else if (protocolVersion >= 761) {
                            playerVersion = "1.20.2";
                        } else if (protocolVersion >= 760) {
                            playerVersion = "1.20.1";
                        } else if (protocolVersion >= 759) {
                            playerVersion = "1.20";
                        } else if (protocolVersion >= 758) {
                            playerVersion = "1.19.4";
                        } else if (protocolVersion >= 757) {
                            playerVersion = "1.19.3";
                        } else if (protocolVersion >= 756) {
                            playerVersion = "1.19.2";
                        } else if (protocolVersion >= 755) {
                            playerVersion = "1.19.1";
                        } else if (protocolVersion >= 754) {
                            playerVersion = "1.19";
                        } else if (protocolVersion >= 753) {
                            playerVersion = "1.18.2";
                        } else if (protocolVersion >= 752) {
                            playerVersion = "1.18.1";
                        } else if (protocolVersion >= 751) {
                            playerVersion = "1.18";
                        } else if (protocolVersion >= 750) {
                            playerVersion = "1.17.1";
                        } else if (protocolVersion >= 749) {
                            playerVersion = "1.17";
                        } else if (protocolVersion >= 748) {
                            playerVersion = "1.16.5";
                        } else if (protocolVersion >= 747) {
                            playerVersion = "1.16.4";
                        } else if (protocolVersion >= 746) {
                            playerVersion = "1.16.3";
                        } else if (protocolVersion >= 745) {
                            playerVersion = "1.16.2";
                        } else if (protocolVersion >= 736) {
                            playerVersion = "1.16.1";
                        } else if (protocolVersion >= 735) {
                            playerVersion = "1.16";
                        } else if (protocolVersion >= 578) {
                            playerVersion = "1.15.2";
                        } else if (protocolVersion >= 575) {
                            playerVersion = "1.15.1";
                        } else if (protocolVersion >= 573) {
                            playerVersion = "1.15";
                        } else if (protocolVersion >= 498) {
                            playerVersion = "1.14.4";
                        } else if (protocolVersion >= 490) {
                            playerVersion = "1.14.3";
                        } else if (protocolVersion >= 485) {
                            playerVersion = "1.14.2";
                        } else if (protocolVersion >= 480) {
                            playerVersion = "1.14.1";
                        } else if (protocolVersion >= 477) {
                            playerVersion = "1.14";
                        } else if (protocolVersion >= 404) {
                            playerVersion = "1.13.2";
                        } else if (protocolVersion >= 401) {
                            playerVersion = "1.13.1";
                        } else if (protocolVersion >= 393) {
                            playerVersion = "1.13";
                        } else if (protocolVersion >= 340) {
                            playerVersion = "1.12.2";
                        } else if (protocolVersion >= 338) {
                            playerVersion = "1.12.1";
                        } else if (protocolVersion >= 335) {
                            playerVersion = "1.12";
                        } else if (protocolVersion >= 316) {
                            playerVersion = "1.11.2";
                        } else if (protocolVersion >= 315) {
                            playerVersion = "1.11.1";
                        } else if (protocolVersion >= 210) {
                            playerVersion = "1.11";
                        } else if (protocolVersion >= 110) {
                            playerVersion = "1.10.2";
                        } else if (protocolVersion >= 109) {
                            playerVersion = "1.10.1";
                        } else if (protocolVersion >= 107) {
                            playerVersion = "1.10";
                        } else if (protocolVersion >= 47) {
                            playerVersion = "1.9.4";
                        } else if (protocolVersion >= 45) {
                            playerVersion = "1.9.2";
                        } else if (protocolVersion >= 44) {
                            playerVersion = "1.9.1";
                        } else if (protocolVersion >= 43) {
                            playerVersion = "1.9";
                        } else if (protocolVersion >= 42) {
                            playerVersion = "1.8.9";
                        } else if (protocolVersion >= 41) {
                            playerVersion = "1.8.8";
                        } else if (protocolVersion >= 40) {
                            playerVersion = "1.8.7";
                        } else if (protocolVersion >= 39) {
                            playerVersion = "1.8.6";
                        } else if (protocolVersion >= 38) {
                            playerVersion = "1.8.5";
                        } else if (protocolVersion >= 37) {
                            playerVersion = "1.8.4";
                        } else if (protocolVersion >= 36) {
                            playerVersion = "1.8.3";
                        } else if (protocolVersion >= 35) {
                            playerVersion = "1.8.2";
                        } else if (protocolVersion >= 34) {
                            playerVersion = "1.8.1";
                        } else if (protocolVersion >= 33) {
                            playerVersion = "1.8";
                        } else if (protocolVersion >= 32) {
                            playerVersion = "1.7.10";
                        } else if (protocolVersion >= 31) {
                            playerVersion = "1.7.9";
                        } else if (protocolVersion >= 30) {
                            playerVersion = "1.7.8";
                        } else if (protocolVersion >= 29) {
                            playerVersion = "1.7.7";
                        } else if (protocolVersion >= 28) {
                            playerVersion = "1.7.6";
                        } else if (protocolVersion >= 27) {
                            playerVersion = "1.7.5";
                        } else if (protocolVersion >= 26) {
                            playerVersion = "1.7.4";
                        } else if (protocolVersion >= 25) {
                            playerVersion = "1.7.3";
                        } else if (protocolVersion >= 24) {
                            playerVersion = "1.7.2";
                        } else if (protocolVersion >= 23) {
                            playerVersion = "1.7.1";
                        } else if (protocolVersion >= 22) {
                            playerVersion = "1.7";
                        } else if (protocolVersion >= 20) {
                            playerVersion = "1.6.4";
                        } else if (protocolVersion >= 19) {
                            playerVersion = "1.6.2";
                        } else {
                            playerVersion = "1.6.1 or older";
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Error getting player version from PacketEvents", e);
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