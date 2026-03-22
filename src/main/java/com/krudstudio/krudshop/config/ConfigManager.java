package com.krudstudio.krudshop.config;

import com.krudstudio.krudshop.KRUDShop;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ConfigManager {

    private final KRUDShop plugin;
    private FileConfiguration config;
    private final Map<String, FileConfiguration> categories = new HashMap<>();

    public ConfigManager(KRUDShop plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        loadCategories();
    }

    private void loadCategories() {
        categories.clear();
        File categoryDir = new File(plugin.getDataFolder(), "categories");
        if (!categoryDir.exists()) {
            categoryDir.mkdirs();
            
            // Extract default categories from jar
            String[] defaults = {"end-shop.yml", "food-shop.yml", "gear-shop.yml", "nether-shop.yml", "shard-shop.yml"};
            for (String def : defaults) {
                File file = new File(categoryDir, def);
                if (!file.exists()) {
                    try (InputStream in = plugin.getResource("categories/" + def)) {
                        if (in != null) {
                            Files.copy(in, file.toPath());
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "Could not copy default category: " + def, e);
                    }
                }
            }
        }

        File[] files = categoryDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                categories.put(name, YamlConfiguration.loadConfiguration(file));
            }
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public Map<String, FileConfiguration> getCategories() {
        return categories;
    }
    
    public FileConfiguration getCategory(String fileName) {
        return categories.get(fileName);
    }
}
