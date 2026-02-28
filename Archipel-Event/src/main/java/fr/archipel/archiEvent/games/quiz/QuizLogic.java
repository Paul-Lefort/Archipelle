package fr.archipel.archiEvent.games.quiz;

import fr.archipel.archiEvent.EventData;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import fr.archipel.archiEvent.ArchiEvent;

import java.util.ArrayList;
import java.util.List;

public class QuizLogic {

    private final EventData eventData;

    // Le constructeur doit porter le nom de la classe (QuizLogic)
    public QuizLogic(EventData eventData) {
        this.eventData = eventData;
    }

    public void quizStart() {
        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
        // On nettoie le nom pour l'affichage (enlève les §e§l)
        String cleanName = org.bukkit.ChatColor.stripColor(eventData.getEventType());
        Bukkit.broadcastMessage("§e§l  Début de l'événement : §6§n" + cleanName.toUpperCase());
        Bukkit.broadcastMessage("§e ");
        Bukkit.broadcastMessage("§f    Les récompenses seront les suivantes :");
        Bukkit.broadcastMessage("§7    1er - " + formatRewards(1));
        Bukkit.broadcastMessage("§7    2ème - " + formatRewards(2));
        Bukkit.broadcastMessage("§7    3ème - " + formatRewards(3));
        Bukkit.broadcastMessage("§6§l§m--------------------------------------------");
        Bukkit.broadcastMessage(" ");

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.playSound(online.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
    }

    private String formatRewards(int place) {
        List<String> parts = new ArrayList<>();
        // On boucle sur ton Enum RewardType
        for (EventData.RewardType type : EventData.RewardType.values()) {
            int amount = eventData.getReward(place, type);
            if (amount > 0) {
                parts.add("§e" + amount + "x " + type.getDisplayName());
            }
        }
        return parts.isEmpty() ? "§8Aucune" : String.join("§f, ", parts);
    }
    public void handleQuestion(Player player, String[] args) {

        // 1. Vérification du type d'event
        if (eventData.getEventType() == null || !eventData.getEventType().contains("Quiz")) {
            player.sendMessage("§c§l[!] §7Tu dois d'abord créer un événement Quiz.");
            return;
        }

        // 2. Vérification des arguments (min: /ae question <Q> <R>)
        if (args.length < 3) {
            player.sendMessage("§cUsage: /archievent question <Question...> <Réponse>");
            return;
        }

        // 3. Extraction de la question et de la réponse
        // On considère que le dernier mot est la réponse
        String answer = args[args.length - 1];
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length - 1; i++) {
            sb.append(args[i]).append(" ");
        }
        String question = sb.toString().trim();

        // 4. Initialisation des données
        eventData.clearQuestionWinners();
        eventData.setQuestion(question);
        eventData.setAnswer(answer);

        // 5. Annonce
        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
        Bukkit.broadcastMessage("§6§l    QUESTION :");
        Bukkit.broadcastMessage("§f    " + question);
        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");

        // 6. Lancement du Timer de 30 secondes
        // On récupère l'instance du plugin via Bukkit pour le scheduler
        JavaPlugin plugin = JavaPlugin.getPlugin(ArchiEvent.class);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            // Si le podium a été complété entre temps, le ChatListener aura mis l'answer à null
            if (eventData.getAnswer() == null) return;

            if (eventData.getCurrentQuestionWinners().isEmpty()) {
                Bukkit.broadcastMessage("§c§l[ArchiEvent] §7Temps écoulé ! Personne n'a trouvé.");
                Bukkit.broadcastMessage("§7La réponse était : §f" + answer);
            } else {
                Bukkit.broadcastMessage("§6§l[ArchiEvent] §7Fin du temps !");
            }

            // Fermeture de la question
            eventData.setAnswer(null);
            eventData.clearQuestionWinners();

        }, 20L * 30);
    }
}