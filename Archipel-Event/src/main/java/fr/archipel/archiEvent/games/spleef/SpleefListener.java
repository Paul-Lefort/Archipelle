package fr.archipel.archiEvent.games.spleef;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.GameMode;

public class SpleefListener implements Listener {

    private final SpleefData spleefData;
    private final SpleefGame spleefGame;

    public SpleefListener(SpleefData spleefData, SpleefGame spleefGame) {
        this.spleefData = spleefData;
        this.spleefGame = spleefGame;
    }

    // Casser uniquement la neige, instantanément
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!spleefData.isInCurrentMatch(player.getUniqueId())) return;

        if (event.getBlock().getType() == Material.SNOW_BLOCK) {
            event.setDropItems(false); // Pas de drops
            // La neige est déjà cassée par l'event, on laisse passer
        } else {
            // Empêcher de casser autre chose
            event.setCancelled(true);
        }
    }

    // Détecter la chute dans la lave
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!spleefData.isInCurrentMatch(player.getUniqueId())) return;
        if (player.getGameMode() != GameMode.SURVIVAL) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.LAVA
                || event.getCause() == EntityDamageEvent.DamageCause.FIRE
                || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
            event.setCancelled(true);
            spleefGame.onPlayerLost(player);
        }
    }

    // Détecter si un joueur touche la lave ou tombe sous la neige
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!spleefData.isInCurrentMatch(player.getUniqueId())) return;
        if (player.getGameMode() != GameMode.SURVIVAL) return;

        Material feet = player.getLocation().getBlock().getType();
        Material below = player.getLocation().clone().subtract(0, 1, 0).getBlock().getType();

        if (feet == Material.LAVA || below == Material.LAVA
                || feet == Material.BARRIER) {
            spleefGame.onPlayerLost(player);
        }
    }

    // Annuler les dégâts PvP entre les joueurs
    @EventHandler
    public void onPvP(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!spleefData.isInCurrentMatch(player.getUniqueId())) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            event.setCancelled(true);
        }
    }
}