package ch.framedev.customScoreboard;



/*
 * ch.framedev.customScoreboard
 * =============================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * =============================================
 * This Class was created at 14.04.2025 16:36
 */

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

import static org.bukkit.Bukkit.getServer;

public class VaultManager {

    private Economy economy;

    public VaultManager() {
        if(!setupEconomy()) {
            CustomScoreboard.getInstance().getLogger().severe("Failed to setup economy!");
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return true;
    }

    public Economy getEconomy() {
        return economy;
    }
}
