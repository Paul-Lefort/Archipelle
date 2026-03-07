package fr.archipel.archiEvent.manager;

import fr.archipel.archiEvent.EventData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class MenuManager {

    public static final String TITLE_EVENT_SELECT = "§8Choisis ton événement";
    public static final String TITLE_REWARDS_TOP3 = "§8Récompenses - Top 3";
    public static final String TITLE_REWARDS_TEAM = "§8Récompenses - Équipes";

    public void openCreationMenu(Player player) {
        player.sendMessage("§b[ArchiEvent] §fOuverture du sélecteur d'événement...");
        Inventory gui = Bukkit.createInventory(null, 9, TITLE_EVENT_SELECT);

        gui.setItem(1, createItem(Material.BOOK, "§e§lQuiz / Question",
                "§7Mode: Questions dans le chat",
                "§7Récompenses: Top 3 individuel"));

        gui.setItem(3, createItem(Material.SNOW_BLOCK, "§f§lSpleef",
                "§7Mode: Dernier debout sur la neige",
                "§7Récompenses: Top 3 survivants"));

        gui.setItem(5, createItem(Material.WATER_BUCKET, "§b§lDès à coudre"));

        gui.setItem(7, createItem(Material.END_CRYSTAL, "§5§lNexus",
                "§7Mode: 2 équipes, détruisez le Nexus adverse",
                "§7Récompenses: Gagnant / Perdant"));

        player.openInventory(gui);
    }

    // Menu Top 3 — pour Quiz, Spleef, etc.
    public void openRewardsMenuTop3(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, TITLE_REWARDS_TOP3);

        String[] labels = {"§6§l1er", "§f§l2ème", "§c§l3ème"};
        for (int rank = 1; rank <= 3; rank++) {
            int startSlot = (rank - 1) * 9;
            gui.setItem(startSlot, createItem(Material.PAPER, labels[rank - 1], "§7Configure les lots à droite"));

            EventData.RewardType[] types = EventData.RewardType.values();
            for (int j = 0; j < types.length; j++) {
                gui.setItem(startSlot + 2 + j, createItem(types[j].getGuiMaterial(), types[j].getDisplayName(),
                        "§fQuantité : §b0", " ", "§7Clique gauche: §a+1", "§7Clique droit: §c-1"));
            }
        }

        gui.setItem(26, createItem(Material.EMERALD_BLOCK, "§a§lVALIDER", "§7Enregistrer la config"));
        player.openInventory(gui);
    }

    // Menu Gagnant/Perdant — pour Nexus et events par équipes
    public void openRewardsMenuTeam(Player player) {
        Inventory gui = Bukkit.createInventory(null, 18, TITLE_REWARDS_TEAM);

        gui.setItem(0, createItem(Material.GOLD_BLOCK, "§6§l🏆 Équipe Gagnante", "§7Récompenses pour toute la team gagnante"));
        gui.setItem(9, createItem(Material.IRON_BLOCK, "§7§l😔 Équipe Perdante", "§7Récompenses de consolation"));

        EventData.RewardType[] types = EventData.RewardType.values();
        for (int j = 0; j < types.length; j++) {
            // Ligne gagnant (row 0)
            gui.setItem(2 + j, createItem(types[j].getGuiMaterial(), types[j].getDisplayName(),
                    "§fQuantité : §b0", " ", "§7Clique gauche: §a+1", "§7Clique droit: §c-1"));
            // Ligne perdant (row 1)
            gui.setItem(11 + j, createItem(types[j].getGuiMaterial(), types[j].getDisplayName(),
                    "§fQuantité : §b0", " ", "§7Clique gauche: §a+1", "§7Clique droit: §c-1"));
        }

        gui.setItem(17, createItem(Material.EMERALD_BLOCK, "§a§lVALIDER", "§7Enregistrer la config"));
        player.openInventory(gui);
    }

    public void openRewardsMenu(Player player) {
        openRewardsMenuTop3(player);
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