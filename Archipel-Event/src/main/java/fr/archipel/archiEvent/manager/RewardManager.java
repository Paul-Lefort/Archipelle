package fr.archipel.archiEvent.manager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class RewardManager {

    public void openRewardsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Configuration des Récompenses");

        String[] keyNames = {"§6Légendaire", "§aSpawners", "§bQuantique", "§dAntique"};
        String[] places = {"§6§l1ère Place", "§f§l2ème Place", "§c§l3ème Place"};

        for (int i = 0; i < 3; i++) {
            inv.setItem(i * 9, createItem(Material.PAPER, places[i], "§7Configuration des clés"));

            for (int j = 0; j < 4; j++) {
                inv.setItem((i * 9) + 2 + j, createItem(Material.BLACK_STAINED_GLASS_PANE, keyNames[j], "§eQuantité : §f0"));
            }
        }

        inv.setItem(26, createItem(Material.EMERALD_BLOCK, "§2§lVALIDER", "§7Enregistrer et fermer"));

        player.openInventory(inv);
    }

    public ItemStack createItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lores = new ArrayList<>();
            lores.add(lore);
            lores.add("");
            if (mat == Material.BLACK_STAINED_GLASS_PANE || mat.name().contains("NUGGET") || mat.name().contains("STAR")) {
                lores.add("§7Clic Gauche : §a+1");
                lores.add("§7Clic Droit : §c-1");
            }
            meta.setLore(lores);
            item.setItemMeta(meta);
        }
        return item;
    }
}