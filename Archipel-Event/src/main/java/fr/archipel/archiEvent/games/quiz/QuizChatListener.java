package fr.archipel.archiEvent.games.quiz;

import fr.archipel.archiEvent.EventData;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class QuizChatListener implements Listener {

    private final EventData eventData;
    private final QuizData quizData; // Ajout du conteneur spécifique

    public QuizChatListener(EventData eventData, QuizData quizData) {
        this.eventData = eventData;
        this.quizData = quizData; // AVANT : this.quizData = this.quizData; (Erreur !)
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        // On vérifie la réponse dans QuizData
        if (quizData.getAnswer() == null) return;

        Player player = event.getPlayer();
        String message = event.getMessage();

        // Comparaison avec la réponse stockée dans QuizData
        if (message.equalsIgnoreCase(quizData.getAnswer())) {

            // Vérification si le joueur a déjà gagné ce round via QuizData
            if (quizData.getCurrentQuestionWinners().contains(player)) {
                event.setCancelled(true);
                player.sendMessage("§c§l[!] §7Tu as déjà trouvé la réponse pour cette question !");
                return;
            }

            event.setCancelled(true);

            // Ajout du gagnant dans QuizData
            quizData.getCurrentQuestionWinners().add(player);

            int position = quizData.getCurrentQuestionWinners().size();
            int points = (position == 1) ? 5 : (position == 2) ? 3 : (position == 3) ? 2 : 0;

            if (points > 0) {
                int finalPoints = points;

                // On repasse sur le thread principal pour Bukkit
                Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("ArchiEvent"), () -> {

                    // Ajout des points globaux dans QuizData
                    quizData.addPoints(player.getName(), finalPoints);

                    String suffix = (position == 1) ? "er" : "ème";
                    Bukkit.broadcastMessage("§a§l[ArchiEvent] §e" + player.getName() + " §f- " + position + suffix + " ! §7(+ " + finalPoints + " pts)");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

                    // Si le podium (3 places) est complet
                    if (quizData.getCurrentQuestionWinners().size() >= 3) {

                        // On ferme la question dans QuizData
                        quizData.setAnswer(null);

                        Bukkit.broadcastMessage("§6§l[ArchiEvent] §fPodium complet ! Fin de la question.");

                        for (Player online : Bukkit.getOnlinePlayers()) {
                            online.playSound(online.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1f, 1.2f);
                        }
                    }
                });
            }
        }
    }
}