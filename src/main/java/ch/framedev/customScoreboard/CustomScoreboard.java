package ch.framedev.customScoreboard;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;

import com.github.retrooper.packetevents.PacketEvents;
import java.nio.file.Files;

import ch.framedev.simplejavautils.SimpleJavaUtils;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;

public final class CustomScoreboard extends JavaPlugin {

    // Singleton instance
    private static CustomScoreboard instance;

    // Managers
    private VaultManager vaultManager;
    private CustomSBManager customSBManager;

    @Override
    public void onLoad() {
        if (!getServer().getPluginManager().isPluginEnabled("PacketEvents")) {
            getLogger().severe("PacketEvents is not installed! Please install it to use this plugin!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        // On Bukkit, calling this here is essential, hence the name "load"
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {

        // Load the examples
        loadExamples();        

        // Check if the server is running version 1.21.4
        if (!getServer().getVersion().contains("1.21.4")) {
            getLogger().severe("This plugin is not compatible with this version of Minecraft!");
            getLogger().severe("Please use version 1.21.4 of Minecraft!");
            getLogger().severe("If you want to use this plugin, please use version 1.21.4 of Minecraft!");
            getServer().getPluginManager().disablePlugin(this);
        }

        // Save the default config
        saveDefaultConfig();

        ConfigUtils configUtils = new ConfigUtils(this);
        configUtils.createDefaultConfig();

        // Initialize the Singleton instance
        instance = this;

        // Check for updates
        UpdateManager updateManager = new UpdateManager(this);
        updateManager.checkForUpdates();

        // Vault Manager
        Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
            if (getServer().getPluginManager().getPlugin("Vault") != null)
                vaultManager = new VaultManager();
        }, 120L);

        // Scoreboard Manager
        customSBManager = new CustomSBManager(this, new ScoreboardFileManager());
        getServer().getPluginManager().registerEvents(customSBManager, this);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> customSBManager.setScoreboard(), 240L,
                getConfig().getInt("scoreboard-settings.updateInterval", 20) * 20L);
                
        // Periodically clear regex cache to prevent memory leaks
        // Run every 30 minutes (36000 ticks)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (customSBManager != null) {
                customSBManager.clearRegexCache();
                getLogger().info("Cleared regex replacement cache to free memory");
            }
        }, 36000L, 36000L);

        // Lag Checker - Run every tick to get accurate TPS
        // Use a dedicated instance to avoid creating new objects
        Lag lagChecker = new Lag();
        getServer().getScheduler().runTaskTimerAsynchronously(this, lagChecker, 100L, 1L);
        
        // Periodically reset TPS calculation to prevent overflow
        // Run every 6 hours (432000 ticks)
        getServer().getScheduler().runTaskTimer(this, () -> {
            Lag.reset();
            getLogger().info("Reset TPS calculation to prevent overflow");
        }, 432000L, 432000L);

        // Initialize PacketEvents
        PacketEvents.getAPI().init();

        // Register the customscoreboard command
        getCommand("customscoreboard").setExecutor(this);
    }

    @Override
    public void onDisable() {
        if (customSBManager != null && customSBManager.getTask() != null) {
            customSBManager.getTask().cancel();
            customSBManager.setTask(null);
        }

        // Terminate PacketEvents
        PacketEvents.getAPI().terminate();
    }

    /**
     * Load the examples
     */
    private void loadExamples() {
        // Load the examples
        File examplesFolder = new File(getDataFolder(), "examples");
        if (!examplesFolder.exists()) {
            examplesFolder.mkdirs();
        }
        // Load the example files
        String[] exampleFiles = {"custom-scoreboard-example-1.yml",
    "custom-scoreboard-example-2.yml",
    "custom-scoreboard-example-3.yml"};
        // Load the example files
        for (String exampleFile : exampleFiles) {
            File file = new File(examplesFolder, exampleFile);
            // If the file does not exist, copy the example file from the resources
            if (!file.exists()) {
                File fromResource = new SimpleJavaUtils().getFromResourceFile("examples/" +exampleFile, CustomScoreboard.class);
                try {
                    // Copy the example file from the resources
                    Files.copy(fromResource.toPath(), file.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Get the Vault Manager
     * 
     * @return Vault Manager
     */
    public VaultManager getVaultManager() {
        return vaultManager;
    }

    /**
     * Get the Custom Scoreboard Manager
     * 
     * @return Custom Scoreboard Manager
     */
    public CustomSBManager getCustomSBManager() {
        return customSBManager;
    }

    /**
     * Get the instance of the plugin
     * 
     * @return Instance of the plugin
     */
    public static CustomScoreboard getInstance() {
        if (instance == null) {
            instance = new CustomScoreboard();
        }
        return instance;
    }

    /**
     * Create a scoreboard
     * 
     * @param name Name of the scoreboard
     */
    public void createScoreboard(String name, Scoreboard scoreboard) {
        if (customSBManager != null) {
            customSBManager.createScoreboard(name, scoreboard);
        }
    }

    /**
     * Handle the customscoreboard command
     * 
     * @param sender  Command sender
     * @param command Command
     * @param label   Label
     * @param args    Arguments
     * @return True if the command was handled, false otherwise
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the command is the customscoreboard command
        if (command.getName().equalsIgnoreCase("customscoreboard")) {
            // Check if the sender has the customscoreboard.command permission
            if (!sender.hasPermission("customscoreboard.command")) {
                sender.sendMessage("You do not have permission to use this command!");
                return true;
            }

            // Check if the args are empty and send the help message
            if (args.length == 0) {
                sender.sendMessage("Usage: /customscoreboard help");
                return true;
                // Check if the args are "reload" and reload the config
            } else if (args[0].equalsIgnoreCase("reload")) {
                // Reload the config
                reloadConfig();
                // Reload the custom scoreboard manager
                try {
                    customSBManager.getConfig().load(customSBManager.getFile().getAbsoluteFile());
                } catch (IOException | InvalidConfigurationException e) {
                    e.printStackTrace();
                }
                // Send a message to the sender
                sender.sendMessage("CustomScoreboard reloaded!");
            }
        }
        return super.onCommand(sender, command, label, args);
    }
}
