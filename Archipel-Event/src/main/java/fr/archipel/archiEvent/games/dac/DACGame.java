package fr.archipel.archiEvent.games.dac;

import fr.archipel.archiEvent.ArchiEvent;
import fr.archipel.archiEvent.EventData;
import fr.archipel.archiEvent.games.Game;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class DACGame implements Game {

    private final EventData eventData;
    private final DACData dacData;
    private Runnable onGameEnd;

    private static final Material[] WOOL_COLORS = {
            Material.WHITE_WOOL, Material.ORANGE_WOOL, Material.MAGENTA_WOOL,
            Material.LIGHT_BLUE_WOOL, Material.YELLOW_WOOL, Material.LIME_WOOL,
            Material.PINK_WOOL, Material.GRAY_WOOL, Material.LIGHT_GRAY_WOOL,
            Material.CYAN_WOOL, Material.PURPLE_WOOL, Material.BLUE_WOOL,
            Material.BROWN_WOOL, Material.GREEN_WOOL, Material.RED_WOOL,
            Material.BLACK_WOOL
    };

    private final Random random = new Random();
    private DACListener dacListener;

    public DACGame(EventData eventData, DACData dacData) {
        this.eventData = eventData;
        this.dacData = dacData;
    }

    public void setDACListener(DACListener listener) {
        this.dacListener = listener;
    }

    public void setOnGameEnd(Runnable onGameEnd) {
        this.onGameEnd = onGameEnd;
    }

    // --- DÉMARRAGE ---

    public void start(List<UUID> registeredPlayers) {
        // Remplir la piscine d'eau
        fillPoolWithWater();

        // Construire la file dans l'ordre donné (déjà shuffled dans ArchiEventCommand)
        for (UUID uuid : registeredPlayers) {
            dacData.getJumpQueue().offer(uuid);
            dacData.addScore(uuid, 0); // Initialiser le score à 0
        }

        // TP tout le monde en spectateur sur le plongeoir
        for (UUID uuid : registeredPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.setGameMode(GameMode.SPECTATOR);
                p.teleport(dacData.getJumpLocation());
            }
        }

        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
        Bukkit.broadcastMessage("§e§l   DÈS À COUDRE — C'EST PARTI !");
        Bukkit.broadcastMessage("§f   " + registeredPlayers.size() + " joueurs participent.");
        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
        Bukkit.broadcastMessage(" ");

        // Lancer le premier sauteur
        nextJumper();
    }

    // --- GESTION DES TOURS ---

    public void nextJumper() {
        UUID next = dacData.pollNextJumper();
        if (next == null) {
            // File vide — ne devrait pas arriver mais sécurité
            endGame();
            return;
        }

        dacData.setCurrentJumper(next);
        Player jumper = Bukkit.getPlayer(next);

        if (jumper == null) {
            // Joueur déconnecté, on passe au suivant
            nextJumper();
            return;
        }

        // Passer le sauteur en aventure et TP sur le plongeoir
        jumper.setGameMode(GameMode.ADVENTURE);
        jumper.teleport(dacData.getJumpLocation());

        Bukkit.broadcastMessage("§e§l[DAC] §f" + jumper.getName() + " §7saute !");
        jumper.sendMessage("§a§l[DAC] §fC'est ton tour ! Saute !");

        // Titre pour le sauteur
        jumper.sendTitle("§a§lÀ TOI !", "§fSaute dans la piscine !", 10, 40, 10);

        // Démarrer la détection d'atterrissage
        if (dacListener != null) dacListener.startWatching();
    }

    /**
     * Appelé par DACListener quand un joueur atterrit dans la zone de plongeon.
     */
    public void onPlayerLand(Player player, Location landLocation) {
        if (!player.getUniqueId().equals(dacData.getCurrentJumper())) return;

        Block landBlock = landLocation.getBlock();
        Material type = landBlock.getType();

        if (type == Material.WATER) {
            // Vérifier si entouré de 4 laines (gauche, droite, devant, derrière)
            boolean surrounded = isWoolSurrounded(landLocation);
            int points = surrounded ? 3 : 1;

            dacData.addScore(player.getUniqueId(), points);

            // Placer une laine aléatoire à l'endroit où il est tombé
            Material wool = WOOL_COLORS[random.nextInt(WOOL_COLORS.length)];
            landBlock.setType(wool);

            if (surrounded) {
                Bukkit.broadcastMessage("§6§l[DAC] §e" + player.getName()
                        + " §fa réussi un §6§lSHOT PARFAIT §f! §6+3 points");
                player.sendTitle("§6§lSHOT PARFAIT !", "§f+3 points !", 10, 50, 10);
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            } else {
                Bukkit.broadcastMessage("§a[DAC] §e" + player.getName()
                        + " §fa atterri dans l'eau ! §a+1 point");
                player.sendTitle("§a§lDans l'eau !", "§f+1 point", 10, 40, 10);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1f, 1f);
            }

            // Repasser en spectateur
            player.setGameMode(GameMode.SPECTATOR);

            // Remettre en file pour la rotation
            dacData.requeueJumper(player.getUniqueId());

            // Vérifier si la piscine est pleine (plus d'eau)
            if (!hasWaterLeft()) {
                endGame();
                return;
            }

        } else {
            // Raté — atterri sur de la laine ou hors piscine
            Bukkit.broadcastMessage("§c[DAC] §e" + player.getName() + " §fa raté ! §c0 point");
            player.sendTitle("§c§lRATÉ !", "§fPas dans l'eau...", 10, 40, 10);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);

            // Annuler les dégâts
            player.setHealth(Math.min(player.getHealth() + 20, player.getMaxHealth()));
            player.setFoodLevel(20);

            player.setGameMode(GameMode.SPECTATOR);
            dacData.requeueJumper(player.getUniqueId());

            if (!hasWaterLeft()) {
                endGame();
                return;
            }
        }

        // Petit délai avant le prochain sauteur
        Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(ArchiEvent.class), this::nextJumper, 20L * 3);
    }

    /**
     * Appelé par DACListener quand le joueur atterrit hors de la zone de plongeon.
     */
    public void onPlayerMissedPool(Player player) {
        if (!player.getUniqueId().equals(dacData.getCurrentJumper())) return;

        Bukkit.broadcastMessage("§c[DAC] §e" + player.getName() + " §fa raté la piscine ! §c0 point");
        player.sendTitle("§c§lHORS PISCINE !", "§fTu as raté !", 10, 40, 10);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);

        // Annuler les dégâts de chute
        player.setHealth(Math.min(player.getHealth() + 20, player.getMaxHealth()));

        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(dacData.getJumpLocation());

        dacData.requeueJumper(player.getUniqueId());

        if (!hasWaterLeft()) {
            endGame();
            return;
        }

        Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(ArchiEvent.class), this::nextJumper, 20L * 3);
    }

    // --- FIN DU JEU ---

    private void endGame() {
        if (dacListener != null) dacListener.stopWatching();
        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
        Bukkit.broadcastMessage("§e§l   PLUS D'EAU ! FIN DU DÈS À COUDRE !");
        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
        Bukkit.broadcastMessage(" ");

        // Remettre tout le monde en survie
        for (UUID uuid : dacData.getScores().keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.setGameMode(GameMode.SURVIVAL);
        }

        if (onGameEnd != null) onGameEnd.run();
    }

    // --- UTILITAIRES ---

    private void fillPoolWithWater() {
        if (!dacData.isPoolDefined()) return;

        Location pos1 = dacData.getPoolPos1();
        Location pos2 = dacData.getPoolPos2();
        World world = pos1.getWorld();

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Material existing = world.getBlockAt(x, y, z).getType();
                    // Ne remplacer que l'eau déjà présente ou l'air — respecter la forme de la piscine
                    if (existing == Material.WATER || existing == Material.AIR) {
                        world.getBlockAt(x, y, z).setType(Material.WATER);
                    }
                }
            }
        }
    }

    private boolean hasWaterLeft() {
        if (!dacData.isPoolDefined()) return false;

        Location pos1 = dacData.getPoolPos1();
        Location pos2 = dacData.getPoolPos2();
        World world = pos1.getWorld();

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (world.getBlockAt(x, y, z).getType() == Material.WATER) return true;
                }
            }
        }
        return false;
    }

    private boolean isWoolSurrounded(Location loc) {
        World world = loc.getWorld();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        Material[] neighbors = {
                world.getBlockAt(x, y, z - 1).getType(), // nord
                world.getBlockAt(x, y, z + 1).getType(), // sud
                world.getBlockAt(x - 1, y, z).getType(), // ouest
                world.getBlockAt(x + 1, y, z).getType()  // est
        };

        // Chaque voisin doit être soit de la laine, soit un bord (hors piscine = ignoré)
        // Un voisin ne compte comme obstacle que s'il est dans la piscine ET n'est pas de la laine
        for (Material neighbor : neighbors) {
            if (neighbor == Material.WATER) return false; // voisin encore en eau = pas entouré
            // laine ou bord (pierre, air hors piscine...) = ok, on continue
        }
        return true;
    }

    private boolean isWool(Material mat) {
        return mat.name().endsWith("_WOOL");
    }

    // --- INTERFACE GAME ---

    @Override
    public Map<String, Integer> getRanking() {
        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(dacData.getScores().entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        Map<String, Integer> ranking = new HashMap<>();
        for (int i = 0; i < Math.min(3, sorted.size()); i++) {
            Player p = Bukkit.getPlayer(sorted.get(i).getKey());
            String name = p != null ? p.getName() : Bukkit.getOfflinePlayer(sorted.get(i).getKey()).getName();
            if (name != null) ranking.put(name, i + 1);
        }
        return ranking;
    }

    @Override
    public Map<String, String> getDisplayScores() {
        Map<String, String> display = new HashMap<>();
        for (Map.Entry<UUID, Integer> entry : dacData.getScores().entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            String name = p != null ? p.getName() : Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name != null) display.put(name, entry.getValue() + " pts");
        }
        return display;
    }

    public DACData getDacData() { return dacData; }
}