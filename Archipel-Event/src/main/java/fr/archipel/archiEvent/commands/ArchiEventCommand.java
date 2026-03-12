package fr.archipel.archiEvent.commands;

import fr.archipel.archiEvent.ArchiEvent;
import fr.archipel.archiEvent.EventData;
import fr.archipel.archiEvent.games.Game;
import fr.archipel.archiEvent.games.dac.DACData;
import fr.archipel.archiEvent.games.dac.DACGame;
import fr.archipel.archiEvent.games.dac.DACListener;
import fr.archipel.archiEvent.games.spleef.SpleefData;
import fr.archipel.archiEvent.games.spleef.SpleefGame;
import fr.archipel.archiEvent.games.spleef.SpleefListener;
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
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ArchiEventCommand implements CommandExecutor {

    private final ArchiEvent plugin;
    private final EventData eventData;
    private final QuizData quizData;
    private final NexusData nexusData;
    private final DACData dacData;
    private final SpleefData spleefData;
    private final QuizLogic quizLogic;
    private final NexusGame nexusGame;
    private final DACGame dacGame;
    private final SpleefGame spleefGame;
    private final MenuManager menuManager;
    private final RewardManager rewardManager;

    private Game currentGame;
    private QuizChatListener quizChatListener;
    private NexusListener nexusListener;
    private DACListener dacListener;
    private SpleefListener spleefListener;
    private BukkitTask registrationReminderTask;

    public ArchiEventCommand(ArchiEvent plugin, EventData eventData, QuizData quizData, NexusData nexusData, DACData dacData, SpleefData spleefData) {
        this.plugin = plugin;
        this.eventData = eventData;
        this.quizData = quizData;
        this.nexusData = nexusData;
        this.dacData = dacData;
        this.spleefData = spleefData;
        this.quizLogic = new QuizLogic(eventData, quizData);
        this.nexusGame = new NexusGame(eventData, nexusData);
        this.dacGame = new DACGame(eventData, dacData);
        this.spleefGame = new SpleefGame(eventData, spleefData);
        this.menuManager = new MenuManager();
        this.rewardManager = new RewardManager(eventData);

        this.nexusGame.setOnGameEnd(() -> {
            nexusGame.broadcastDamageRanking();
            Map<String, Integer> ranking = nexusGame.getRanking();
            if (!ranking.isEmpty()) {
                broadcastRanking(ranking, nexusGame.getDisplayScores());
                rewardManager.distributeRewards(ranking);
            }
            cleanup();
        });

        this.dacGame.setOnGameEnd(() -> {
            Map<String, Integer> ranking = dacGame.getRanking();
            if (!ranking.isEmpty()) {
                broadcastRanking(ranking, dacGame.getDisplayScores());
                rewardManager.distributeRewards(ranking);
            }
            cleanup();
        });

        this.spleefGame.setOnGameEnd(() -> {
            Map<String, Integer> ranking = spleefGame.getRanking();
            if (!ranking.isEmpty()) {
                broadcastRanking(ranking, spleefGame.getDisplayScores());
                rewardManager.distributeRewards(ranking);
            }
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

        // Commandes staff
        boolean isStaffCommand = List.of("create","open","start","test","stop","cancel",
                        "setnexus","setarena","setpool","setjump","setspleefspawn","question")
                .contains(args[0].toLowerCase());

        if (isStaffCommand && !player.hasPermission("archievent.staff")) {
            player.sendMessage("§c§l[!] §7Tu n'as pas la permission d'utiliser cette commande.");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create"    -> menuManager.openCreationMenu(player);
            case "open"      -> handleOpen(player);
            case "participe" -> handleParticipe(player);
            case "quitter"   -> handleQuitter(player);
            case "start"     -> handleStart(player, false);
            case "test"      -> handleStart(player, true);
            case "question"  -> quizLogic.handleQuestion(player, args);
            case "setnexus"  -> handleSetNexus(player, args);
            case "setpool"   -> handleSetPool(player, args);
            case "setjump"   -> handleSetJump(player);
            case "setarena"  -> handleSetArena(player, args);
            case "stop"      -> handleStop(player);
            case "cancel"    -> handleCancel(player);
            default          -> sendHelp(player);
        }
        return true;
    }

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

        // Reminder toutes les 5 minutes tant que les inscriptions sont ouvertes
        long fiveMinutes = 20L * 60 * 5;
        registrationReminderTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!eventData.isRegistrationOpen()) {
                registrationReminderTask.cancel();
                return;
            }
            Bukkit.broadcastMessage("§6§l[ArchiEvent] §f" + eventData.getRegisteredCount()
                    + " joueur(s) inscrit(s) pour §e" + eventData.getEventType()
                    + "§f ! Tape §a/archievent participe §fpour rejoindre !");
        }, fiveMinutes, fiveMinutes);
    }

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

    private void handleQuitter(Player player) {
        if (!eventData.isRegistrationOpen()) {
            player.sendMessage("§c§l[!] §7Les inscriptions ne sont pas ouvertes.");
            return;
        }

        boolean removed = eventData.unregister(player.getUniqueId());

        if (removed) {
            player.sendMessage("§c§l[ArchiEvent] §fTu t'es désinscrit(e). ("
                    + eventData.getRegisteredCount() + " participants restants)");
            Bukkit.broadcastMessage("§c[ArchiEvent] §e" + player.getName()
                    + " §fs'est désinscrit(e). §7(" + eventData.getRegisteredCount() + " participants)");
        } else {
            player.sendMessage("§c§l[!] §7Tu n'es pas inscrit(e).");
        }
    }

    private void handleStart(Player player, boolean testMode) {
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
            announceStart();
            quizLogic.quizStart();

        } else if (eventData.getEventType().contains("Spleef")) {
            if (!spleefData.isArenaDefined()) {
                player.sendMessage("§c§l[!] §7Définis le centre de l'arène avec §e/archievent setarena");
                return;
            }
            if (!testMode && eventData.getRegisteredCount() < 2) {
                player.sendMessage("§c§l[!] §7Il faut au moins 2 joueurs inscrits pour démarrer.");
                return;
            }

            List<UUID> players = new ArrayList<>(eventData.getRegisteredPlayers());
            if (testMode) {
                if (!players.contains(player.getUniqueId())) players.add(player.getUniqueId());
                player.sendMessage("§e§l[TEST] §7Mode test activé.");
            }
            eventData.closeRegistration();

            if (spleefListener == null) {
                spleefListener = new SpleefListener(spleefData, spleefGame);
                Bukkit.getPluginManager().registerEvents(spleefListener, plugin);
            }
            currentGame = spleefGame;
            announceStart();
            spleefGame.start(players);

        } else if (eventData.getEventType().contains("Dès") || eventData.getEventType().contains("DAC")) {
            if (!dacData.isPoolDefined()) {
                player.sendMessage("§c§l[!] §7Définis la zone de plongeon d'abord :");
                player.sendMessage("§e/archievent setpool 1 §7- premier coin");
                player.sendMessage("§e/archievent setpool 2 §7- deuxième coin");
                return;
            }
            if (!dacData.isJumpDefined()) {
                player.sendMessage("§c§l[!] §7Définis le point de saut avec §e/archievent setjump");
                return;
            }
            if (!testMode && eventData.getRegisteredCount() < 2) {
                player.sendMessage("§c§l[!] §7Il faut au moins 2 joueurs inscrits pour démarrer.");
                return;
            }

            List<UUID> players = new ArrayList<>(eventData.getRegisteredPlayers());
            if (testMode) {
                if (!players.contains(player.getUniqueId())) players.add(player.getUniqueId());
                player.sendMessage("§e§l[TEST] §7Mode test activé.");
            }
            Collections.shuffle(players);
            eventData.closeRegistration();

            if (dacListener == null) {
                dacListener = new DACListener(dacData, dacGame);
                Bukkit.getPluginManager().registerEvents(dacListener, plugin);
                dacGame.setDACListener(dacListener);
            }
            currentGame = dacGame;
            announceStart();
            dacGame.start(players);

        } else if (eventData.getEventType().contains("Nexus")) {
            if (!nexusData.areBothLocationsSet()) {
                player.sendMessage("§c§l[!] §7Tu dois d'abord définir les deux Nexus :");
                player.sendMessage("§e/archievent setnexus rouge §7- spawn à ta position");
                player.sendMessage("§e/archievent setnexus bleu  §7- spawn à ta position");
                return;
            }

            if (!testMode && eventData.getRegisteredCount() < 2) {
                player.sendMessage("§c§l[!] §7Il faut au moins 2 joueurs inscrits pour démarrer.");
                player.sendMessage("§7(utilise §e/archievent test §7pour ignorer cette vérification)");
                return;
            }

            if (testMode) {
                eventData.register(player.getUniqueId());
                player.sendMessage("§e§l[TEST] §7Mode test activé — inscriptions ignorées.");
            }

            eventData.closeRegistration();

            if (nexusListener == null) {
                nexusListener = new NexusListener(nexusData, nexusGame);
                Bukkit.getPluginManager().registerEvents(nexusListener, plugin);
            }
            currentGame = nexusGame;
            announceStart();
            nexusGame.start();
        }
    }

    private void handleSetNexus(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /archievent setnexus <rouge|bleu>");
            return;
        }

        Location loc = new Location(
                player.getWorld(),
                player.getLocation().getBlockX() + 0.5,
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ() + 0.5
        );

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

    private void handleSetPool(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /archievent setpool <1|2>");
            return;
        }
        Location loc = new Location(player.getWorld(),
                player.getLocation().getBlockX() + 0.5,
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ() + 0.5);

        switch (args[1]) {
            case "1" -> {
                dacData.setPoolPos1(loc);
                player.sendMessage("§b§l[DAC] §fCoin 1 positionné en "
                        + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
            }
            case "2" -> {
                dacData.setPoolPos2(loc);
                player.sendMessage("§b§l[DAC] §fCoin 2 positionné en "
                        + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
            }
            default -> player.sendMessage("§cUsage: /archievent setpool <1|2>");
        }
    }

    private void handleSetJump(Player player) {
        Location loc = player.getLocation();
        dacData.setJumpLocation(loc);
        player.sendMessage("§b§l[DAC] §fPoint de saut positionné en "
                + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
    }

    private void handleSetArena(Player player, String[] args) {
        Location loc = new Location(player.getWorld(),
                player.getLocation().getBlockX() + 0.5,
                player.getLocation().getBlockY() - 1,  // -1 pour que la surface soit à tes pieds
                player.getLocation().getBlockZ() + 0.5);

        spleefData.setArenaCenter(loc);

        spleefGame.generateArena();

        player.sendMessage("§f§l[SPLEEF] §fCentre positionné en "
                + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        player.sendMessage("§7Spawns calculés automatiquement — arène générée !");
    }

    private void announceStart() {
        String eventName = eventData.getEventType() != null ? eventData.getEventType() : "Event";
        boolean isTeam = eventData.getRewardMode() == EventData.RewardMode.TEAM;

        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
        Bukkit.broadcastMessage("§e§l  Début de l'événement : §6§n" + eventName.replaceAll("§.", "").toUpperCase());
        Bukkit.broadcastMessage("§e ");
        Bukkit.broadcastMessage("§f    Les récompenses seront les suivantes :");

        if (isTeam) {
            Bukkit.broadcastMessage("§7    Gagnant - " + formatRewards(1));
            Bukkit.broadcastMessage("§7    Perdant - " + formatRewards(2));
        } else {
            Bukkit.broadcastMessage("§7    1er  - " + formatRewards(1));
            Bukkit.broadcastMessage("§7    2ème - " + formatRewards(2));
            Bukkit.broadcastMessage("§7    3ème - " + formatRewards(3));
        }

        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
        Bukkit.broadcastMessage(" ");

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.playSound(online.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
    }

    private String formatRewards(int place) {
        List<String> parts = new ArrayList<>();
        for (EventData.RewardType type : EventData.RewardType.values()) {
            int amount = eventData.getReward(place, type);
            if (amount > 0) {
                if (type == EventData.RewardType.MONEY) {
                    parts.add("§e" + amount + "$ " + type.getDisplayName());
                } else {
                    parts.add("§e" + amount + "x " + type.getDisplayName());
                }
            }
        }
        return parts.isEmpty() ? "§8Aucune" : String.join("§f, ", parts);
    }

    public void broadcastRanking(Map<String, Integer> ranking, Map<String, String> displayScores) {
        boolean isTeamMode = eventData.getRewardMode() == EventData.RewardMode.TEAM;

        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
        Bukkit.broadcastMessage("§e§l   CLASSEMENT FINAL : §f" + eventData.getEventType());

        ranking.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(entry -> {
                    String label;
                    if (isTeamMode) {
                        label = entry.getValue() == 1 ? "§6§l🏆 Gagnant" : "§7§l😔 Perdant";
                    } else {
                        String suffix = (entry.getValue() == 1) ? "er" : "ème";
                        label = "§f" + entry.getValue() + suffix;
                    }
                    String score = displayScores.getOrDefault(entry.getKey(), "");
                    String scorePart = score.isEmpty() ? "" : " §7- §f" + score;
                    Bukkit.broadcastMessage("§f    " + label + " §e" + entry.getKey() + scorePart);
                });

        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
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

        Map<String, String> displayScores = currentGame.getDisplayScores();

        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
        Bukkit.broadcastMessage("§e§l   CLASSEMENT FINAL : §f" + eventData.getEventType());

        boolean isTeamMode = eventData.getRewardMode() == EventData.RewardMode.TEAM;

        ranking.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(entry -> {
                    String label;
                    if (isTeamMode) {
                        label = entry.getValue() == 1 ? "§6§l🏆 Gagnant" : "§7§l😔 Perdant";
                    } else {
                        String suffix = (entry.getValue() == 1) ? "er" : "ème";
                        label = "§f" + entry.getValue() + suffix;
                    }
                    String score = displayScores.getOrDefault(entry.getKey(), "");
                    String scorePart = score.isEmpty() ? "" : " §7- §f" + score;
                    Bukkit.broadcastMessage("§f    " + label + " §e" + entry.getKey() + scorePart);
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

    public void cleanup() {
        if (registrationReminderTask != null) {
            registrationReminderTask.cancel();
            registrationReminderTask = null;
        }
        if (quizChatListener != null) {
            HandlerList.unregisterAll(quizChatListener);
            quizChatListener = null;
        }
        if (nexusListener != null) {
            HandlerList.unregisterAll(nexusListener);
            nexusListener = null;
        }
        if (dacListener != null) {
            HandlerList.unregisterAll(dacListener);
            dacListener = null;
        }
        if (spleefListener != null) {
            HandlerList.unregisterAll(spleefListener);
            spleefListener = null;
        }
        nexusGame.clearHolograms();
        nexusGame.clearScoreboard();
        currentGame = null;
        eventData.reset();
        quizData.reset();
        nexusData.reset();
        dacData.reset();
        spleefData.reset();
    }

    private void sendHelp(Player player) {
        boolean isStaff = player.hasPermission("archievent.staff");

        player.sendMessage("§8§m      §r §6§lArchiEvent §8§m      ");

        player.sendMessage("§e/archievent participe             §7- S'inscrire à l'évent");
        player.sendMessage("§e/archievent quitter               §7- Se désinscrire de l'évent");

        if (isStaff) {
            player.sendMessage("§8§m-----------§r §7Staff §8§m-----------");
            player.sendMessage("§e/archievent create                §7- Configurer l'évent");
            player.sendMessage("§e/archievent open                  §7- Ouvrir les inscriptions");
            player.sendMessage("§e/archievent start                 §7- Lancer l'évent");
            player.sendMessage("§e/archievent test                  §7- Lancer sans vérif. inscrits");
            player.sendMessage("§e/archievent stop                  §7- Finir et donner les lots");
            player.sendMessage("§e/archievent cancel                §7- Tout stopper sans lots");
            player.sendMessage("§e/archievent question <q> <r>      §7- Poser une question (Quiz)");
            player.sendMessage("§e/archievent setnexus <rouge|bleu> §7- Poser un Nexus");
            player.sendMessage("§e/archievent setarena              §7- Définir l'arène Spleef");
            player.sendMessage("§e/archievent setpool <1|2>         §7- Définir la piscine DAC");
            player.sendMessage("§e/archievent setjump               §7- Définir le plongeoir DAC");
        }
    }
}