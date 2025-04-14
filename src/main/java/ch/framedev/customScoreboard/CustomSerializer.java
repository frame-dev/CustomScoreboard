package ch.framedev.customScoreboard;



/*
 * ch.framedev.customScoreboard
 * =============================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * =============================================
 * This Class was created at 14.04.2025 15:39
 */

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class CustomSerializer implements ConfigurationSerializable {

    private final Scoreboard scoreboard;
    private final Objective objective;
    private final String name;

    public CustomSerializer(String name, Scoreboard scoreboard, Objective objective) {
        this.name = name;
        this.scoreboard = scoreboard;
        this.objective = objective;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("displayName", PlainTextComponentSerializer.plainText().serialize(objective.displayName()));
        map.put("criteria", objective.getTrackedCriteria().getName());
        map.put("displaySlot", objective.getDisplaySlot().name());
        return map;
    }
}
