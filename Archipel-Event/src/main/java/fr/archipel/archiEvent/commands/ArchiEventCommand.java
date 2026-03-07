package fr.archipel.archiEvent.commands;

import fr.archipel.archiEvent.ArchiEvent;
import fr.archipel.archiEvent.EventData;
import fr.archipel.archiEvent.games.Game;
import fr.archipel.archiEvent.games.nexus.NexusData;
import fr.archipel.archiEvent.games.nexus.NexusGame;
import fr.archipel.archiEvent.games.nexus.NexusListener;
import fr.archipel.archiEvent.games.quiz.QuizChatListener;
import fr.archipel.archiEvent.games.quiz.QuizData;
import fr.archipel.archiEvent.games.quiz.QuizLogic;
import fr.archipel.archiEvent.manager.MenuManager;
import fr.archipel.archiEvent.manager.RewardManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ArchiEventCommand implements CommandExecutor {

    private final ArchiEvent plugin;
    private final EventData eventData;
    private final QuizData quizData;
    private final NexusData nexusData;
    private final QuizLogic quizLogic;
    private final NexusGame nexusGame;
    private final MenuManager menuManager;
    private final RewardManager rewardManager;

    private Game currentGame;
    private QuizChatListener quizChatListener;
    private NexusListener nexusListener;

    public ArchiEventCommand(ArchiEvent plugin, EventData eventData, QuizData quizData, NexusData nexusData) {
        this.plugin = plugin;
        this.eventData = eventData;
        this.quizData = quizData;
        this.nexusData = nexusData;
        this.quizLogic = new QuizLogic(eventData, quizData);
        this.nexusGame = new NexusGame(eventData, nexusData);
        this.menuManager = new MenuManager();
        this.rewardManager = new RewardManager(eventData);

        this.nexusGame.setOnGameEnd(() -> {
            nexusGame.broadcastDamageRanking();
            Map<String, Integer> ranking = nexusGame.getRanking();
            if (!ranking.isEmpty()) rewardManager.distributeRewards(ranking);
            cleanup();
        });
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
            case "create"    -> menuManager.openCreationMenu(player);
            case "open"      -> handleOpen(player);
            case "participe" -> handleParticipe(player);
            case "start"     -> handleStart(player);
            case "question"  -> quizLogic.handleQuestion(player, args);
            case "setnexus"  -> handleSetNexus(player, args);
            case "stop"      -> handleStop(player);
            case "cancel"    -> handleCancel(player);
            default          -> sendHelp(player);
        }
        return true;
    }

    // Ouvre les inscriptions — staff only
    private void handleOpen(Player player) {
        if (eventData.getEventType() == null) {
            player.sendMessage("§c§l[!] §7Configure l'évent avec /archievent create d'abord.");
            return;
        }
        if (eventData.isRegistrationOpen()) {
            player.sendMessage("§c§l[!] §7Les inscriptions sont déjà ouvertes.");
            return;
        }

        eventData.openRegistration();

        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
        Bukkit.broadcastMessage("§e§l   INSCRIPTIONS OUVERTES !");
        Bukkit.broadcastMessage("§f   Événement : " + eventData.getEventType());
        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage("§f   Tape §a/archievent participe §fpour rejoindre !");
        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
        Bukkit.broadcastMessage(" ");
    }

    // Inscription d'un joueur — accessible à tous
    private void handleParticipe(Player player) {
        if (!eventData.isRegistrationOpen()) {
            player.sendMessage("§c§l[!] §7Les inscriptions ne sont pas ouvertes.");
            return;
        }

        boolean added = eventData.register(player.getUniqueId());

        if (added) {
            player.sendMessage("§a§l[ArchiEvent] §fTu es inscrit(e) ! ("
                    + eventData.getRegisteredCount() + " participants)");
            Bukkit.broadcastMessage("§a[ArchiEvent] §e" + player.getName()
                    + " §fs'est inscrit(e) ! §7(" + eventData.getRegisteredCount() + " participants)");
        } else {
            player.sendMessage("§c§l[!] §7Tu es déjà inscrit(e) !");
        }
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
            currentGame = quizLogic;
            quizLogic.quizStart();

        } else if (eventData.getEventType().contains("Nexus")) {
            if (!nexusData.areBothLocationsSet()) {
                player.sendMessage("§c§l[!] §7Tu dois d'abord définir les deux Nexus :");
                player.sendMessage("§e/archievent setnexus rouge §7- spawn à ta position");
                player.sendMessage("§e/archievent setnexus bleu  §7- spawn à ta position");
                return;
            }
            if (eventData.getRegisteredCount() < 2) {
                player.sendMessage("§c§l[!] §7Il faut au moins 2 joueurs inscrits pour démarrer.");
                return;
            }

            // Fermer les inscriptions au démarrage
            eventData.closeRegistration();

            if (nexusListener == null) {
                nexusListener = new NexusListener(nexusData, nexusGame);
                Bukkit.getPluginManager().registerEvents(nexusListener, plugin);
            }
            currentGame = nexusGame;
            nexusGame.start();
        }
    }

    private void handleSetNexus(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /archievent setnexus <rouge|bleu>");
            return;
        }

        Location loc = player.getLocation();

        switch (args[1].toLowerCase()) {
            case "rouge" -> {
                nexusData.setPendingRedLocation(loc);
                player.sendMessage("§c§l[ArchiEvent] §fNexus Rouge positionné en "
                        + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
            }
            case "bleu" -> {
                nexusData.setPendingBlueLocation(loc);
                player.sendMessage("§9§l[ArchiEvent] §fNexus Bleu positionné en "
                        + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
            }
            default -> player.sendMessage("§cUsage: /archievent setnexus <rouge|bleu>");
        }
    }

    private void handleStop(Player player) {
        if (currentGame == null) {
            player.sendMessage("§c§l[!] §7Aucun événement en cours.");
            return;
        }

        if (currentGame instanceof NexusGame) {
            nexusGame.broadcastDamageRanking();
        }

        Map<String, Integer> ranking = currentGame.getRanking();

        if (ranking.isEmpty()) {
            player.sendMessage("§c§l[!] §7Aucun résultat. Fermeture sans récompenses.");
            cleanup();
            return;
        }

        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
        Bukkit.broadcastMessage("§e§l   CLASSEMENT FINAL : §f" + eventData.getEventType());

        ranking.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(entry -> {
                    String suffix = (entry.getValue() == 1) ? "er" : "ème";
                    Bukkit.broadcastMessage("§f    " + entry.getValue() + suffix + " §e" + entry.getKey());
                });

        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");

        rewardManager.distributeRewards(ranking);
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
            quizChatListener = null;
        }
        if (nexusListener != null) {
            HandlerList.unregisterAll(nexusListener);
            nexusListener = null;
        }
        nexusGame.clearHolograms();
        nexusGame.clearScoreboard();
        currentGame = null;
        eventData.reset();
        quizData.reset();
        nexusData.reset();
    }

    private void sendHelp(Player player) {
        player.sendMessage("§8§m      §r §6§lArchiEvent §8§m      ");
        player.sendMessage("§e/archievent create                §7- Configurer l'évent");
        player.sendMessage("§e/archievent open                  §7- Ouvrir les inscriptions");
        player.sendMessage("§e/archievent participe             §7- S'inscrire à l'évent");
        player.sendMessage("§e/archievent start                 §7- Lancer l'évent");
        player.sendMessage("§e/archievent setnexus <rouge|bleu> §7- Poser un Nexus");
        player.sendMessage("§e/archievent stop                  §7- Finir et donner les lots");
        player.sendMessage("§e/archievent cancel                §7- Tout stopper sans lots");
    }
}