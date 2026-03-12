package fr.archipel.archiEvent.games.dac;

import fr.archipel.archiEvent.ArchiEvent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class DACListener implements Listener {

    private final DACData dacData;
    private final DACGame dacGame;
    private BukkitTask landingWatcher;

    private double jumpY;

    private static final double MIN_FALL_DISTANCE = 2.0;

    public DACListener(DACData dacData, DACGame dacGame) {
        this.dacData = dacData;
        this.dacGame = dacGame;
    }

    public void startWatching() {
        stopWatching();

        Player jumper = getJumper();
        if (jumper == null) return;

        jumpY = jumper.getLocation().getY();

        landingWatcher = JavaPlugin.getPlugin(ArchiEvent.class)
                .getServer().getScheduler().runTaskTimer(
                        JavaPlugin.getPlugin(ArchiEvent.class),
                        this::tick,
                        10L, 2L
                );
    }

    public void stopWatching() {
        if (landingWatcher != null) {
            landingWatcher.cancel();
            landingWatcher = null;
        }
    }

    private void tick() {
        Player jumper = getJumper();
        if (jumper == null || jumper.getGameMode() != GameMode.ADVENTURE) return;

        double currentY = jumper.getLocation().getY();

        if (currentY > jumpY - MIN_FALL_DISTANCE) return;

        if (jumper.isOnGround() || isInWater(jumper)) {
            stopWatching();
            Location loc = jumper.getLocation();

            if (dacData.isInPool(loc)) {
                dacGame.onPlayerLand(jumper, loc);
            } else {
                dacGame.onPlayerMissedPool(jumper);
            }
        }
    }

    private Player getJumper() {
        if (dacData.getCurrentJumper() == null) return null;
        return JavaPlugin.getPlugin(ArchiEvent.class)
                .getServer().getPlayer(dacData.getCurrentJumper());
    }

    private boolean isInWater(Player player) {
        return player.getLocation().getBlock().getType() == Material.WATER;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.getUniqueId().equals(dacData.getCurrentJumper())) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }
}