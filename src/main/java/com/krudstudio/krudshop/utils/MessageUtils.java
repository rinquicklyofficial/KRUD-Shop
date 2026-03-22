package com.krudstudio.krudshop.utils;

import com.krudstudio.krudshop.KRUDShop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MessageUtils {

    private static final MiniMessage mm = KRUDShop.getMiniMessage();
    private static final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();

    public static Component format(String message) {
        if (message == null) return Component.empty();
        // Support legacy & codes by converting to MiniMessage compatible or just using legacy serializer
        if (message.contains("&")) {
            return legacySerializer.deserialize(message);
        }
        return mm.deserialize(message);
    }

    public static List<Component> formatList(List<String> list) {
        if (list == null) return new ArrayList<>();
        return list.stream().map(MessageUtils::format).collect(Collectors.toList());
    }

    public static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(format(name));
            meta.lore(formatList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}
