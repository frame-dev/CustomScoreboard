package ch.framedev.customScoreboard;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomScoreboard extends JavaPlugin {

    private static CustomScoreboard instance;
    private VaultManager vaultManager;

    @Override
    public void onEnable() {
        instance = this;
        // Plugin startup logic

        Bukkit.getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                if(getServer().getPluginManager().getPlugin("Vault") != null)
                    vaultManager = new VaultManager();
            }
        },120L);
        CustomSBManager customSBManager = new CustomSBManager(this, new ScoreboardFileManager());
        customSBManager.createScoreboard("custom_scoreboard");
        getServer().getPluginManager().registerEvents(customSBManager, this);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                customSBManager.setScoreboard();
            }
        },240L, getConfig().getInt("scoreboard.updateInterval", 20) * 20L);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
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
