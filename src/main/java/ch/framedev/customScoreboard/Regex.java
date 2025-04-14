package ch.framedev.customScoreboard;

import java.util.Map;

/*
 * ch.framedev.customScoreboard
 * =============================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * =============================================
 * This Class was created at 14.04.2025 16:07
 */

public enum Regex {
    PLAYER("%player%"), WORLD("%world%"), HEALTH("%health%"), FOOD("%food%"),
    TIME("%time%"), DATE("%date%"), IP("%ip%"), VERSION("%version%"),
    ONLINE("%online%"), MAX_PLAYERS("%max_players%"),
    MONEY("%money%"), PING("%ping%"), COORDINATES("%coordinates%"), LEVEL("%level%"),
    TPS("%tps%"), EXP("%exp%"), TIME_AS_TICKS("%time_as_ticks%"),
    PLAYER_VERSION("%player_version%");

    private final String regex;

    Regex(String regex) {
        this.regex = regex;
    }

    public String getRegex() {
        return regex;
    }

    public boolean isRegex(String text) {
        for (Regex regex : Regex.values()) {
            if (text.contains(regex.regex)) {
                return true;
            }
        }
        return false;
    }

    public static String replaceRegex(String text, Map<Regex, Object> replacements) {
        if (text == null) return null;
        for (Map.Entry<Regex, Object> entry : replacements.entrySet()) {
            if (entry.getValue() != null) {
                text = text.replaceAll(entry.getKey().getRegex(), entry.getValue().toString());
            }
        }
        return text;
    }
}
