package fr.archipel.archiEvent.games.nexus;

import fr.archipel.archiEvent.EventData;
import fr.archipel.archiEvent.games.Game;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;

public class NexusGame implements Game {

    private final EventData eventData;
    private final NexusData nexusData;
    private Runnable onGameEnd;

    // Scoreboard partagé par tous les joueurs
    private Scoreboard scoreboard;
    private Objective objective;
    private ArmorStand holoRed;
    private ArmorStand holoBlue;
    private boolean gameEnded = false;

    public NexusGame(EventData eventData, NexusData nexusData) {
        this.eventData = eventData;
        this.nexusData = nexusData;
    }

    public void setOnGameEnd(Runnable callback) {
        this.onGameEnd = callback;
    }

    // --- DÉMARRAGE ---

    public void start() {
        // Récupérer uniquement les joueurs inscrits
        List<UUID> registeredUuids = new ArrayList<>(eventData.getRegisteredPlayers());
        Collections.shuffle(registeredUuids);

        List<Player> online = new ArrayList<>();
        for (UUID uuid : registeredUuids) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) online.add(p);
        }

        for (int i = 0; i < online.size(); i++) {
            UUID uuid = online.get(i).getUniqueId();
            if (i < online.size() / 2) {
                nexusData.getTeamRed().addMember(uuid);
            } else {
                nexusData.getTeamBlue().addMember(uuid);
            }
        }

        // Spawn cristaux
        Location redLoc = nexusData.getPendingRedLocation();
        EnderCrystal redCrystal = redLoc.getWorld().spawn(redLoc, EnderCrystal.class, crystal -> {
            crystal.setShowingBottom(false);
            crystal.setInvulnerable(false);
        });
        nexusData.getTeamRed().setCrystal(redCrystal);
        nexusData.getTeamRed().setCrystalLocation(redLoc);

        Location blueLoc = nexusData.getPendingBlueLocation();
        EnderCrystal blueCrystal = blueLoc.getWorld().spawn(blueLoc, EnderCrystal.class, crystal -> {
            crystal.setShowingBottom(false);
            crystal.setInvulnerable(false);
        });
        nexusData.getTeamBlue().setCrystal(blueCrystal);
        nexusData.getTeamBlue().setCrystalLocation(blueLoc);

        // Spawn hologrammes au-dessus des cristaux
        holoRed = spawnHologram(redLoc.clone().add(0, 2.5, 0), "§c§lNexus Équipe Rouge");
        holoBlue = spawnHologram(blueLoc.clone().add(0, 2.5, 0), "§9§lNexus Équipe Bleue");

        // Init scoreboard
        initScoreboard(online);

        // Annonce
        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
        Bukkit.broadcastMessage("§e§l   NEXUS - Début de l'événement !");
        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage("§c   Équipe Rouge §f(" + nexusData.getTeamRed().getMembers().size() + " joueurs)");
        Bukkit.broadcastMessage("§9   Équipe Bleue §f(" + nexusData.getTeamBlue().getMembers().size() + " joueurs)");
        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage("§f   Détruisez le Nexus adverse !");
        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");

        for (Player p : online) {
            NexusTeam team = nexusData.getTeamOf(p.getUniqueId());
            if (team != null) p.sendMessage("§fTon équipe : " + team.getName());
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
    }

    // --- SCOREBOARD ---

    private void initScoreboard(List<Player> players) {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective("nexus", Criteria.DUMMY, "§6§lNEXUS §r§8| §fDégâts");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank()); // Cache les chiffres à droite

        // Ligne vide en haut
        scoreboard.resetScores(" ");

        // HP des nexus
        objective.getScore("§c❤ Nexus Rouge").setScore(16);
        objective.getScore("§c" + formatHp(NexusData.NEXUS_MAX_HP)).setScore(15);
        objective.getScore("  ").setScore(14);
        objective.getScore("§9❤ Nexus Bleu").setScore(13);
        objective.getScore("§9" + formatHp(NexusData.NEXUS_MAX_HP)).setScore(12);
        objective.getScore("   ").setScore(11);
        objective.getScore("§e§lTop Dégâts").setScore(10);

        // Lignes joueurs (placeholders, mis à jour via updateScoreboard)
        for (int i = 1; i <= 5; i++) {
            objective.getScore("§7  -").setScore(10 - i);
        }

        for (Player p : players) {
            p.setScoreboard(scoreboard);
        }
    }

    public void updateScoreboard(Map<UUID, Double> damages) {
        if (scoreboard == null || objective == null) return;

        // Mettre à jour les HP
        updateHpLine("§c" , nexusData.getRedCrystalHp(), "§c");
        updateHpLine("§9", nexusData.getBlueCrystalHp(), "§9");

        // Trier les dégâts
        List<Map.Entry<UUID, Double>> sorted = new ArrayList<>(damages.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Nettoyer les anciennes lignes joueurs
        for (String entry : new ArrayList<>(scoreboard.getEntries())) {
            if (entry.startsWith("§f") || entry.startsWith("§7#")) {
                scoreboard.resetScores(entry);
            }
        }

        // Ajouter le top 5
        for (int i = 0; i < Math.min(5, sorted.size()); i++) {
            UUID uuid = sorted.get(i).getKey();
            double dmg = sorted.get(i).getValue();
            Player p = Bukkit.getPlayer(uuid);
            String name = p != null ? p.getName() : "?";
            NexusTeam team = nexusData.getTeamOf(uuid);
            String color = team == nexusData.getTeamRed() ? "§c" : "§9";

            String line = "§f" + (i + 1) + ". " + color + name + " §7" + String.format("%.0f", dmg);
            objective.getScore(line).setScore(9 - i);
        }
    }

    private void updateHpLine(String colorPrefix, double hp, String color) {
        // Supprimer l'ancienne ligne HP
        for (String entry : new ArrayList<>(scoreboard.getEntries())) {
            if (entry.startsWith(colorPrefix) && entry.contains("/")) {
                scoreboard.resetScores(entry);
            }
        }
        objective.getScore(color + formatHp(hp)).setScore(
                colorPrefix.equals("§c") ? 15 : 12
        );
    }

    private String formatHp(double hp) {
        int max = (int) NexusData.NEXUS_MAX_HP;
        int percent = (int) ((hp / max) * 100);
        int filled = percent / 10; // barre de 10 segments
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "█" : "§8█§r");
        }
        return " " + bar + " §f" + String.format("%.0f", hp) + "/" + max;
    }

    // --- FIN DE PARTIE ---

    public void onCrystalDestroyed(NexusTeam winner, NexusTeam loser) {
        // Empêcher le double appel au même tick
        if (gameEnded) return;
        gameEnded = true;

        // Supprimer le cristal détruit
        if (loser.getCrystal() != null && !loser.getCrystal().isDead()) {
            loser.getCrystal().remove();
        }

        // Supprimer le cristal survivant proprement
        if (winner.getCrystal() != null && !winner.getCrystal().isDead()) {
            winner.getCrystal().remove();
        }

        // Retirer les hologrammes
        clearHolograms();

        // Retirer le scoreboard de tous les joueurs
        clearScoreboard();

        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
        Bukkit.broadcastMessage("§e§l   NEXUS DÉTRUIT !");
        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage("§f   L'équipe " + loser.getName() + " §fa perdu son Nexus !");
        Bukkit.broadcastMessage("§f   Victoire de l'équipe " + winner.getName() + " §f!");
        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }

        if (onGameEnd != null) onGameEnd.run();
    }

    private ArmorStand spawnHologram(Location loc, String text) {
        return loc.getWorld().spawn(loc, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setCanPickupItems(false);
            stand.setInvulnerable(true);
            stand.setCustomName(text);
            stand.setCustomNameVisible(true);
            stand.setSmall(true);
            stand.setMarker(true); // Pas de hitbox
        });
    }

    public void clearHolograms() {
        if (holoRed != null && !holoRed.isDead()) holoRed.remove();
        if (holoBlue != null && !holoBlue.isDead()) holoBlue.remove();
        holoRed = null;
        holoBlue = null;
        gameEnded = false;
    }

    public void clearScoreboard() {
        if (scoreboard == null) return;
        Scoreboard def = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(def);
        }
        scoreboard = null;
        objective = null;
    }

    // --- CLASSEMENT FINAL ---

    @Override
    public Map<String, Integer> getRanking() {
        NexusTeam winner = determineWinner();
        if (winner == null) return new HashMap<>();

        NexusTeam loser = nexusData.getOpponentTeam(winner);
        Map<String, Integer> ranking = new HashMap<>();

        // Gagnants → place 1, Perdants → place 2
        for (UUID uuid : winner.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            String name = p != null ? p.getName() : Bukkit.getOfflinePlayer(uuid).getName();
            if (name != null) ranking.put(name, 1);
        }
        for (UUID uuid : loser.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            String name = p != null ? p.getName() : Bukkit.getOfflinePlayer(uuid).getName();
            if (name != null) ranking.put(name, 2);
        }
        return ranking;
    }

    @Override
    public Map<String, String> getDisplayScores() {
        Map<String, String> display = new HashMap<>();
        for (Map.Entry<UUID, Double> entry : nexusData.getDamageDealt().entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            String name = p != null ? p.getName() : Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name != null) display.put(name, String.format("%.0f", entry.getValue()) + " dégâts");
        }
        return display;
    }

    private NexusTeam determineWinner() {
        if (nexusData.getRedCrystalHp() <= 0) return nexusData.getTeamBlue();
        if (nexusData.getBlueCrystalHp() <= 0) return nexusData.getTeamRed();
        return null;
    }

    public void broadcastDamageRanking() {
        Map<UUID, Double> damages = nexusData.getDamageDealt();
        if (damages.isEmpty()) return;

        List<Map.Entry<UUID, Double>> sorted = new ArrayList<>(damages.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
        Bukkit.broadcastMessage("§e§l   TOP DÉGÂTS AU NEXUS ADVERSE :");
        Bukkit.broadcastMessage(" ");

        for (int i = 0; i < Math.min(5, sorted.size()); i++) {
            UUID uuid = sorted.get(i).getKey();
            double dmg = sorted.get(i).getValue();
            Player p = Bukkit.getPlayer(uuid);
            String name = p != null ? p.getName() : Bukkit.getOfflinePlayer(uuid).getName();
            NexusTeam team = nexusData.getTeamOf(uuid);
            String teamColor = team != null ? team.getName() : "§7";
            Bukkit.broadcastMessage("§f   " + (i + 1) + ". " + teamColor + " §e" + name
                    + " §7- §f" + String.format("%.0f", dmg) + " dégâts");
        }

        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
    }

    public NexusData getNexusData() { return nexusData; }
}