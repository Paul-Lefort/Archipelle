package fr.archipel.archiEvent.games.quiz;

import fr.archipel.archiEvent.EventData;
import fr.archipel.archiEvent.ArchiEvent;
import fr.archipel.archiEvent.games.Game;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizLogic implements Game {

    private final EventData eventData;
    private final QuizData quizData;

    public QuizLogic(EventData eventData, QuizData quizData) {
        this.eventData = eventData;
        this.quizData = quizData;
    }

    /**
     * Retourne le classement final : trie les scores et retourne Map<NomJoueur, Place>.
     * Appelé par /stop via ArchiEventCommand.
     */
    @Override
    public Map<String, String> getDisplayScores() {
        Map<String, String> display = new HashMap<>();
        for (Map.Entry<String, Integer> entry : quizData.getGlobalScores().entrySet()) {
            display.put(entry.getKey(), entry.getValue() + " pts");
        }
        return display;
    }

    @Override
    public Map<String, Integer> getRanking() {
        Map<String, Integer> scores = quizData.getGlobalScores();
        Map<String, Integer> ranking = new HashMap<>();

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(scores.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        for (int i = 0; i < Math.min(3, sorted.size()); i++) {
            ranking.put(sorted.get(i).getKey(), i + 1);
        }

        return ranking;
    }

    public void quizStart() {
        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");

        String eventName = eventData.getEventType() != null ? eventData.getEventType() : "Quiz";
        String cleanName = ChatColor.stripColor(eventName);

        Bukkit.broadcastMessage("§e§l  Début de l'événement : §6§n" + cleanName.toUpperCase());
        Bukkit.broadcastMessage("§e ");
        Bukkit.broadcastMessage("§f    Les récompenses seront les suivantes :");
        Bukkit.broadcastMessage("§7    1er - " + formatRewards(1));
        Bukkit.broadcastMessage("§7    2ème - " + formatRewards(2));
        Bukkit.broadcastMessage("§7    3ème - " + formatRewards(3));
        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
        Bukkit.broadcastMessage(" ");

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.playSound(online.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
    }

    public void handleQuestion(Player player, String[] args) {
        if (eventData.getEventType() == null || !eventData.getEventType().contains("Quiz")) {
            player.sendMessage("§c§l[!] §7Tu dois d'abord créer un événement Quiz.");
            return;
        }

        if (args.length < 3) {
            player.sendMessage("§cUsage: /archievent question <Question...> <Réponse>");
            return;
        }

        String answer = args[args.length - 1];
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length - 1; i++) {
            sb.append(args[i]).append(" ");
        }
        String question = sb.toString().trim();

        quizData.clearQuestionWinners();
        quizData.setQuestion(question);
        quizData.setAnswer(answer);

        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
        Bukkit.broadcastMessage("§6§l    QUESTION :");
        Bukkit.broadcastMessage("§f    " + question);
        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");

        JavaPlugin plugin = JavaPlugin.getPlugin(ArchiEvent.class);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (quizData.getAnswer() == null) return;

            if (quizData.getCurrentQuestionWinners().isEmpty()) {
                Bukkit.broadcastMessage("§c§l[ArchiEvent] §7Temps écoulé ! Personne n'a trouvé.");
                Bukkit.broadcastMessage("§7La réponse était : §f" + quizData.getAnswer());
            } else {
                Bukkit.broadcastMessage("§6§l[ArchiEvent] §7Fin du temps !");
            }

            quizData.setAnswer(null);
            quizData.clearQuestionWinners();

        }, 20L * 30);
    }

    private String formatRewards(int place) {
        List<String> parts = new ArrayList<>();
        for (EventData.RewardType type : EventData.RewardType.values()) {
            int amount = eventData.getReward(place, type);
            if (amount > 0) {
                parts.add("§e" + amount + "x " + type.getDisplayName());
            }
        }
        return parts.isEmpty() ? "§8Aucune" : String.join("§f, ", parts);
    }
}