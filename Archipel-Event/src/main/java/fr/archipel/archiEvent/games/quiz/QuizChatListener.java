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

    public QuizChatListener(EventData eventData) {
        this.eventData = eventData;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {

        if (eventData.getAnswer() == null) return;

        Player player = event.getPlayer();
        String message = event.getMessage();

        if (message.equalsIgnoreCase(eventData.getAnswer())) {

            if (eventData.getCurrentQuestionWinners().contains(player)) {
                event.setCancelled(true);
                player.sendMessage("§c§l[!] §7Tu as déjà trouvé la réponse pour cette question !");
                return;
            }

            event.setCancelled(true);

            eventData.getCurrentQuestionWinners().add(player);

            int position = eventData.getCurrentQuestionWinners().size();
            int points = (position == 1) ? 5 : (position == 2) ? 3 : (position == 3) ? 2 : 0;

            if (points > 0) {
                int finalPoints = points;

                // On repasse sur le thread principal pour les messages et sons
                Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("ArchiEvent"), () -> {

                    eventData.addPoints(player.getName(), finalPoints);

                    String suffix = (position == 1) ? "er" : "ème";
                    Bukkit.broadcastMessage("§a§l[ArchiEvent] §e" + player.getName() + " §f- " + position + suffix + " ! §7(+ " + finalPoints + " pts)");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

                    if (eventData.getCurrentQuestionWinners().size() >= 3) {

                        eventData.setAnswer(null);

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