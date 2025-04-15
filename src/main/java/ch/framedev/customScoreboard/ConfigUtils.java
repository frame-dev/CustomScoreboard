package ch.framedev.customScoreboard;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Utility class for handling configuration files
 * =============================================
 * This Class was created at 15.04.2025 15:32
 * This Class was created by FrameDev
 * Please do not change anything without my consent!
 * =============================================
 */
public class ConfigUtils {

    // Plugin instance
    private final CustomScoreboard plugin;

    /**
     * Constructor for the ConfigUtils class
     * @param plugin Plugin instance
     */
    public ConfigUtils(CustomScoreboard plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates the default config if it doesn't exist
     */
    public void createDefaultConfig() {
        FileConfiguration config = plugin.getConfig();
        if (!config.contains("vault.symbole")) {
            config.set("vault.symbole", "$");
            plugin.saveConfig();
        }

        if (!config.contains("scoreboard-settings.updateInterval")) {
            config.set("scoreboard-settings.updateInterval", 20);
            plugin.saveConfig();
        }

        if (!config.contains("scoreboard-settings.displayName")) {
            config.set("scoreboard-settings.displayName", "&6&lScoreboard");
            plugin.saveConfig();
        }
    }
}
