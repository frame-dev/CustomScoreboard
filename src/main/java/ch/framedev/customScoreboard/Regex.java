package ch.framedev.customScoreboard;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.util.EnumMap;

/*
 * ch.framedev.customScoreboard
 * =============================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * =============================================
 * This Class was created at 14.04.2025 16:07
 * 
 * Updated to be more efficient and to use less memory
 */

public enum Regex {
    PLAYER("%player%"), WORLD("%world%"), HEALTH("%health%"), FOOD("%food%"),
    TIME("%time%"), DATE("%date%"), IP("%ip%"), VERSION("%version%"),
    ONLINE("%online%"), MAX_PLAYERS("%max_players%"),
    MONEY("%money%"), PING("%ping%"), COORDINATES("%coordinates%"), LEVEL("%level%"),
    TPS("%tps%"), EXP("%exp%"), TIME_AS_TICKS("%time_as_ticks%"), PLAYER_WORLD_TIME("%player_world_time%"),
    PLAYER_VERSION("%player_version%");

    private final String regex;
    private final Pattern pattern;
    
    // Cache for compiled patterns
    private static final Map<Regex, Pattern> PATTERN_CACHE = new EnumMap<>(Regex.class);
    
    // Cache for replacement results to avoid repeated replacements
    private static final Map<String, String> REPLACEMENT_CACHE = new ConcurrentHashMap<>();
    
    // Maximum cache size to prevent memory leaks
    private static final int MAX_CACHE_SIZE = 1000;
    
    // Static initializer to populate the pattern cache
    static {
        for (Regex regex : Regex.values()) {
            PATTERN_CACHE.put(regex, regex.pattern);
        }
    }

    Regex(String regex) {
        this.regex = regex;
        this.pattern = Pattern.compile(regex);
    }

    public String getRegex() {
        return regex;
    }
    
    public Pattern getPattern() {
        return pattern;
    }

    public boolean isRegex(String text) {
        if (text == null) return false;
        
        // Quick check for any placeholder
        if (!text.contains("%")) return false;
        
        // Check if any regex pattern matches
        for (Regex regex : Regex.values()) {
            if (text.contains(regex.regex)) {
                return true;
            }
        }
        return false;
    }

    public static String replaceRegex(String text, Map<Regex, Object> replacements) {
        if (text == null) return null;
        if (replacements == null || replacements.isEmpty()) return text;
        
        // Check if text contains any placeholders
        if (!text.contains("%")) return text;
        
        // Generate a cache key based on the text and replacements
        String cacheKey = generateCacheKey(text, replacements);
        
        // Check if we have a cached result
        String cachedResult = REPLACEMENT_CACHE.get(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        // Process the replacements
        String result = text;
        for (Map.Entry<Regex, Object> entry : replacements.entrySet()) {
            if (entry.getValue() != null) {
                // Use Matcher.quoteReplacement to escape special characters in the replacement
                String replacement = Matcher.quoteReplacement(entry.getValue().toString());
                
                // Use the precompiled pattern for better performance
                Pattern pattern = entry.getKey().getPattern();
                result = pattern.matcher(result).replaceAll(replacement);
            }
        }
        
        // Cache the result if the cache isn't too large
        if (REPLACEMENT_CACHE.size() < MAX_CACHE_SIZE) {
            REPLACEMENT_CACHE.put(cacheKey, result);
        }
        
        return result;
    }
    
    /**
     * Generate a cache key for the given text and replacements
     * @param text The text to replace in
     * @param replacements The replacements to apply
     * @return A cache key string
     */
    private static String generateCacheKey(String text, Map<Regex, Object> replacements) {
        StringBuilder key = new StringBuilder(text);
        for (Map.Entry<Regex, Object> entry : replacements.entrySet()) {
            if (entry.getValue() != null) {
                key.append("|").append(entry.getKey().regex).append("=").append(entry.getValue());
            }
        }
        return key.toString();
    }
    
    /**
     * Clear the replacement cache to free memory
     */
    public static void clearCache() {
        REPLACEMENT_CACHE.clear();
    }
}
