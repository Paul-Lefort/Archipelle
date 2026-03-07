package fr.archipel.archiEvent.games.dac;

import fr.archipel.archiEvent.EventData;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener; // Ne pas oublier cet import !
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.List;

public class DACPlayerListener implements Listener { // Doit implémenter Listener

    private final EventData eventData;
    private final DACLogic dacLogic;

    // Le constructeur permet de lier le Listener à ta logique de jeu
    public DACPlayerListener(EventData eventData, DACLogic dacLogic) {
        this.eventData = eventData;
        this.dacLogic = dacLogic;
    }

    @EventHandler
    public void onJump(PlayerMoveEvent event) {
        Player p = event.getPlayer();

        // On vérifie si c'est le tour de ce joueur via eventData
        if (eventData.getCurrentDacPlayer() == null || !p.getUniqueId().equals(eventData.getCurrentDacPlayer().getUniqueId())) {
            return;
        }

        // On récupère la liste des participants depuis dacLogic
        List<Player> participants = dacLogic.getParticipants();

        // --- CAS 1 : SUCCÈS (Touche l'eau) ---
        if (p.getLocation().getBlock().getType() == Material.WATER) {
            p.getLocation().getBlock().setType(Material.WHITE_WOOL);

            p.setGameMode(GameMode.SPECTATOR);
            p.sendMessage("§a§l✔ §fRéussi !");

            // On le remet à la fin de la file
            participants.remove(p);
            participants.add(p);

            // On appelle la méthode via dacLogic
            dacLogic.nextTurn();
        }

        // --- CAS 2 : ÉCHEC (Touche le sol/bloc) ---
        else if (p.isOnGround()) {
            // Petite sécurité : on vérifie qu'il n'est pas juste sur la plateforme de départ
            if (p.getLocation().getY() < eventData.getDacJumpPoint().getY() - 2) {
                p.setGameMode(GameMode.SPECTATOR);
                p.sendMessage("§c§l✘ §fÉliminé !");

                participants.remove(p);
                dacLogic.nextTurn();
            }
        }
    }
}