package com.krudstudio.krudshop.economy;

import com.krudstudio.krudshop.KRUDShop;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    private final KRUDShop plugin;
    private Economy econ = null;

    public EconomyManager(KRUDShop plugin) {
        this.plugin = plugin;
    }

    public boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public boolean hasMoney(OfflinePlayer player, double amount) {
        return econ.has(player, amount);
    }

    public void withdraw(OfflinePlayer player, double amount) {
        econ.withdrawPlayer(player, amount);
    }

    public void deposit(OfflinePlayer player, double amount) {
        econ.depositPlayer(player, amount);
    }

    public double getBalance(OfflinePlayer player) {
        return econ.getBalance(player);
    }

    public String format(double amount) {
        return econ.format(amount);
    }
}
