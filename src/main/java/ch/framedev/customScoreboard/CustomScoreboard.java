package ch.framedev.customScoreboard;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomScoreboard extends JavaPlugin {

    private static CustomScoreboard instance;
    private VaultManager vaultManager;
    private CustomSBManager customSBManager;

    @Override
    public void onEnable() {
        // Initialize the Singleton instance
        instance = this;

        Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
            if(getServer().getPluginManager().getPlugin("Vault") != null)
                vaultManager = new VaultManager();
        },120L);

        customSBManager = new CustomSBManager(this, new ScoreboardFileManager());
        customSBManager.createScoreboard("custom_scoreboard");
        getServer().getPluginManager().registerEvents(customSBManager, this);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> customSBManager.setScoreboard(),240L, getConfig().getInt("scoreboard.updateInterval", 20) * 20L);
    }

    @Override
    public void onDisable() {
        if(customSBManager != null && customSBManager.getTask() != null) {
            customSBManager.getTask().cancel();
            customSBManager.setTask(null);
        }
    }

    public VaultManager getVaultManager() {
        return vaultManager;
    }

    public static CustomScoreboard getInstance() {
        if(instance == null) {
            instance = new CustomScoreboard();
        }
        return instance;
    }

    public void createScoreboard(String name) {

    }
}
