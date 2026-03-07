package fr.archipel.archiEvent.games.nexus;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

public class NexusListener implements Listener {

    private final NexusData nexusData;
    private final NexusGame nexusGame;

    public NexusListener(NexusData nexusData, NexusGame nexusGame) {
        this.nexusData = nexusData;
        this.nexusGame = nexusGame;
    }

    @EventHandler
    public void onCrystalExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal crystal)) return;
        if (isNexusCrystal(crystal)) event.setCancelled(true);
    }

    @EventHandler
    public void onCrystalEnvironmentalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal crystal)) return;
        if (!isNexusCrystal(crystal)) return;
        if (!(event instanceof EntityDamageByEntityEvent)) event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerHitCrystal(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal crystal)) return;
        if (!isNexusCrystal(crystal)) return;

        // Annuler dans tous les cas — on gère nous-mêmes
        event.setCancelled(true);

        // Remonter au joueur tireur si c'est un projectile (flèche, trident, etc.)
        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player p) attacker = p;
        }

        if (attacker == null) return;

        UUID attackerId = attacker.getUniqueId();
        NexusTeam attackerTeam = nexusData.getTeamOf(attackerId);

        // Pas dans une équipe ou frappe son propre cristal → rien
        if (attackerTeam == null) return;

        NexusTeam opponentTeam = nexusData.getOpponentTeam(attackerTeam);
        if (opponentTeam.getCrystal() == null) return;
        if (!opponentTeam.getCrystal().getUniqueId().equals(crystal.getUniqueId())) return;

        // Appliquer les dégâts
        double damage = event.getFinalDamage() > 0 ? event.getFinalDamage() : 1.0;
        nexusData.addDamage(attackerId, damage);

        if (opponentTeam == nexusData.getTeamRed()) {
            nexusData.damageRedCrystal(damage);
            double hp = nexusData.getRedCrystalHp();
            sendHpActionBar(attacker, "§c", "Nexus Rouge", hp);
            nexusGame.updateScoreboard(nexusData.getDamageDealt());
            if (hp <= 0) nexusGame.onCrystalDestroyed(nexusData.getTeamBlue(), nexusData.getTeamRed());
        } else {
            nexusData.damageBlueCrystal(damage);
            double hp = nexusData.getBlueCrystalHp();
            sendHpActionBar(attacker, "§9", "Nexus Bleu", hp);
            nexusGame.updateScoreboard(nexusData.getDamageDealt());
            if (hp <= 0) nexusGame.onCrystalDestroyed(nexusData.getTeamRed(), nexusData.getTeamBlue());
        }
    }

    private void sendHpActionBar(Player player, String color, String teamName, double hp) {
        int max = (int) NexusData.NEXUS_MAX_HP;
        int percent = (int) ((hp / max) * 100);
        int filled = percent / 5;

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            bar.append(i < filled ? color + "█" : "§8█");
        }

        String message = color + "❤ " + teamName + " §f" + String.format("%.0f", hp) + "/" + max + " §7[" + bar + "§7]";
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    private boolean isNexusCrystal(EnderCrystal crystal) {
        UUID id = crystal.getUniqueId();
        return (nexusData.getTeamRed().getCrystal() != null && nexusData.getTeamRed().getCrystal().getUniqueId().equals(id))
                || (nexusData.getTeamBlue().getCrystal() != null && nexusData.getTeamBlue().getCrystal().getUniqueId().equals(id));
    }
}