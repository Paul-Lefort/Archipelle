package fr.archipel.archiEvent.games.dac;

import fr.archipel.archiEvent.EventData;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DACLogic {

    private final EventData eventData;
    private final List<Player> participants = new ArrayList<>();
    private boolean registrationsOpen = false;

    public DACLogic(EventData eventData) {
        this.eventData = eventData;
    }

    // --- PHASE 1 : ANNONCE ET INSCRIPTIONS ---
    public void dacStart() {
        if (eventData.getEventType() == null || !eventData.getEventType().contains("Dès")) {
            return;
        }

        registrationsOpen = true;
        participants.clear();

        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
        Bukkit.broadcastMessage("§b§l    Événement Dès À Coudre ");
        Bukkit.broadcastMessage("§f    Afin de pouvoir y participer, veuillez vous inscrire");
        Bukkit.broadcastMessage("§7    Utilisez la commande : §e/archievent participe");
        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
    }

    public void addPlayer(Player player) {
        if (!registrationsOpen) {
            player.sendMessage("§c§l[!] §7Les inscriptions pour le DAC ne sont pas ouvertes.");
            return;
        }

        if (participants.contains(player)) {
            player.sendMessage("§c§l[!] §7Tu es déjà inscrit à l'événement !");
            return;
        }

        participants.add(player);
        player.sendMessage("§a§l[!] §fInscription confirmée ! (§e" + participants.size() + " §fjoueurs)");
    }

    // --- PHASE 2 : LANCEMENT (TRI ET PREPARATION) ---
    public void launch(Player player) {
        // Nettoyage des joueurs déconnectés
        participants.removeIf(p -> !p.isOnline());

        if (participants.size() < 1) {
            player.sendMessage("§c§l[!] §7Il n'y a pas assez de participants.");
            return;
        }

        registrationsOpen = false; // On ferme les inscriptions
        Collections.shuffle(participants); // On mélange l'ordre de passage

        // On synchronise l'ordre avec EventData
        eventData.setDacPlayerOrder(new ArrayList<>(participants));

        Bukkit.broadcastMessage("§b§l[DAC] §fLa partie commence ! Tirage au sort effectué.");

        // On met tout le monde en spectateur au début
        for (Player p : participants) {
            p.setGameMode(GameMode.SPECTATOR);
            // On peut les TP à une zone d'attente ici si nécessaire
        }

        // On lance le premier tour
        nextTurn();
    }

    public void nextTurn() {
        if (participants.isEmpty()) {
            finishGame();
            return;
        }

        // Vérification si le bassin est plein avant de continuer
        if (isPoolFull()) {
            Bukkit.broadcastMessage("§b§l[DAC] §fLe bassin est plein ! Fin de la partie.");
            finishGame();
            return;
        }

        // On prend le premier de la liste (celui dont c'est le tour)
        Player p = participants.get(0);
        eventData.setCurrentDacPlayer(p);

        p.setGameMode(GameMode.SURVIVAL);

        if (eventData.getDacJumpPoint() != null) {
            p.teleport(eventData.getDacJumpPoint());
        } else {
            Bukkit.broadcastMessage("§c§l[!] §7Erreur : Le point de saut n'est pas configuré !");
            return;
        }

        Bukkit.broadcastMessage("§b§l[DAC] §fC'est au tour de §e" + p.getName());
    }

    public boolean isPoolFull() {
        if (eventData.getPoolPos1() == null || eventData.getPoolPos2() == null) return false;

        Location p1 = eventData.getPoolPos1();
        Location p2 = eventData.getPoolPos2();

        for (int x = Math.min(p1.getBlockX(), p2.getBlockX()); x <= Math.max(p1.getBlockX(), p2.getBlockX()); x++) {
            for (int y = Math.min(p1.getBlockY(), p2.getBlockY()); y <= Math.max(p1.getBlockY(), p2.getBlockY()); y++) {
                for (int z = Math.min(p1.getBlockZ(), p2.getBlockZ()); z <= Math.max(p1.getBlockZ(), p2.getBlockZ()); z++) {
                    if (p1.getWorld().getBlockAt(x, y, z).getType() == Material.WATER) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void finishGame() {
        Bukkit.broadcastMessage("§b§l[DAC] §fL'événement est terminé !");
        // Ici tu pourras appeler ta logique de stop pour distribuer les prix
    }

    public List<Player> getParticipants() {
        return participants;
    }
}