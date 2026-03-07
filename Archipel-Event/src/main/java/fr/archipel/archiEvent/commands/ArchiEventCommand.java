package fr.archipel.archiEvent.commands;

import fr.archipel.archiEvent.ArchiEvent; // Assure-toi d'importer ta classe principale
import fr.archipel.archiEvent.EventData;
import fr.archipel.archiEvent.EventData.RewardType;
import fr.archipel.archiEvent.games.quiz.QuizChatListener;
import fr.archipel.archiEvent.games.quiz.QuizData;
import fr.archipel.archiEvent.games.quiz.QuizLogic;
import fr.archipel.archiEvent.manager.MenuManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArchiEventCommand implements CommandExecutor {

    private final ArchiEvent plugin; // Nécessaire pour registerEvents
    private final EventData eventData;
    private final QuizData quizData;
    private final QuizLogic quizLogic;

    // On stocke le listener ici pour pouvoir l'unregister plus tard
    private QuizChatListener quizChatListener;

    public ArchiEventCommand(ArchiEvent plugin, EventData eventData, QuizData quizData) {
        this.plugin = plugin;
        this.eventData = eventData;
        this.quizData = quizData;
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
            case "create" -> handleCreate(player);
            case "start" -> handleStart(player);
            case "question" -> quizLogic.handleQuestion(player, args);
            case "stop" -> handleStop(player);
            case "cancel" -> handleCancel(player);
            default -> sendHelp(player);
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
            if (quizChatListener == null) {
                quizChatListener = new QuizChatListener(eventData, quizData);
                Bukkit.getPluginManager().registerEvents(quizChatListener, plugin);
            }
            quizLogic.quizStart();
        } else if (eventData.getEventType().contains("Spleef")) {
            player.sendMessage("§f§l[!] §fL'événement Spleef est configuré.");
        }
    }

    private void handleStop(Player player) {
        Map<String, Integer> scores = quizData.getGlobalScores();

        if (scores.isEmpty()) {
            player.sendMessage("§c§l[!] §7Aucun point marqué. Fermeture.");
            cleanup();
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

        cleanup();
    }

    private void handleCancel(Player player) {
        if (eventData.getEventType() == null) {
            player.sendMessage("§c§l[!] §7Rien à annuler.");
            return;
        }
        cleanup();
        Bukkit.broadcastMessage("§c§l[ArchiEvent] §fL'événement a été §nannulé§f.");
    }


    private void cleanup() {
        if (quizChatListener != null) {
            HandlerList.unregisterAll(quizChatListener);
            quizChatListener = null; // Important pour pouvoir le recréer au prochain start
        }

        eventData.setEventType(null);
        eventData.reset();
        quizData.reset();
    }

    private void sendHelp(Player player) {
        player.sendMessage("§8§m      §r §6§lArchiEvent §8§m      ");
        player.sendMessage("§e/archievent create §7- Configurer l'évent");
        player.sendMessage("§e/archievent start  §7- Lancer l'annonce");
        player.sendMessage("§e/archievent stop   §7- Finir et donner les lots");
        player.sendMessage("§e/archievent cancel §7- Tout stopper sans lots");
    }
}