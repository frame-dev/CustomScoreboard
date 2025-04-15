package ch.framedev.customScoreboard;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigUtils {

    private final CustomScoreboard plugin;
    
        public ConfigUtils(CustomScoreboard plugin) {
            this.plugin = plugin;
    }

    public void createDefaultConfig() {
        FileConfiguration config = plugin.getConfig();
        if(!config.contains("vault.symbole")) {
            config.set("vault.symbole", "$");
            plugin.saveConfig();
        }

        if(!config.contains("scoreboard-settings.updateInterval")) {
            config.set("scoreboard-settings.updateInterval", 20);
            plugin.saveConfig();
        }

        if(!config.contains("scoreboard-settings.displayName")) {
            config.set("scoreboard-settings.displayName", "&6&lScoreboard");
            plugin.saveConfig();
        }
    }
}
