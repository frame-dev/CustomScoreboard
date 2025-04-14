package ch.framedev.customScoreboard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class CustomSBManager implements Listener {

    private final ScoreboardFileManager fileManager;
    private final CustomScoreboard plugin;
    private Scoreboard scoreboard;

    public CustomSBManager(CustomScoreboard plugin, ScoreboardFileManager fileManager) {
        this.plugin = plugin;
        this.fileManager = fileManager;
    }

    public void createScoreboard(String name) {
        String displayName = plugin.getConfig().getString("scoreboard.displayName", "&6Scoreboard");
        displayName = displayName.replace("&", "§");
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Component component = PlainTextComponentSerializer.plainText().deserialize(displayName);
        Objective objective = scoreboard.registerNewObjective(name, Criteria.DUMMY, component);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.displayName(component);

        @NotNull Map<String, Object> objectives = new CustomSerializer(name, scoreboard, objective).serialize();
        fileManager.setScoreboard(name, objectives);

        for (Player player : Bukkit.getOnlinePlayers()) {
            Map<Regex, Object> replacements = loadCustomRegexReplacements(player);
            for (int i = 0; i <= 15; i++) {
                String entry = plugin.getConfig().getString("scoreboard." + i);
                String scoreName = plugin.getConfig().getString("scoreboard." + i + ".name");
                String value = plugin.getConfig().getString("scoreboard." + i + ".value");
                if (entry != null && scoreName != null && value != null) {
                    scoreName = scoreName.replace("&", "§");
                    value = Regex.replaceRegex(value, replacements);
                    objective.getScore(value != null ? scoreName + ":" + value : scoreName + ":" + "Not Set").setScore(i);
                }
            }
        }
        this.scoreboard = scoreboard;
    }

    public void setScoreboard() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(scoreboard);
        }
    }

    private Map<Regex, Object> loadCustomRegexReplacements(Player player) {
        Map<Regex, Object> replacements = new HashMap<>();
        for (Regex regex : Regex.values()) {
            if (regex == Regex.PLAYER)
                replacements.put(regex, player.getName());
            else if (regex == Regex.WORLD)
                replacements.put(regex, player.getWorld().getName());
            else if (regex == Regex.TIME) {
                String timeFormat = plugin.getConfig().getString("formats.time", "HH:mm:ss");
                java.text.SimpleDateFormat timeFormatter = new java.text.SimpleDateFormat(timeFormat);
                java.util.Date time = new java.util.Date();
                replacements.put(regex, time);
            }
            else if (regex == Regex.DATE) {
                String dateFormat = plugin.getConfig().getString("formats.date", "yyyy-MM-dd");
                java.text.SimpleDateFormat dateFormatter = new java.text.SimpleDateFormat(dateFormat);
                java.util.Date date = new java.util.Date();
                replacements.put(regex, dateFormatter.format(date));
            }
            else if (regex == Regex.IP) {
                if (player.getAddress() == null) continue;
                replacements.put(regex, player.getAddress().getHostName());
            }
            else if (regex == Regex.VERSION)
                replacements.put(regex, Bukkit.getVersion());
            else if (regex == Regex.ONLINE)
                replacements.put(regex, Bukkit.getOnlinePlayers().size());
            else if (regex == Regex.MAX_PLAYERS)
                replacements.put(regex, Bukkit.getMaxPlayers());
            else if (regex == Regex.SERVER_NAME)
                replacements.put(regex, Bukkit.getServer().getName());
            else if (regex == Regex.MONEY)
                if(plugin.getVaultManager() != null) {
                    replacements.put(regex, plugin.getVaultManager().getEconomy().format(plugin.getVaultManager().getEconomy().getBalance(player)));
                } else {
                    replacements.put(regex, "Vault not found");
                }
            else if (regex == Regex.PING)
                replacements.put(regex, player.getPing() + "ms");
            else if(regex == Regex.COORDINATES)
                replacements.put(regex, player.getLocation().getBlockX() + " " + player.getLocation().getBlockY() + " " + player.getLocation().getBlockZ());
            else if(regex == Regex.HEALTH)
                replacements.put(regex, player.getHealth());
            else if(regex == Regex.FOOD)
                replacements.put(regex, player.getFoodLevel());
            else if(regex == Regex.LEVEL)
                replacements.put(regex, player.getLevel());
        }
        return replacements;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                player.setScoreboard(scoreboard);
            }
        }, 120);
    }
}