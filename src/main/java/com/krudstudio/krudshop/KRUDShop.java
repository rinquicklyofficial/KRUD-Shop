package com.krudstudio.krudshop;

import com.krudstudio.krudshop.commands.ShopCommand;
import com.krudstudio.krudshop.config.ConfigManager;
import com.krudstudio.krudshop.economy.EconomyManager;
import com.krudstudio.krudshop.gui.InventoryManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

public class KRUDShop extends JavaPlugin {

    private static KRUDShop instance;
    private ConfigManager configManager;
    private EconomyManager economyManager;
    private InventoryManager inventoryManager;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfigs();

        this.economyManager = new EconomyManager(this);
        if (!this.economyManager.setupEconomy()) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.inventoryManager = new InventoryManager(this);
        
        getCommand("shop").setExecutor(new ShopCommand(this));
        getServer().getPluginManager().registerEvents(this.inventoryManager, this);

        getLogger().info("[KRUD Shop] Enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("[KRUD Shop] Disabled successfully!");
    }

    public static KRUDShop getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public static MiniMessage getMiniMessage() {
        return MINI_MESSAGE;
    }
}
