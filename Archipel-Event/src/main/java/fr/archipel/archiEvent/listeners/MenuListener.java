package fr.archipel.archiEvent.listeners;

import fr.archipel.archiEvent.EventData;
import fr.archipel.archiEvent.EventData.RewardMode;
import fr.archipel.archiEvent.EventData.RewardType;
import fr.archipel.archiEvent.manager.MenuManager;
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

        // --- SÉLECTION DE L'EVENT ---
        if (title.equals(MenuManager.TITLE_EVENT_SELECT)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            String eventType = clicked.getItemMeta().getDisplayName();
            currentEvent.setEventType(eventType);

            // Ouvrir le bon menu selon le type d'event
            if (eventType.contains("Nexus")) {
                currentEvent.setRewardMode(RewardMode.TEAM);
                menuManager.openRewardsMenuTeam(player);
            } else {
                currentEvent.setRewardMode(RewardMode.TOP3);
                menuManager.openRewardsMenuTop3(player);
            }
            return;
        }

        // --- MENU TOP 3 ---
        if (title.equals(MenuManager.TITLE_REWARDS_TOP3)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            int slot = event.getRawSlot();
            Inventory inv = event.getInventory();

            if (slot == 26) {
                // Validation — lire les 3 lignes (places 1, 2, 3)
                for (int rank = 1; rank <= 3; rank++) {
                    int startSlot = (rank - 1) * 9;
                    RewardType[] types = RewardType.values();
                    for (int j = 0; j < types.length; j++) {
                        ItemStack item = inv.getItem(startSlot + 2 + j);
                        if (item != null && item.hasItemMeta()) {
                            currentEvent.setReward(rank, types[j], getCountFromLore(item));
                        }
                    }
                }
                player.sendMessage("§a§l[ArchiEvent] §fConfiguration enregistrée !");
                player.closeInventory();
                return;
            }

            handleKeyClick(event, clicked);
        }

        // --- MENU GAGNANT/PERDANT ---
        if (title.equals(MenuManager.TITLE_REWARDS_TEAM)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            int slot = event.getRawSlot();
            Inventory inv = event.getInventory();

            if (slot == 17) {
                // Validation — ligne 0 = gagnant (place 1), ligne 1 = perdant (place 2)
                RewardType[] types = RewardType.values();
                for (int j = 0; j < types.length; j++) {
                    ItemStack winner = inv.getItem(2 + j);
                    if (winner != null && winner.hasItemMeta()) {
                        currentEvent.setReward(1, types[j], getCountFromLore(winner));
                    }
                    ItemStack loser = inv.getItem(11 + j);
                    if (loser != null && loser.hasItemMeta()) {
                        currentEvent.setReward(2, types[j], getCountFromLore(loser));
                    }
                }
                player.sendMessage("§a§l[ArchiEvent] §fConfiguration enregistrée !");
                player.closeInventory();
                return;
            }

            handleKeyClick(event, clicked);
        }
    }

    private void handleKeyClick(InventoryClickEvent event, ItemStack clicked) {
        for (RewardType type : RewardType.values()) {
            if (clicked.getType() == type.getGuiMaterial()) {
                boolean isAdd = (event.getClick() == ClickType.LEFT);
                boolean isRemove = (event.getClick() == ClickType.RIGHT);
                if (isAdd || isRemove) updateKeyCount(clicked, isAdd, type.getGuiMaterial());
                break;
            }
        }
    }

    private void updateKeyCount(ItemStack item, boolean add, Material originalMaterial) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        boolean isMoney = originalMaterial == EventData.RewardType.MONEY.getGuiMaterial()
                && item.getItemMeta().getDisplayName().contains("Money");
        int step = isMoney ? 500 : 1;

        List<String> lore = meta.getLore();
        int count = 0;
        if (lore != null && !lore.isEmpty()) {
            String digits = lore.get(0).replaceAll("[^0-9]", "");
            count = digits.isEmpty() ? 0 : Integer.parseInt(digits);
        }

        if (add) count += step;
        else if (count >= step) count -= step;

        List<String> newLore = new ArrayList<>();
        if (isMoney) {
            newLore.add("§fSomme : §a" + count + "$");
            newLore.add(" ");
            newLore.add("§7Clique gauche: §a+500$");
            newLore.add("§7Clique droit: §c-500$");
        } else {
            newLore.add("§fQuantité : §b" + count);
            newLore.add(" ");
            newLore.add("§7Clique gauche: §a+1");
            newLore.add("§7Clique droit: §c-1");
        }
        meta.setLore(newLore);
        item.setItemMeta(meta);
        item.setType(originalMaterial);
    }

    private int getCountFromLore(ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getLore() == null) return 0;
        List<String> lore = item.getItemMeta().getLore();
        if (lore.isEmpty()) return 0;
        try {
            // Fonctionne pour "Quantité : 3" et "Somme : 1500$"
            return Integer.parseInt(lore.get(0).replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}