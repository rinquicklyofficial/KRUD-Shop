package com.krudstudio.krudshop.commands;

import com.krudstudio.krudshop.KRUDShop;
import com.krudstudio.krudshop.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ShopCommand implements CommandExecutor, TabCompleter {

    private final KRUDShop plugin;

    public ShopCommand(KRUDShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (!player.hasPermission("krudshop.admin")) {
                    player.sendMessage(MessageUtils.format(plugin.getConfigManager().getConfig().getString("messages.no-permission")));
                    return true;
                }
                plugin.getConfigManager().loadConfigs();
                sendReloadMessage(player);
                return true;
            }

            // Check if it's a category
            if (plugin.getConfigManager().getConfig().contains("categories." + args[0])) {
                plugin.getInventoryManager().openCategory(player, args[0], 0);
                return true;
            }
        }

        plugin.getInventoryManager().openMainMenu(player);
        return true;
    }

    private void sendReloadMessage(Player player) {
        String separator = plugin.getConfigManager().getConfig().getString("messages.separator", "<#7c3aed>⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯");
        String prefix = plugin.getConfigManager().getConfig().getString("messages.prefix", "[ᴋʀᴜᴅ ꜱʜᴏᴘ]");
        
        player.sendMessage(MessageUtils.format(separator));
        player.sendMessage(MessageUtils.format(" " + prefix + " <gradient:#a855f7:#f59e0b><bold>✔ Reloaded!</bold></gradient>"));
        player.sendMessage(MessageUtils.format(separator));
        player.sendMessage(MessageUtils.format("<gray>All configs and categories reloaded!"));
        player.sendMessage(MessageUtils.format(separator));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("reload");
            completions.addAll(plugin.getConfigManager().getConfig().getConfigurationSection("categories").getKeys(false));
            return completions.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        return completions;
    }
}
