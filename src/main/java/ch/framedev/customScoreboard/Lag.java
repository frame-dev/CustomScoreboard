/* I couldn't find a vanilla method so this is there until we do.
The script is by LazyLemons on the Bukkit forums.
https://bukkit.org/threads/get-server-tps.143410/ */

package ch.framedev.customScoreboard;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Efficient implementation of TPS (Ticks Per Second) calculation
 * Uses atomic operations for thread safety and optimized algorithms
 * Updated by FrameDev for the CustomScoreboard plugin
 */
public class Lag implements Runnable {
    // Size of the circular buffer for tick times
    private static final int BUFFER_SIZE = 600;
    
    // Atomic counter for thread safety
    private static final AtomicInteger TICK_COUNT = new AtomicInteger(0);
    
    // Atomic array for thread-safe access to tick times
    private static final AtomicLongArray TICKS = new AtomicLongArray(BUFFER_SIZE);
    
    // Last tick time for performance optimization
    private static final AtomicLong LAST_TICK = new AtomicLong(0L);
    
    // Cache for TPS values to reduce redundant calculations
    private static volatile double lastTPS = 20.0D;
    private static volatile long lastTPSCalculation = 0L;
    private static final long TPS_CACHE_DURATION = 1000L; // 1 second cache

    /**
     * Get the current TPS (Ticks Per Second)
     * 
     * @return The current TPS
     */
    public static double getTPS() {
        return getTPS(100);
    }

    /**
     * Get the TPS over a specific number of ticks
     * 
     * @param ticks The number of ticks to calculate TPS over
     * @return The TPS over the specified number of ticks
     */
    public static double getTPS(int ticks) {
        // Check if we have a cached value that's still valid
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTPSCalculation < TPS_CACHE_DURATION) {
            return lastTPS;
        }
        
        // Calculate new TPS value
        int currentTickCount = TICK_COUNT.get();
        if (currentTickCount < ticks) {
            lastTPS = 20.0D;
            lastTPSCalculation = currentTime;
            return lastTPS;
        }
        
        // Calculate TPS using the circular buffer
        int target = (currentTickCount - 1 - ticks) % BUFFER_SIZE;
        long elapsed = currentTime - TICKS.get(target);
        
        // Avoid division by zero
        if (elapsed <= 0) {
            lastTPS = 20.0D;
            lastTPSCalculation = currentTime;
            return lastTPS;
        }
        
        // Calculate TPS
        lastTPS = ticks / (elapsed / 1000.0D);
        lastTPSCalculation = currentTime;
        
        // Cap TPS at 20.0 to avoid unrealistic values
        if (lastTPS > 20.0D) {
            lastTPS = 20.0D;
        }
        
        return lastTPS;
    }

    /**
     * Get the elapsed time since a specific tick
     * 
     * @param tickID The tick ID to calculate elapsed time from
     * @return The elapsed time in milliseconds
     */
    public static long getElapsed(int tickID) {
        int currentTickCount = TICK_COUNT.get();
        if (currentTickCount - tickID >= BUFFER_SIZE) {
            return 0L; // Tick is too old, data not available
        }
        
        long time = TICKS.get(tickID % BUFFER_SIZE);
        return System.currentTimeMillis() - time;
    }

    /**
     * Run method called by the scheduler
     * Records the current time for TPS calculation
     */
    @Override
    public void run() {
        long currentTime = System.currentTimeMillis();
        int currentTickCount = TICK_COUNT.getAndIncrement();
        
        // Store the current time in the circular buffer
        TICKS.set(currentTickCount % BUFFER_SIZE, currentTime);
        
        // Update the last tick time
        LAST_TICK.set(currentTime);
    }
    
    /**
     * Reset the TPS calculation
     * Useful when the server has been running for a long time
     */
    public static void reset() {
        TICK_COUNT.set(0);
        LAST_TICK.set(System.currentTimeMillis());
        lastTPS = 20.0D;
        lastTPSCalculation = System.currentTimeMillis();
    }
}