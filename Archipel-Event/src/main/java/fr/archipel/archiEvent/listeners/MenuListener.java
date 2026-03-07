package fr.archipel.archiEvent.listeners;

import fr.archipel.archiEvent.EventData;
import fr.archipel.archiEvent.EventData.RewardType; // On importe l'Enum
import fr.archipel.archiEvent.manager.MenuManager; // Assure-toi que c'est le bon nom
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MenuListener implements Listener {

    private final EventData currentEvent;
    private final MenuManager menuManager = new MenuManager();

    public MenuListener(EventData eventData) {
        this.currentEvent = eventData;
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        // --- GESTION DU PREMIER MENU ---
        if (title.equals("§8Choisis ton événement")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            currentEvent.setEventType(clicked.getItemMeta().getDisplayName());
            menuManager.openRewardsMenu(player);
            return;
        }

        // --- GESTION DU DEUXIÈME MENU ---
        if (title.equals("§8Configuration des Récompenses")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            int slot = event.getRawSlot();
            Inventory inv = event.getInventory();

            // --- VALIDATION (SLOT 26) ---
            if (slot == 26) {
                for (int rank = 1; rank <= 3; rank++) {
                    int startSlot = (rank - 1) * 9;

                    RewardType[] types = RewardType.values();
                    for (int j = 0; j < types.length; j++) {
                        int targetSlot = startSlot + 2 + j;
                        ItemStack item = inv.getItem(targetSlot);

                        if (item != null && item.hasItemMeta()) {
                            int count = getCountFromLore(item);
                            // CORRECTIF : On passe directement l'Enum 'types[j]'
                            currentEvent.setReward(rank, types[j], count);
                        }
                    }
                }
                player.sendMessage("§a§l[ArchiEvent] §fConfiguration enregistrée !");
                player.closeInventory();
                return;
            }

            // --- CLIC SUR UNE CLÉ (+ / -) ---
            for (RewardType type : RewardType.values()) {
                if (clicked.getType() == type.getGuiMaterial()) {
                    boolean isAdd = (event.getClick() == ClickType.LEFT);
                    boolean isRemove = (event.getClick() == ClickType.RIGHT);

                    if (isAdd || isRemove) {
                        updateKeyCount(clicked, isAdd, type.getGuiMaterial());
                    }
                    break;
                }
            }
        }
    }

    private void updateKeyCount(ItemStack item, boolean add, Material originalMaterial) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<String> lore = meta.getLore();
        int count = 0;

        if (lore != null && !lore.isEmpty()) {
            String digits = lore.get(0).replaceAll("[^0-9]", "");
            count = digits.isEmpty() ? 0 : Integer.parseInt(digits);
        }

        if (add) count++;
        else if (count > 0) count--;

        List<String> newLore = new ArrayList<>();
        newLore.add("§fQuantité : §b" + count);
        newLore.add(" ");
        newLore.add("§7Clique gauche: §a+1");
        newLore.add("§7Clique droit: §c-1");
        meta.setLore(newLore);
        item.setItemMeta(meta);

        item.setType(originalMaterial);
    }

    private int getCountFromLore(ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getLore() == null) return 0;
        List<String> lore = item.getItemMeta().getLore();
        if (lore.isEmpty()) return 0;
        try {
            return Integer.parseInt(lore.get(0).replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}