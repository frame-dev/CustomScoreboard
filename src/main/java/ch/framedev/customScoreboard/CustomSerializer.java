package ch.framedev.customScoreboard;



/*
 * ch.framedev.customScoreboard
 * =============================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * =============================================
 * This Class was created at 14.04.2025 15:39
 */

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;

public class CustomSerializer implements ConfigurationSerializable {

    private final Objective objective;
    private final String name;

    public CustomSerializer(String name, Scoreboard scoreboard, Objective objective) {
        this.name = name;
        this.objective = objective;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("displayName",objective.getDisplayName());
        map.put("criteria", objective.getTrackedCriteria().getName());
        map.put("displaySlot", objective.getDisplaySlot().name());
        return map;
    }
}
