package fr.archipel.archiEvent.manager;

import fr.archipel.archiEvent.EventData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MenuManager {

    // On transforme ça en méthode propre
    public void openCreationMenu(Player player) {
        player.sendMessage("§b[ArchiEvent] §fOuverture du sélecteur d'événement...");

        // Un inventaire de 9 slots suffit largement ici
        Inventory gui = Bukkit.createInventory(null, 9, "§8Choisis ton événement");

// --- ITEM: QUIZ ---
        ItemStack quiz = new ItemStack(Material.BOOK);
        ItemMeta quizMeta = quiz.getItemMeta();
        if (quizMeta != null) {
            quizMeta.setDisplayName("§e§lQuiz / Question");
            List<String> lore = new ArrayList<>();
            lore.add("§7Mode: Questions dans le chat");
            lore.add("§7Récompenses: Aux 3 plus rapides");
            quizMeta.setLore(lore);
            quiz.setItemMeta(quizMeta);
        }

// --- ITEM: SPLEEF ---
        ItemStack spleef = new ItemStack(Material.SNOW_BLOCK);
        ItemMeta spleefMeta = spleef.getItemMeta();
        if (spleefMeta != null) {
            spleefMeta.setDisplayName("§f§lSpleef");
            List<String> lore = new ArrayList<>();
            lore.add("§7Mode: Dernier debout sur la neige");
            lore.add("§7Récompenses: Top 3 survivants");
            spleefMeta.setLore(lore);
            spleef.setItemMeta(spleefMeta);
        }

        // --- ITEM 3: DAC ---
        ItemStack dac = new ItemStack(Material.WATER_BUCKET);
        ItemMeta dacMeta = dac.getItemMeta();
        if (dacMeta != null) {
            dacMeta.setDisplayName("§b§lDès à coudre");
            dac.setItemMeta(dacMeta);
        }

        gui.setItem(2, quiz);
        gui.setItem(4, spleef);
        gui.setItem(6, dac);

        player.openInventory(gui);
    }

    public void openRewardsMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§8Configuration des Récompenses");

        // On boucle sur les 3 places du podium
        for (int rank = 1; rank <= 3; rank++) {
            int startSlot = (rank - 1) * 9;

            // Label de la ligne (1er, 2ème, 3ème)
            gui.setItem(startSlot, createItem(Material.PAPER, "§e§l" + rank + (rank == 1 ? "er" : "ème") + " Place", "§7Configure les lots à droite"));

            // On place les icônes de l'Enum RewardType
            EventData.RewardType[] types = EventData.RewardType.values();
            for (int j = 0; j < types.length; j++) {
                EventData.RewardType type = types[j];
                gui.setItem(startSlot + 2 + j, createItem(type.getGuiMaterial(), type.getDisplayName(),
                        "§fQuantité : §b0",
                        " ",
                        "§7Clique gauche: §a+1",
                        "§7Clique droit: §c-1"));
            }
        }

        // Bouton Valider
        gui.setItem(26, createItem(Material.EMERALD_BLOCK, "§a§lVALIDER", "§7Enregistrer la config"));

        player.openInventory(gui);
    }

    // Utilitaire pour créer des items rapidement
    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}