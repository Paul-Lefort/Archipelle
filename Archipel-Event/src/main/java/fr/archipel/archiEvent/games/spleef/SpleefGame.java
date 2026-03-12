package fr.archipel.archiEvent.games.spleef;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CylinderRegion;
import com.sk89q.worldedit.world.block.BlockTypes;
import fr.archipel.archiEvent.ArchiEvent;
import fr.archipel.archiEvent.EventData;
import fr.archipel.archiEvent.games.Game;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class SpleefGame implements Game {

    private final EventData eventData;
    private final SpleefData spleefData;
    private Runnable onGameEnd;

    // Top 3 final : place -> nom
    private final Map<Integer, String> podium = new HashMap<>();
    private int podiumPlace = 1;

    public SpleefGame(EventData eventData, SpleefData spleefData) {
        this.eventData = eventData;
        this.spleefData = spleefData;
    }

    public void setOnGameEnd(Runnable onGameEnd) {
        this.onGameEnd = onGameEnd;
    }

    // --- DÉMARRAGE ---

    public void start(List<UUID> registeredPlayers) {
        // Mélanger aléatoirement pour le bracket
        List<UUID> shuffled = new ArrayList<>(registeredPlayers);
        Collections.shuffle(shuffled);
        spleefData.setBracket(shuffled);

        // Générer l'arène de neige via WorldEdit
        generateArena();

        // TP tout le monde en spectateur
        for (UUID uuid : shuffled) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.setGameMode(GameMode.SPECTATOR);
                p.teleport(spleefData.getSpectatorSpawn());
            }
        }

        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
        Bukkit.broadcastMessage("§e§l   SPLEEF — TOURNOI 1v1 !");
        Bukkit.broadcastMessage("§f   " + shuffled.size() + " joueurs participent.");
        Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
        Bukkit.broadcastMessage(" ");

        // Lancer le premier match avec un délai
        Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(ArchiEvent.class),
                this::nextMatch, 20L * 3);
    }

    // --- GESTION DU BRACKET ---

    public void nextMatch() {
        List<UUID> bracket = spleefData.getBracket();

        // Plus qu'un joueur = gagnant final
        if (bracket.size() == 1) {
            UUID winner = bracket.get(0);
            Player p = Bukkit.getPlayer(winner);
            String name = p != null ? p.getName() : Bukkit.getOfflinePlayer(winner).getName();
            podium.put(podiumPlace++, name);
            spleefData.eliminate(winner);

            Bukkit.broadcastMessage("§6§l§m-------------------------------------------");
            Bukkit.broadcastMessage("§e§l   🏆 GAGNANT DU TOURNOI : §f" + name);
            Bukkit.broadcastMessage("§6§l§m-------------------------------------------");

            if (p != null) {
                p.setGameMode(GameMode.SURVIVAL);
                p.sendTitle("§6§l🏆 VICTOIRE !", "§fTu as gagné le tournoi !", 10, 60, 10);
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }

            if (onGameEnd != null) onGameEnd.run();
            return;
        }

        // Plus personne = fin anormale
        if (bracket.isEmpty()) {
            if (onGameEnd != null) onGameEnd.run();
            return;
        }

        // Prendre les deux premiers du bracket
        UUID uuidA = bracket.get(0);
        UUID uuidB = bracket.get(1);
        spleefData.setCurrentMatch(uuidA, uuidB);

        Player playerA = Bukkit.getPlayer(uuidA);
        Player playerB = Bukkit.getPlayer(uuidB);

        String nameA = playerA != null ? playerA.getName() : Bukkit.getOfflinePlayer(uuidA).getName();
        String nameB = playerB != null ? playerB.getName() : Bukkit.getOfflinePlayer(uuidB).getName();

        // Si un joueur est déconnecté, il perd par forfait
        if (playerA == null) { onMatchEnd(uuidB, uuidA); return; }
        if (playerB == null) { onMatchEnd(uuidA, uuidB); return; }

        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage("§6§l[SPLEEF] §eProchain match : §f" + nameA + " §7vs §f" + nameB);
        Bukkit.broadcastMessage(" ");

        // Reconstruire l'arène
        rebuildArena();

        // TP et setup des joueurs
        setupPlayer(playerA, spleefData.getSpawnA());
        setupPlayer(playerB, spleefData.getSpawnB());

        // Countdown 3 secondes puis libérer
        runCountdown(playerA, playerB);
    }

    private void runCountdown(Player a, Player b) {
        for (int i = 3; i >= 1; i--) {
            final int count = i;
            Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(ArchiEvent.class), () -> {
                a.sendTitle("§e§l" + count, "", 0, 25, 5);
                b.sendTitle("§e§l" + count, "", 0, 25, 5);
                a.playSound(a.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                b.playSound(b.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
            }, 20L * (4 - i));
        }

        // GO
        Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(ArchiEvent.class), () -> {
            a.sendTitle("§a§lGO !", "§fCasse la neige !", 0, 30, 10);
            b.sendTitle("§a§lGO !", "§fCasse la neige !", 0, 30, 10);
            a.playSound(a.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            b.playSound(b.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        }, 20L * 4);
    }

    /**
     * Appelé par SpleefListener quand un joueur tombe dans la lave.
     */
    public void onPlayerLost(Player loser) {
        UUID loserUuid = loser.getUniqueId();
        if (!spleefData.isInCurrentMatch(loserUuid)) return;

        UUID winnerUuid = loserUuid.equals(spleefData.getCurrentPlayerA())
                ? spleefData.getCurrentPlayerB()
                : spleefData.getCurrentPlayerA();

        onMatchEnd(winnerUuid, loserUuid);
    }

    private void onMatchEnd(UUID winnerUuid, UUID loserUuid) {
        Player winner = Bukkit.getPlayer(winnerUuid);
        Player loser = Bukkit.getPlayer(loserUuid);

        String winnerName = winner != null ? winner.getName() : Bukkit.getOfflinePlayer(winnerUuid).getName();
        String loserName = loser != null ? loser.getName() : Bukkit.getOfflinePlayer(loserUuid).getName();

        Bukkit.broadcastMessage("§a§l[SPLEEF] §e" + winnerName + " §fgagne contre §e" + loserName + " §f!");

        // Éliminer le perdant
        spleefData.eliminate(loserUuid);

        // Soigner le gagnant et le repasser en spectateur temporairement
        if (winner != null) {
            winner.setGameMode(GameMode.SPECTATOR);
            winner.teleport(spleefData.getSpectatorSpawn());
            winner.sendTitle("§a§lVICTOIRE !", "§fTu passes au tour suivant !", 10, 50, 10);
            winner.getInventory().clear();
        }
        if (loser != null) {
            loser.setGameMode(GameMode.SPECTATOR);
            loser.teleport(spleefData.getSpectatorSpawn());
            loser.sendTitle("§c§lÉLIMINÉ", "§fMeilleure chance la prochaine fois !", 10, 50, 10);
            loser.getInventory().clear();
        }

        // Le gagnant reste en tête du bracket, le perdant est sorti
        // Réorganiser : retirer les deux premiers, remettre le gagnant à la fin
        List<UUID> bracket = spleefData.getBracket();
        bracket.remove(winnerUuid);
        bracket.remove(loserUuid);
        bracket.add(winnerUuid); // gagnant repart en fin de bracket

        // Prochain match après 5 secondes
        Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(ArchiEvent.class),
                this::nextMatch, 20L * 5);
    }

    // --- ARÈNE ---

    /**
     * Génère l'arène complète via WorldEdit.
     * Le staff fait /setarena debout sur la neige.
     * getBlockY() = Y de la neige.
     *
     *   cy-3 à cy+2 : barrière r=9, hauteur 6 — murs invisibles
     *   cy-2         : lave r=8
     *   cy-1         : air r=8
     *   cy           : snow_block r=8 — sol jouable
     *   cy+1 à cy+2  : air r=8 — espace de jeu
     */
    public void generateArena() {
        Location center = spleefData.getArenaCenter();
        if (center == null) return;

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(center.getWorld());
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        try (EditSession editSession = WorldEdit.getInstance()
                .newEditSessionBuilder()
                .world(weWorld)
                .build()) {

            com.sk89q.worldedit.math.Vector2 r9 = com.sk89q.worldedit.math.Vector2.at(9, 9);
            com.sk89q.worldedit.math.Vector2 r8 = com.sk89q.worldedit.math.Vector2.at(8, 8);

            // 1. Barrière r=9, de cy-3 à cy+2 (hauteur 6) — pose en premier
            editSession.setBlocks(
                    new CylinderRegion(weWorld, BlockVector3.at(cx, cy - 3, cz), r9, cy - 3, cy + 2),
                    BlockTypes.BARRIER.getDefaultState()
            );

            // 2. Lave r=8 à cy-2 — écrase la barrière à l'intérieur
            editSession.setBlocks(
                    new CylinderRegion(weWorld, BlockVector3.at(cx, cy - 2, cz), r8, cy - 2, cy - 2),
                    BlockTypes.LAVA.getDefaultState()
            );

            // 3. Air r=8 à cy-1
            editSession.setBlocks(
                    new CylinderRegion(weWorld, BlockVector3.at(cx, cy - 1, cz), r8, cy - 1, cy - 1),
                    BlockTypes.AIR.getDefaultState()
            );

            // 4. Neige r=8 à cy
            editSession.setBlocks(
                    new CylinderRegion(weWorld, BlockVector3.at(cx, cy, cz), r8, cy, cy),
                    BlockTypes.SNOW_BLOCK.getDefaultState()
            );

            // 5. Air r=8 à cy+1 et cy+2 — espace de jeu
            editSession.setBlocks(
                    new CylinderRegion(weWorld, BlockVector3.at(cx, cy + 1, cz), r8, cy + 1, cy + 2),
                    BlockTypes.AIR.getDefaultState()
            );

        } catch (Exception e) {
            Bukkit.getLogger().severe("[ArchiEvent] Erreur génération arène Spleef : " + e.getMessage());
        }
    }

    private void rebuildArena() {
        Location center = spleefData.getArenaCenter();
        if (center == null) return;

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(center.getWorld());
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        try (EditSession editSession = WorldEdit.getInstance()
                .newEditSessionBuilder()
                .world(weWorld)
                .build()) {

            // Reposer la neige à cy (Y du staff = Y de la neige)
            editSession.setBlocks(
                    new CylinderRegion(weWorld, BlockVector3.at(cx, cy, cz),
                            com.sk89q.worldedit.math.Vector2.at(8, 8), cy, cy),
                    BlockTypes.SNOW_BLOCK.getDefaultState()
            );

        } catch (Exception e) {
            Bukkit.getLogger().severe("[ArchiEvent] Erreur reconstruction arène : " + e.getMessage());
        }
    }

    private void scanSnowBlocks() {
        // Plus utilisé — on passe par WorldEdit pour reconstruire
    }

    private void setupPlayer(Player player, Location spawn) {
        player.setGameMode(GameMode.SURVIVAL);
        player.teleport(spawn);
        player.getInventory().clear();
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);

        // Donner la pelle enchantée efficacité
        ItemStack shovel = new ItemStack(Material.DIAMOND_SHOVEL);
        ItemMeta meta = shovel.getItemMeta();
        meta.setDisplayName("§b§lPelle du Spleef");
        meta.addEnchant(org.bukkit.enchantments.Enchantment.EFFICIENCY, 5, true);
        meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 10, true);
        shovel.setItemMeta(meta);
        player.getInventory().setItem(0, shovel);
    }

    // --- INTERFACE GAME ---

    @Override
    public Map<String, Integer> getRanking() {
        Map<String, Integer> ranking = new HashMap<>();
        for (Map.Entry<Integer, String> entry : podium.entrySet()) {
            ranking.put(entry.getValue(), entry.getKey());
        }
        return ranking;
    }

    @Override
    public Map<String, String> getDisplayScores() {
        return new HashMap<>(); // Pas de score à afficher pour le Spleef
    }

    public SpleefData getSpleefData() { return spleefData; }
}