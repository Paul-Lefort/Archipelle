package fr.archipel.archiEvent.commands;

import fr.archipel.archiEvent.EventData;
import fr.archipel.archiEvent.EventData.RewardType;
import fr.archipel.archiEvent.games.quiz.QuizData;
import fr.archipel.archiEvent.games.quiz.QuizLogic;
import fr.archipel.archiEvent.manager.MenuManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArchiEventCommand implements CommandExecutor {

    private final EventData eventData;
    private final QuizData quizData; // On stocke l'instance pour les scores
    private final QuizLogic quizLogic;

    public ArchiEventCommand(EventData eventData, QuizData quizData) {
        this.eventData = eventData;
        this.quizData = quizData;
        // On passe les deux instances à QuizLogic comme prévu
        this.quizLogic = new QuizLogic(eventData, quizData);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSeul un joueur peut executer cette commande.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                handleCreate(player);
                break;
            case "start":
                handleStart(player);
                break;
            case "question":
                quizLogic.handleQuestion(player, args);
                break;
            case "stop":
                handleStop(player);
                break;
            case "cancel":
                handleCancel(player);
                break;
            default:
                sendHelp(player);
                break;
        }
        return true;
    }

    private void handleCreate(Player player) {
        MenuManager menuManager = new MenuManager();
        menuManager.openCreationMenu(player);
    }

    private void handleStart(Player player) {
        if (eventData.getEventType() == null) {
            player.sendMessage("§c§l[!] §7Configure l'évent avec /archievent create.");
            return;
        }

        if (eventData.getEventType().contains("Quiz")) {
            // Utilisation de l'instance déjà créée au lieu d'en recréer une
            quizLogic.quizStart();
        } else if (eventData.getEventType().contains("Spleef")) {
            player.sendMessage("§f§l[!] §fL'événement Spleef est configuré.");
        }
    }

    private void handleStop(Player player) {
        // RÉCUPÉRATION DES SCORES VIA QUIZDATA
        Map<String, Integer> scores = quizData.getGlobalScores();

        if (scores.isEmpty()) {
            player.sendMessage("§c§l[!] §7Aucun point marqué. Fermeture.");
            eventData.setEventType(null);
            quizData.reset(); // Reset du quiz
            return;
        }

        List<Map.Entry<String, Integer>> ranking = new ArrayList<>(scores.entrySet());
        ranking.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
        Bukkit.broadcastMessage("§e§l   CLASSEMENT FINAL : §f" + eventData.getEventType());

        for (int i = 0; i < Math.min(3, ranking.size()); i++) {
            String playerName = ranking.get(i).getKey();
            int points = ranking.get(i).getValue();
            int rank = i + 1;

            Bukkit.broadcastMessage("§f    " + rank + ". §e" + playerName + " §7- §f" + points + " pts");

            for (RewardType type : RewardType.values()) {
                int amount = eventData.getReward(rank, type);
                if (amount > 0) {
                    String cmd = "excellentcrates give " + playerName + " " + type.getCommandName() + " " + amount;
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
            }
        }
        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");

        // Reset global
        eventData.setEventType(null);
        eventData.reset();
        quizData.reset(); // On n'oublie pas de reset le quiz
    }

    private void handleCancel(Player player) {
        if (eventData.getEventType() == null) {
            player.sendMessage("§c§l[!] §7Rien à annuler.");
            return;
        }
        eventData.setEventType(null);
        eventData.reset();
        quizData.reset(); // Reset la question, la réponse et les scores

        Bukkit.broadcastMessage("§c§l[ArchiEvent] §fL'événement a été §nannulé§f.");
    }

    private void sendHelp(Player player) {
        player.sendMessage("§8§m      §r §6§lArchiEvent §8§m      ");
        player.sendMessage("§e/archievent create §7- Configurer l'évent");
        player.sendMessage("§e/archievent start  §7- Lancer l'annonce");
        player.sendMessage("§e/archievent stop   §7- Finir et donner les lots");
        player.sendMessage("§e/archievent cancel §7- Tout stopper sans lots");
    }
}