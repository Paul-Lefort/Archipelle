package fr.archipel.archiEvent.commands;

import fr.archipel.archiEvent.EventData;
import fr.archipel.archiEvent.EventData.RewardType; // Import de l'Enum
import fr.archipel.archiEvent.games.quiz.QuizLogic;
import fr.archipel.archiEvent.manager.MenuManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
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
    private final QuizLogic quizLogic;

    public ArchiEventCommand(EventData eventData) {
        this.eventData = eventData;
        // INITIALISATION CRUCIALE :
        this.quizLogic = new QuizLogic(eventData);
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
            QuizLogic quizLogic = new QuizLogic(eventData);
            quizLogic.quizStart();
        }
        else if (eventData.getEventType().contains("Spleef")) {
            player.sendMessage("§f§l[!] §fL'événement Spleef est configuré.");
            // Plus tard : SpleefLogic spleefLogic = new SpleefLogic(eventData);
        }
    }



    private void handleStop(Player player) {
        Map<String, Integer> scores = eventData.getGlobalScores();

        if (scores.isEmpty()) {
            player.sendMessage("§c§l[!] §7Aucun point marqué. Fermeture.");
            eventData.setEventType(null);
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

            // DISTRIBUTION VIA ENUM
            for (RewardType type : RewardType.values()) {
                int amount = eventData.getReward(rank, type);
                if (amount > 0) {
                    String cmd = "excellentcrates give " + playerName + " " + type.getCommandName() + " " + amount;
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
            }
        }
        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");

        // Reset
        eventData.setEventType(null);
        eventData.reset();
    }

    private void handleCancel(Player player) {
        if (eventData.getEventType() == null) {
            player.sendMessage("§c§l[!] §7Rien à annuler.");
            return;
        }
        eventData.setEventType(null);
        eventData.setAnswer(null);
        eventData.reset();
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