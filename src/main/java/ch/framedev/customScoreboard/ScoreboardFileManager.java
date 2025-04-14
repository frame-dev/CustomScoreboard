package ch.framedev.customScoreboard;



/*
 * ch.framedev.customScoreboard
 * =============================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * =============================================
 * This Class was created at 14.04.2025 15:32
 */

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;

public class ScoreboardFileManager {

    private final File scoreboardFile = new File(CustomScoreboard.getInstance().getDataFolder(), "scoreboards.yml");
    private final YamlConfiguration scoreboardConfig = YamlConfiguration.loadConfiguration(scoreboardFile);

    public ScoreboardFileManager() {
        if (!scoreboardFile.exists()) {
            try {
                scoreboardFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setScoreboard(String name, @NotNull Map<String, Object> objectives) {
        scoreboardConfig.set(name, objectives);
        try {
            scoreboardConfig.save(scoreboardFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
