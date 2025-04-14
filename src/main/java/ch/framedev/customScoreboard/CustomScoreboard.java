package ch.framedev.customScoreboard;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;

public final class CustomScoreboard extends JavaPlugin {

    private static CustomScoreboard instance;
    private VaultManager vaultManager;
    private CustomSBManager customSBManager;

    @Override
    public void onEnable() {
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
    }

    @Override
    public void onDisable() {
        if (customSBManager != null && customSBManager.getTask() != null) {
            customSBManager.getTask().cancel();
            customSBManager.setTask(null);
        }
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
