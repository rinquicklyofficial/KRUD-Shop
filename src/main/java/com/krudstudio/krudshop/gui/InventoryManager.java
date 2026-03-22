package com.krudstudio.krudshop.gui;

import com.krudstudio.krudshop.KRUDShop;
import com.krudstudio.krudshop.config.ConfigManager;
import com.krudstudio.krudshop.utils.MessageUtils;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class InventoryManager implements Listener {

    private final KRUDShop plugin;
    private final NamespacedKey ACTION_KEY;
    private final NamespacedKey DATA_KEY;

    public InventoryManager(KRUDShop plugin) {
        this.plugin = plugin;
        this.ACTION_KEY = new NamespacedKey(plugin, "gui_action");
        this.DATA_KEY = new NamespacedKey(plugin, "gui_data");
    }

    public void openMainMenu(Player player) {
        ConfigManager cm = plugin.getConfigManager();
        FileConfiguration config = cm.getConfig();
        String title = config.getString("main-inventory.title", "🛒 KRUD Shop");
        int rows = config.getInt("main-inventory.rows", 3);
        List<String> layout = config.getStringList("main-inventory.layout");

        Inventory inv = Bukkit.createInventory(new ShopHolder("main"), rows * 9, MessageUtils.format(title));

        ItemStack filler = getFiller();
        
        Map<String, ConfigurationSection> categories = new HashMap<>();
        ConfigurationSection catSection = config.getConfigurationSection("categories");
        if (catSection != null) {
            for (String key : catSection.getKeys(false)) {
                categories.put(key, catSection.getConfigurationSection(key));
            }
        }

        Iterator<ConfigurationSection> catIterator = categories.values().iterator();

        for (int r = 0; r < layout.size(); r++) {
            String row = layout.get(r);
            for (int c = 0; c < row.length(); c++) {
                int slot = r * 9 + c;
                char icon = row.charAt(c);
                if (icon == 'X') {
                    inv.setItem(slot, filler);
                } else if (icon == 'O') {
                    if (catIterator.hasNext()) {
                        ConfigurationSection cat = catIterator.next();
                        inv.setItem(slot, createCategoryItem(cat));
                    }
                }
            }
        }

        player.openInventory(inv);
        playSound(player, "open");
    }

    private ItemStack getFiller() {
        ConfigManager cm = plugin.getConfigManager();
        Material mat = Material.valueOf(cm.getConfig().getString("items.filler.material", "GRAY_STAINED_GLASS_PANE"));
        String name = cm.getConfig().getString("items.filler.name", " ");
        return MessageUtils.createItem(mat, name, new ArrayList<>());
    }

    private ItemStack createCategoryItem(ConfigurationSection cat) {
        Material mat = Material.valueOf(cat.getString("icon", "STONE"));
        String name = cat.getString("name", "Category");
        List<String> lore = cat.getStringList("lore");
        
        // Replace {count} in lore
        String fileName = cat.getString("file");
        FileConfiguration catConfig = plugin.getConfigManager().getCategory(fileName);
        int count = 0;
        if (catConfig != null && catConfig.getConfigurationSection("items") != null) {
            count = catConfig.getConfigurationSection("items").getKeys(false).size();
        }
        
        List<String> finalLore = new ArrayList<>();
        for (String line : lore) {
            finalLore.add(line.replace("{count}", String.valueOf(count)));
        }

        ItemStack item = MessageUtils.createItem(mat, name, finalLore);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "open_category");
        meta.getPersistentDataContainer().set(DATA_KEY, PersistentDataType.STRING, cat.getName());
        item.setItemMeta(meta);
        return item;
    }

    public void openCategory(Player player, String categoryId, int page) {
        ConfigManager cm = plugin.getConfigManager();
        ConfigurationSection cat = cm.getConfig().getConfigurationSection("categories." + categoryId);
        if (cat == null) return;

        String fileName = cat.getString("file");
        FileConfiguration catConfig = cm.getCategory(fileName);
        if (catConfig == null) return;

        String title = cat.getString("inventory-name", cat.getString("name"));
        List<String> layout = cat.getStringList("layout");
        int rows = layout.size();

        Inventory inv = Bukkit.createInventory(new ShopHolder("category:" + categoryId + ":" + page), rows * 9, MessageUtils.format(title));

        ItemStack filler = getFiller();
        
        // Prepare items
        List<ConfigurationSection> items = new ArrayList<>();
        ConfigurationSection itemSection = catConfig.getConfigurationSection("items");
        if (itemSection != null) {
            for (String key : itemSection.getKeys(false)) {
                items.add(itemSection.getConfigurationSection(key));
            }
        }

        int itemsPerPage = 0;
        for (String row : layout) {
            for (char c : row.toCharArray()) {
                if (c == 'O') itemsPerPage++;
            }
        }

        int totalPages = (int) Math.ceil((double) items.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;

        int startIndex = page * itemsPerPage;
        int itemIndex = startIndex;

        for (int r = 0; r < layout.size(); r++) {
            String row = layout.get(r);
            for (int c = 0; c < row.length(); c++) {
                int slot = r * 9 + c;
                char icon = row.charAt(c);
                if (icon == 'X') {
                    inv.setItem(slot, filler);
                } else if (icon == 'O') {
                    if (itemIndex < items.size() && itemIndex < startIndex + itemsPerPage) {
                        inv.setItem(slot, createShopItem(items.get(itemIndex), categoryId));
                        itemIndex++;
                    }
                } else if (icon == '<') {
                    if (page > 0) {
                        inv.setItem(slot, createNavigationItem("back-page", categoryId, page - 1));
                    } else {
                        inv.setItem(slot, filler);
                    }
                } else if (icon == '>') {
                    if (page < totalPages - 1) {
                        inv.setItem(slot, createNavigationItem("next-page", categoryId, page + 1));
                    } else {
                        inv.setItem(slot, filler);
                    }
                } else if (icon == 'B') {
                    inv.setItem(slot, createNavigationItem("back-to-main", null, 0));
                }
            }
        }

        player.openInventory(inv);
        playSound(player, "open");
    }

    private ItemStack createShopItem(ConfigurationSection itemSection, String categoryId) {
        Material mat = Material.valueOf(itemSection.getString("material", "STONE"));
        String name = itemSection.getString("display-name", itemSection.getName());
        List<String> lore = itemSection.getStringList("lore");
        int amount = itemSection.getInt("amount", 1);
        double price = itemSection.getDouble("price", 0);

        List<String> finalLore = new ArrayList<>();
        for (String line : lore) {
            finalLore.add(line);
        }
        finalLore.add("<dark_gray>⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯");
        finalLore.add("<green>Left Click → Buy x" + amount);
        finalLore.add("<yellow>Shift+Click → Choose amount");

        ItemStack item = MessageUtils.createItem(mat, name, finalLore);
        item.setAmount(amount);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "buy_item");
        meta.getPersistentDataContainer().set(DATA_KEY, PersistentDataType.STRING, categoryId + ":" + itemSection.getName());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNavigationItem(String type, String categoryId, int targetPage) {
        ConfigManager cm = plugin.getConfigManager();
        Material mat = Material.valueOf(cm.getConfig().getString("items." + type + ".material", "ARROW"));
        String name = cm.getConfig().getString("items." + type + ".name", type);
        
        ItemStack item = MessageUtils.createItem(mat, name, new ArrayList<>());
        ItemMeta meta = item.getItemMeta();
        if (type.equals("back-to-main")) {
            meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "back_to_main");
        } else {
            meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "change_page");
            meta.getPersistentDataContainer().set(DATA_KEY, PersistentDataType.STRING, categoryId + ":" + targetPage);
        }
        item.setItemMeta(meta);
        return item;
    }

    public void openConfirmGui(Player player, String categoryId, String itemId, int amount) {
        ConfigManager cm = plugin.getConfigManager();
        FileConfiguration catConfig = cm.getCategory(cm.getConfig().getString("categories." + categoryId + ".file"));
        ConfigurationSection itemSection = catConfig.getConfigurationSection("items." + itemId);
        if (itemSection == null) return;

        double basePrice = itemSection.getDouble("price");
        int baseAmount = itemSection.getInt("amount");
        double totalPrice = (basePrice / baseAmount) * amount;

        String title = cm.getConfig().getString("gui.confirm-title", "<gradient:#a855f7:#f59e0b>Confirm Purchase");
        Inventory inv = Bukkit.createInventory(new ShopHolder("confirm:" + categoryId + ":" + itemId + ":" + amount), 27, MessageUtils.format(title));

        ItemStack filler = getFiller();
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        // Display Item
        ItemStack displayItem = new ItemStack(Material.valueOf(itemSection.getString("material")));
        ItemMeta displayMeta = displayItem.getItemMeta();
        displayMeta.displayName(MessageUtils.format(itemSection.getString("display-name")));
        displayItem.setItemMeta(displayMeta);
        displayItem.setAmount(amount);
        inv.setItem(4, displayItem);

        // Confirm Button
        Material confirmMat = Material.valueOf(cm.getConfig().getString("items.confirm.material", "LIME_STAINED_GLASS_PANE"));
        String confirmName = cm.getConfig().getString("items.confirm.name", "<green><bold>✔ Confirm Purchase");
        
        double balance = plugin.getEconomyManager().getBalance(player);
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add("<gray>Item: <white>" + itemSection.getString("display-name"));
        confirmLore.add("<gray>Amount: <white>" + amount + "x");
        confirmLore.add("<gray>Price: <green>$" + totalPrice);
        confirmLore.add("<gray>Your balance: <white>$" + balance);
        confirmLore.add("<gray>After purchase: <white>$" + (balance - totalPrice));
        confirmLore.add("<green>Click to confirm!");

        ItemStack confirmItem = MessageUtils.createItem(confirmMat, confirmName, confirmLore);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "confirm_buy");
        confirmItem.setItemMeta(confirmMeta);
        inv.setItem(11, confirmItem);

        // Cancel Button
        Material cancelMat = Material.valueOf(cm.getConfig().getString("items.cancel.material", "RED_STAINED_GLASS_PANE"));
        String cancelName = cm.getConfig().getString("items.cancel.name", "<red><bold>✘ Cancel");
        ItemStack cancelItem = MessageUtils.createItem(cancelMat, cancelName, new ArrayList<>());
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "cancel_buy");
        cancelItem.setItemMeta(cancelMeta);
        inv.setItem(15, cancelItem);

        player.openInventory(inv);
    }

    public void openAmountSelector(Player player, String categoryId, String itemId) {
        ConfigManager cm = plugin.getConfigManager();
        FileConfiguration catConfig = cm.getCategory(cm.getConfig().getString("categories." + categoryId + ".file"));
        ConfigurationSection itemSection = catConfig.getConfigurationSection("items." + itemId);
        if (itemSection == null) return;

        double basePrice = itemSection.getDouble("price");
        int baseAmount = itemSection.getInt("amount");
        double unitPrice = basePrice / baseAmount;

        String title = "<gradient:#a855f7:#f59e0b>Select Amount";
        Inventory inv = Bukkit.createInventory(new ShopHolder("amount:" + categoryId + ":" + itemId), 27, MessageUtils.format(title));

        ItemStack filler = getFiller();
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        List<Integer> options = cm.getConfig().getIntegerList("gui.amount-options");
        int[] slots = {11, 12, 13, 14, 15};
        
        for (int i = 0; i < Math.min(options.size(), slots.length); i++) {
            int amount = options.get(i);
            double total = unitPrice * amount;
            
            ItemStack optItem = new ItemStack(Material.valueOf(itemSection.getString("material")));
            optItem.setAmount(amount);
            ItemMeta meta = optItem.getItemMeta();
            meta.displayName(MessageUtils.format("<gradient:#a855f7:#f59e0b>Buy " + amount + "x"));
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Amount: <white>" + amount + "x");
            lore.add("<gray>Total: <green>$" + total);
            lore.add("<green>Click to select!");
            meta.lore(MessageUtils.formatList(lore));
            meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "select_amount");
            meta.getPersistentDataContainer().set(DATA_KEY, PersistentDataType.STRING, String.valueOf(amount));
            optItem.setItemMeta(meta);
            
            inv.setItem(slots[i], optItem);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof ShopHolder holder)) return;

        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String action = meta.getPersistentDataContainer().get(ACTION_KEY, PersistentDataType.STRING);
        String data = meta.getPersistentDataContainer().get(DATA_KEY, PersistentDataType.STRING);

        if (action == null) return;

        switch (action) {
            case "open_category":
                openCategory(player, data, 0);
                break;
            case "change_page":
                String[] parts = data.split(":");
                openCategory(player, parts[0], Integer.parseInt(parts[1]));
                break;
            case "back_to_main":
                openMainMenu(player);
                break;
            case "buy_item":
                String[] buyParts = data.split(":");
                if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                    if (plugin.getConfigManager().getConfig().getBoolean("gui.shift-click-amount", true)) {
                        openAmountSelector(player, buyParts[0], buyParts[1]);
                    }
                } else {
                    if (plugin.getConfigManager().getConfig().getBoolean("gui.confirm-purchase", true) && !player.hasPermission("krudshop.bypass.confirm")) {
                        openConfirmGui(player, buyParts[0], buyParts[1], item.getAmount());
                    } else {
                        processPurchase(player, buyParts[0], buyParts[1], item.getAmount());
                    }
                }
                break;
            case "select_amount":
                String[] amtParts = holder.getType().split(":");
                int selectedAmount = Integer.parseInt(data);
                if (plugin.getConfigManager().getConfig().getBoolean("gui.confirm-purchase", true) && !player.hasPermission("krudshop.bypass.confirm")) {
                    openConfirmGui(player, amtParts[1], amtParts[2], selectedAmount);
                } else {
                    processPurchase(player, amtParts[1], amtParts[2], selectedAmount);
                }
                break;
            case "confirm_buy":
                String[] confParts = holder.getType().split(":");
                processPurchase(player, confParts[1], confParts[2], Integer.parseInt(confParts[3]));
                break;
            case "cancel_buy":
                String[] cancelParts = holder.getType().split(":");
                openCategory(player, cancelParts[1], 0);
                playSound(player, "cancel");
                break;
        }
    }

    private void processPurchase(Player player, String categoryId, String itemId, int amount) {
        ConfigManager cm = plugin.getConfigManager();
        FileConfiguration catConfig = cm.getCategory(cm.getConfig().getString("categories." + categoryId + ".file"));
        ConfigurationSection itemSection = catConfig.getConfigurationSection("items." + itemId);
        if (itemSection == null) return;

        double basePrice = itemSection.getDouble("price");
        int baseAmount = itemSection.getInt("amount");
        double totalPrice = (basePrice / baseAmount) * amount;

        if (!plugin.getEconomyManager().hasMoney(player, totalPrice)) {
            sendDonutMessage(player, "no-money", Map.of(
                    "price", String.valueOf(totalPrice),
                    "balance", String.valueOf(plugin.getEconomyManager().getBalance(player)),
                    "missing", String.valueOf(totalPrice - plugin.getEconomyManager().getBalance(player))
            ));
            playSound(player, "error");
            player.closeInventory();
            return;
        }

        // Check inventory space
        ItemStack purchaseItem = new ItemStack(Material.valueOf(itemSection.getString("material")), amount);
        if (!hasSpace(player, purchaseItem)) {
            player.sendMessage(MessageUtils.format(cm.getConfig().getString("messages.inventory-full", "<red>✘ Your inventory is full!")));
            playSound(player, "error");
            return;
        }

        plugin.getEconomyManager().withdraw(player, totalPrice);
        player.getInventory().addItem(purchaseItem);

        sendDonutMessage(player, "purchase-success", Map.of(
                "item", itemSection.getString("display-name"),
                "amount", String.valueOf(amount),
                "price", String.valueOf(totalPrice),
                "balance", String.valueOf(plugin.getEconomyManager().getBalance(player))
        ));
        playSound(player, "purchase");
        
        // Re-open category or close? Let's re-open category
        openCategory(player, categoryId, 0);
    }

    private boolean hasSpace(Player player, ItemStack item) {
        int free = 0;
        for (ItemStack i : player.getInventory().getStorageContents()) {
            if (i == null || i.getType() == Material.AIR) {
                free += item.getMaxStackSize();
            } else if (i.isSimilar(item)) {
                free += item.getMaxStackSize() - i.getAmount();
            }
        }
        return free >= item.getAmount();
    }

    private void sendDonutMessage(Player player, String type, Map<String, String> placeholders) {
        ConfigManager cm = plugin.getConfigManager();
        String separator = cm.getConfig().getString("messages.separator", "<#7c3aed>⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯");
        String prefix = cm.getConfig().getString("messages.prefix", "[ᴋʀᴜᴅ ꜱʜᴏᴘ]");
        
        player.sendMessage(MessageUtils.format(separator));
        
        String msg;
        if (type.equals("purchase-success")) {
            player.sendMessage(MessageUtils.format(" " + prefix + " <gradient:#a855f7:#f59e0b><bold>✔ Purchased!</bold></gradient>"));
            player.sendMessage(MessageUtils.format(separator));
            msg = "<gray>🛒 Item: <gradient:#a855f7:#f59e0b>{item}</gradient>\n" +
                  "<gray>📦 Amount: <white>{amount}x\n" +
                  "<gray>💰 Paid: <green>${price}\n" +
                  "<gray>💰 Balance: <white>${balance}";
        } else if (type.equals("no-money")) {
            player.sendMessage(MessageUtils.format(" " + prefix + " <gradient:#a855f7:#f59e0b><bold>✘ No Money!</bold></gradient>"));
            player.sendMessage(MessageUtils.format(separator));
            msg = "<red>✘ Insufficient funds!\n" +
                  "<gray>💰 Need: <red>${price}\n" +
                  "<gray>💰 Have: <red>${balance}\n" +
                  "<gray>💰 Missing: <red>${missing}";
        } else {
            msg = cm.getConfig().getString("messages." + type, "");
        }

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        for (String line : msg.split("\n")) {
            player.sendMessage(MessageUtils.format(line));
        }
        
        player.sendMessage(MessageUtils.format(separator));
    }

    private void playSound(Player player, String type) {
        String soundName = plugin.getConfigManager().getConfig().getString("sounds." + type);
        if (soundName != null) {
            try {
                player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(soundName), 1f, 1f);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private static class ShopHolder implements InventoryHolder {
        private final String type;
        public ShopHolder(String type) { this.type = type; }
        public String getType() { return type; }
        @Override public @NotNull Inventory getInventory() { return null; }
    }
}
