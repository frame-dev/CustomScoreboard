package ch.framedev.customScoreboard;

import org.bukkit.Bukkit;
import org.bukkit.WeatherType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
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

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.WeakHashMap;
import java.util.Collections;
import org.bukkit.scheduler.BukkitRunnable;

public class CustomSBManager implements Listener {

    private final ScoreboardFileManager fileManager;
    private final CustomScoreboard plugin;
    private Objective objective;

    private File file;
    private FileConfiguration config;

    private int onlinePlayers = 0;
    private Map<Player, Double> balanceCache = Collections.synchronizedMap(new WeakHashMap<>());

    private BukkitTask task;
    private BukkitTask joinTask;

    public CustomSBManager(CustomScoreboard plugin, ScoreboardFileManager fileManager) {
        this.plugin = plugin;
        this.fileManager = fileManager;
        this.file = new File(plugin.getDataFolder(), "scoreboard.yml");
        if (!file.exists()) {
            try {
                Files.copy(plugin.getResource("scoreboard.yml"), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                config = YamlConfiguration.loadConfiguration(file);
                config.save(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public File getFile() {
        return file;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * Get the task
     * @return BukkitTask
     */
    public BukkitTask getTask() {
        return task;
    }

    /**
     * Set the task
     * @param task BukkitTask
     */
    public void setTask(BukkitTask task) {
        this.task = task;
    }

    /**
     * Create the scoreboard for a player
     * @param name Name of the scoreboard
     * @param scoreboard Scoreboard
     */
    public void createScoreboard(String name, Scoreboard scoreboard) {
        if (name == null || name.isEmpty()) {
            plugin.getLogger().warning("Cannot create scoreboard with null or empty name");
            return;
        }

        String displayName = plugin.getConfig().getString("scoreboard-settings.displayName", "&6Scoreboard");
        displayName = displayName.replace("&", "§");

        // Check if the scoreboard is null and create a new one if it is
        if (scoreboard == null) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        }
        try {
            // Check if the objective is null and create a new one if it is
            if (scoreboard.getObjective(name) == null || objective == null && scoreboard.getObjectives().isEmpty()) {
                objective = scoreboard.registerNewObjective(name, Criteria.DUMMY, displayName);
            } else {
                objective = scoreboard.getObjective(name);
                for (String entry : scoreboard.getEntries()) {
                    scoreboard.resetScores(entry);
                }
            }

            // Check if the objective is null and log an error if it is
            if (objective == null) {
                plugin.getLogger().severe("Failed to create or get objective for scoreboard: " + name);
                return;
            }

            // Set the display slot and display name of the objective
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            objective.setDisplayName(displayName);

            // Serialize the scoreboard
            Map<String, Object> objectives = new CustomSerializer(name, scoreboard, objective).serialize();
            // Save the scoreboard to the file manager
            fileManager.setScoreboard(name, objectives);

            // Loop through all online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Create a new set of used entries
                Set<String> usedEntries = new HashSet<>();
                // Load the custom regex replacements
                Map<Regex, Object> replacements = loadCustomRegexReplacements(player);

                // Clear previous entries before adding new ones
                for (String entry : scoreboard.getEntries()) {
                    scoreboard.resetScores(entry);
                }

                // Check if the scoreboard configuration is null and log a warning if it is
                if (config.getConfigurationSection("scoreboard") == null) {
                    plugin.getLogger().warning("No scoreboard configuration found in config.yml");
                    continue;
                }

                // Get all scoreboard entries
                Set<String> scoreboardEntries = config.getConfigurationSection("scoreboard").getKeys(false);

                // Loop through all scoreboard entries
                for (int i = 0; i < scoreboardEntries.size(); i++) {
                    try {
                        // Get the score name and value
                        String scoreName = config
                                .getString("scoreboard." + scoreboardEntries.toArray()[i] + ".name");
                        String value = config
                                .getString("scoreboard." + scoreboardEntries.toArray()[i] + ".value");
                        
                        // Check if this is a placeholder entry
                        if (scoreName != null && scoreName.equalsIgnoreCase("placeholder")) {
                            // Get the placeholder style from config or use default
                            String placeholderStyle = value != null && !value.isEmpty() ? 
                                    value : "--------------------------------";
                            
                            // Create the placeholder entry - use ONLY the placeholder style
                            String fullEntry = placeholderStyle;
                            
                            // Ensure uniqueness by appending spaces if necessary
                            while (usedEntries.contains(fullEntry)) {
                                fullEntry += " ";
                            }
                            
                            // Add full entry to used entries
                            usedEntries.add(fullEntry);
                            
                            // Set the score
                            try {
                                objective.getScore(fullEntry).setScore(i);
                            } catch (NumberFormatException e) {
                                plugin.getLogger()
                                        .warning("Invalid score value: " + i + " for entry: " + fullEntry);
                            }
                        } else {
                            // Regular scoreboard entry processing
                            // Check if the score name and value are not null
                            if (scoreName != null && value != null) {
                                // Replace color codes
                                scoreName = scoreName.replace("&", "§");
                                // Replace regex
                                value = Regex.replaceRegex(value, replacements);
                                value = value.replace("&", "§");
                                if (plugin.getVaultManager() != null && value.contains("%currency%")) {
                                    String currency = plugin.getConfig().getString("vault.symbole", "$");
                                    value = value.replace("%currency%", currency);
                                }
                                String fullEntry = value != null ? scoreName + "" + value : scoreName + ": Not Set";

                                // Ensure uniqueness by appending spaces if necessary
                                while (usedEntries.contains(fullEntry)) {
                                    fullEntry += " ";
                                }
                                // add full entry to used entries
                                usedEntries.add(fullEntry);

                                // Set the score
                                try {
                                    objective.getScore(fullEntry).setScore(i);
                                } catch (NumberFormatException e) {
                                    plugin.getLogger()
                                            .warning("Invalid score value: " + i + " for entry: " + fullEntry);
                                }
                            }
                        }
                    } catch (Exception ex) {
                        // Log the error
                        plugin.getLogger().log(Level.SEVERE, "Error creating scoreboard: " + name, ex);
                    }
                }

                try {
                    // Set the scoreboard
                    player.setScoreboard(scoreboard);
                } catch (Exception e) {
                    // Log the error
                    plugin.getLogger().log(Level.SEVERE, "Failed to set scoreboard for player: " + player.getName(), e);
                }
            }
        } catch (Exception e) {
            // Log the error
            plugin.getLogger().log(Level.SEVERE, "Error creating scoreboard: " + name, e);
        }
    }

    public void setScoreboard() {
        // Loop through all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Create the scoreboard and set it to the player
            createScoreboard("custom_scoreboard", player.getScoreboard());
        }
    }

    /**
     * Load the custom regex replacements
     * 
     * @param player Player
     * @return Map of regex replacements
     */
    private Map<Regex, Object> loadCustomRegexReplacements(Player player) {
        // Create a new map of regex replacements
        Map<Regex, Object> replacements = new HashMap<>();

        // Check if the player is null
        if (player == null) {
            plugin.getLogger().warning("Cannot load regex replacements for null player");
            return replacements;
        }

        // Loop through all regex values
        for (Regex regex : Regex.values()) {
            try {
                // Check if the regex is the player regex
                if (regex == Regex.PLAYER)
                    replacements.put(regex, player.getName());
                // Check if the regex is the world regex
                else if (regex == Regex.WORLD)
                    replacements.put(regex, player.getWorld().getName());
                // Check if the regex is the time regex
                else if (regex == Regex.TIME) {
                    String timeFormat = plugin.getConfig().getString("formats.time", "HH:mm:ss");
                    SimpleDateFormat timeFormatter = new SimpleDateFormat(timeFormat);
                    Date time = new Date();
                    replacements.put(regex, timeFormatter.format(time));
                    // Check if the regex is the player world time regex
                } else if (regex == Regex.PLAYER_WORLD_TIME) {
                    // Get player's world time
                    long worldTime = player.getWorld().getTime();
                    // Convert ticks to hours, minutes
                    int hours = (int) ((worldTime / 1000 + 6) % 24);
                    int minutes = (int) ((worldTime % 1000) / 1000 * 60);

                    // Format according to config or default
                    String formattedTime = String.format("%02d:%02d", hours, minutes);
                    replacements.put(regex, formattedTime);
                    // Check if the regex is the date regex
                } else if (regex == Regex.DATE) {
                    String dateFormat = plugin.getConfig().getString("formats.date", "yyyy-MM-dd");
                    SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat);
                    Date date = new Date();
                    replacements.put(regex, dateFormatter.format(date));
                    // Check if the regex is the ip regex
                } else if (regex == Regex.IP) {
                    // Check if the player's address is null
                    if (player.getAddress() == null) {
                        replacements.put(regex, "Unknown");
                        continue;
                    }
                    // Get the player's address
                    InetSocketAddress inetAddress = player.getAddress();
                    // Check if the address is not null
                    if (inetAddress != null) {
                        replacements.put(regex, inetAddress.getHostName());
                    } else {
                        replacements.put(regex, "Unknown");
                    }
                    // Check if the regex is the version regex
                } else if (regex == Regex.VERSION)
                    // Get the server version
                    replacements.put(regex, Bukkit.getVersion());
                // Check if the regex is the player version regex
                else if (regex == Regex.PLAYER_VERSION) {
                    // Use PacketEvents to get player version
                    String playerVersion = "Unknown";

                    try {
                        int protocolVersion = 0;
                        PlayerManager playerManager = PacketEvents.getAPI().getPlayerManager();
                        User user = playerManager.getUser(player);
                        protocolVersion = user.getClientVersion().getProtocolVersion();
                        playerVersion = getPlayerVersion(protocolVersion);
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Error getting player version from PacketEvents", e);
                    }

                    replacements.put(regex, playerVersion);
                    // Check if the regex is the online players regex
                } else if (regex == Regex.ONLINE) {
                    // Check if the online players are not the same as the cached online players
                    if (onlinePlayers != Bukkit.getOnlinePlayers().size()) {
                        // Update the cached online players
                        onlinePlayers = Bukkit.getOnlinePlayers().size();
                    }
                    replacements.put(regex, onlinePlayers);
                    // Check if the regex is the max players regex
                } else if (regex == Regex.MAX_PLAYERS) {
                    // Get the max players
                    replacements.put(regex, Bukkit.getMaxPlayers());
                    // Check if the regex is the money regex
                } else if (regex == Regex.MONEY) {
                    // Check if the vault manager is not null
                    if (plugin.getVaultManager() != null) {
                        try {
                            VaultManager vaultManager = plugin.getVaultManager();

                            // Get the cached balance or use a default value
                            double cachedBalance = balanceCache.getOrDefault(player, 0.0);
                            replacements.put(regex, vaultManager.getEconomy().format(cachedBalance));

                            // Update the balance asynchronously
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    try {
                                        double currentBalance = vaultManager.getEconomy().getBalance(player);

                                        // Thread-safe update of the cache using compute
                                        balanceCache.compute(player, (key, oldValue) -> {
                                            if (oldValue == null || !oldValue.equals(currentBalance)) {
                                                return currentBalance;
                                            }
                                            return oldValue;
                                        });
                                    } catch (Exception e) {
                                        plugin.getLogger().log(Level.WARNING,
                                                "Failed to get player balance from Vault asynchronously", e);
                                    }
                                }
                            }.runTaskAsynchronously(plugin);
                        } catch (Exception e) {
                            plugin.getLogger().log(Level.WARNING, "Failed to initialize Vault balance retrieval", e);
                            replacements.put(regex, "Vault error");
                        }
                    } else {
                        replacements.put(regex, "Vault not found");
                    }
                    // Check if the regex is the ping regex
                } else if (regex == Regex.PING)
                    replacements.put(regex, player.getPing() + "ms");
                // Check if the regex is the coordinates regex
                else if (regex == Regex.COORDINATES)
                    replacements.put(regex, "X" + player.getLocation().getBlockX() + " Y"
                            + player.getLocation().getBlockY() + " Z" + player.getLocation().getBlockZ());
                // Check if the regex is the health regex
                else if (regex == Regex.HEALTH)
                    replacements.put(regex, player.getHealth());
                // Check if the regex is the food level regex
                else if (regex == Regex.FOOD)
                    replacements.put(regex, player.getFoodLevel());
                // Check if the regex is the level regex
                else if (regex == Regex.LEVEL)
                    replacements.put(regex, player.getLevel());
                // Check if the regex is the tps regex
                else if (regex == Regex.TPS) {
                    replacements.put(regex, String.format("%.2f", Lag.getTPS()));
                } else if (regex == Regex.EXP)
                    replacements.put(regex, String.format("%.2f", player.getExp()));
                // Check if the regex is the time as ticks regex
                else if (regex == Regex.TIME_AS_TICKS)
                    replacements.put(regex, player.getWorld().getTime());
                // Check if the regex is the weather regex
                else if (regex == Regex.WEATHER) {
                    if (isThundering(player)) {
                        replacements.put(regex, "Thunder");
                    } else {
                        replacements.put(regex, getWeatherType(player) == WeatherType.DOWNFALL ? "Rain" : "Sun");
                    }
                }
            } catch (Exception e) {
                // Log the error
                plugin.getLogger().log(Level.WARNING, "Error processing regex replacement: " + regex, e);
                // Replace the regex with "Error"
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
                    plugin.getLogger().log(Level.SEVERE, "Error creating scoreboard for player: " + player.getName(),
                            e);
                }
            }
        }, 120);
    }

    /**
     * Clear the regex replacement cache to free memory
     * This should be called periodically or when memory usage is high
     */
    public void clearRegexCache() {
        Regex.clearCache();
    }

    /**
     * Get the player version based on protocol version
     * 
     * @param protocolVersion The protocol version
     * @return The player version
     */
    private String getPlayerVersion(int protocolVersion) {
        String playerVersion = "Unknown";
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
        return playerVersion;
    }

    public WeatherType getWeatherType(Player player) {
        if (player.getWorld().hasStorm() || player.getWorld().isThundering()) {
            return WeatherType.DOWNFALL;
        } else {
            return WeatherType.CLEAR;
        }
    }

    public boolean isThundering(Player player) {
        return player.getWorld().isThundering();
    }
}