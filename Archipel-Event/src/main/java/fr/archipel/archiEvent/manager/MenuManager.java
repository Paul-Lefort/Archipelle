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

    public void openCreationMenu(Player player) {
        player.sendMessage("§b[ArchiEvent] §fOuverture du sélecteur d'événement...");

        Inventory gui = Bukkit.createInventory(null, 9, "§8Choisis ton événement");

        // QUIZ
        ItemStack quiz = new ItemStack(Material.BOOK);
        ItemMeta quizMeta = quiz.getItemMeta();
        if (quizMeta != null) {
            quizMeta.setDisplayName("§e§lQuiz / Question");
            quizMeta.setLore(Arrays.asList("§7Mode: Questions dans le chat", "§7Récompenses: Aux 3 plus rapides"));
            quiz.setItemMeta(quizMeta);
        }

        // SPLEEF
        ItemStack spleef = new ItemStack(Material.SNOW_BLOCK);
        ItemMeta spleefMeta = spleef.getItemMeta();
        if (spleefMeta != null) {
            spleefMeta.setDisplayName("§f§lSpleef");
            spleefMeta.setLore(Arrays.asList("§7Mode: Dernier debout sur la neige", "§7Récompenses: Top 3 survivants"));
            spleef.setItemMeta(spleefMeta);
        }

        // DAC
        ItemStack dac = new ItemStack(Material.WATER_BUCKET);
        ItemMeta dacMeta = dac.getItemMeta();
        if (dacMeta != null) {
            dacMeta.setDisplayName("§b§lDès à coudre");
            dac.setItemMeta(dacMeta);
        }

        // NEXUS
        ItemStack nexus = new ItemStack(Material.END_CRYSTAL);
        ItemMeta nexusMeta = nexus.getItemMeta();
        if (nexusMeta != null) {
            nexusMeta.setDisplayName("§5§lNexus");
            nexusMeta.setLore(Arrays.asList(
                    "§7Mode: 2 équipes, détruisez le Nexus adverse",
                    "§7Récompenses: Toute la team gagnante"));
            nexus.setItemMeta(nexusMeta);
        }

        gui.setItem(1, quiz);
        gui.setItem(3, spleef);
        gui.setItem(5, dac);
        gui.setItem(7, nexus);

        player.openInventory(gui);
    }

    public void openRewardsMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§8Configuration des Récompenses");

        for (int rank = 1; rank <= 3; rank++) {
            int startSlot = (rank - 1) * 9;
            gui.setItem(startSlot, createItem(Material.PAPER,
                    "§e§l" + rank + (rank == 1 ? "er" : "ème") + " Place",
                    "§7Configure les lots à droite"));

            EventData.RewardType[] types = EventData.RewardType.values();
            for (int j = 0; j < types.length; j++) {
                EventData.RewardType type = types[j];
                gui.setItem(startSlot + 2 + j, createItem(type.getGuiMaterial(), type.getDisplayName(),
                        "§fQuantité : §b0", " ", "§7Clique gauche: §a+1", "§7Clique droit: §c-1"));
            }
        }

        gui.setItem(26, createItem(Material.EMERALD_BLOCK, "§a§lVALIDER", "§7Enregistrer la config"));
        player.openInventory(gui);
    }

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