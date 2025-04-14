package ch.framedev.customScoreboard;



/*
 * ch.framedev.customScoreboard
 * =============================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * =============================================
 * This Class was created at 14.04.2025 16:07
 */

import java.util.Map;

public enum Regex {
    PLAYER("%player%"), WORLD("%world%"), HEALTH("%health%"), FOOD("%food%"),
    TIME("%time%"), DATE("%date%"), IP("%ip%"), VERSION("%version%"),
    ONLINE("%online%"), MAX_PLAYERS("%max_players%"), SERVER_NAME("%server_name%"),
    MONEY("%money%"), PING("%ping%"), COORDINATES("%coordinates%"), LEVEL("%level%");

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
        if (text == null || replacements == null) {
            return text;
        }
        for (Regex regex : Regex.values()) {
            if (text.contains(regex.regex) && replacements.containsKey(regex)) {
                text = text.replace(regex.regex, replacements.get(regex).toString());
            }
        }
        return text;
    }
}
