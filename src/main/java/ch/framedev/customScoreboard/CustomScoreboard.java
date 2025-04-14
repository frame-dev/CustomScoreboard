package ch.framedev.customScoreboard;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;

import com.github.retrooper.packetevents.PacketEvents;

import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;

public final class CustomScoreboard extends JavaPlugin {

    private static CustomScoreboard instance;
    private VaultManager vaultManager;
    private CustomSBManager customSBManager;

    @Override
    public void onLoad() {
        if(!getServer().getPluginManager().isPluginEnabled("PacketEvents")) {
            getLogger().severe("PacketEvents is not installed! Please install it to use this plugin!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        //On Bukkit, calling this here is essential, hence the name "load"
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        if (!getServer().getVersion().contains("1.21.4")) {
            getLogger().severe("This plugin is not compatible with this version of Minecraft!");
            getLogger().severe("Please use version 1.21.4 of Minecraft!");
            getLogger().severe("If you want to use this plugin, please use version 1.21.4 of Minecraft!");
            getServer().getPluginManager().disablePlugin(this);
        }
        saveDefaultConfig();

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
                getConfig().getInt("scoreboard.updateInterval", 20) * 20L);

        // Lag Checker
        getServer().getScheduler().runTaskTimerAsynchronously(this, new Lag(), 100L, 1L);

        PacketEvents.getAPI().init();
    }

    @Override
    public void onDisable() {
        if (customSBManager != null && customSBManager.getTask() != null) {
            customSBManager.getTask().cancel();
            customSBManager.setTask(null);
        }
        PacketEvents.getAPI().terminate();
    }

    /**
     * Get the Vault Manager
     * @return Vault Manager
     */
    public VaultManager getVaultManager() {
        return vaultManager;
    }

    /**
     * Get the Custom Scoreboard Manager
     * @return Custom Scoreboard Manager
     */
    public CustomSBManager getCustomSBManager() {
        return customSBManager;
    }

    /**
     * Get the instance of the plugin
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
     * @param name Name of the scoreboard
     */
    public void createScoreboard(String name, Scoreboard scoreboard) {
        if (customSBManager != null) {
            customSBManager.createScoreboard(name, scoreboard);
        }
    }
}
