package fr.archipel.archiEvent.games.quiz;

import fr.archipel.archiEvent.EventData;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class QuizChatListener implements Listener {

    private final EventData eventData;
    private final QuizData quizData;

    public QuizChatListener(EventData eventData, QuizData quizData) {
        this.eventData = eventData;
        this.quizData = quizData;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (quizData.getAnswer() == null) return;
        if (quizData.getCurrentQuestionWinners().size() >= 3) return;

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (quizData.getCurrentQuestionWinners().contains(playerId)) {
            if (event.getMessage().equalsIgnoreCase(quizData.getAnswer())) {
                event.setCancelled(true);
                player.sendMessage("§c§l[!] §7Tu as déjà trouvé la réponse !");
            }
            return;
        }

        if (!event.getMessage().equalsIgnoreCase(quizData.getAnswer())) return;

        event.setCancelled(true);

        quizData.getCurrentQuestionWinners().add(playerId);
        int position = quizData.getCurrentQuestionWinners().size();
        int points = (position == 1) ? 5 : (position == 2) ? 3 : 2;
        int finalPoints = points;

        Plugin plugin = Bukkit.getPluginManager().getPlugin("ArchiEvent");
        if (plugin == null) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            quizData.addPoints(player.getName(), finalPoints);

            String suffix = (position == 1) ? "er" : "ème";
            Bukkit.broadcastMessage("§a§l[ArchiEvent] §e" + player.getName() + " §f- " + position + suffix + " ! §7(+ " + finalPoints + " pts)");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

            if (quizData.getCurrentQuestionWinners().size() >= 3) {
                quizData.setAnswer(null);
                Bukkit.broadcastMessage("§6§l[ArchiEvent] §fPodium complet !");

                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.playSound(online.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1f, 1.2f);
                }
            }
        });
    }
}