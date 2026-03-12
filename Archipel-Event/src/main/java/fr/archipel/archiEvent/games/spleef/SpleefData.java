package fr.archipel.archiEvent.games.spleef;

import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.*;

public class SpleefData {

    // Centre de l'arène (posé par le staff)
    private Location arenaCenter;

    // Rayon et hauteur du cylindre
    public static final int RADIUS = 8;
    public static final int HEIGHT = 1; // épaisseur du sol

    // Spawns calculés automatiquement
    private Location spawnA;
    private Location spawnB;
    private Location spectatorSpawn;

    // Bracket : liste ordonnée des participants encore en lice
    private final List<UUID> bracket = new ArrayList<>();

    // Joueurs éliminés dans l'ordre (pour le podium inversé)
    private final List<UUID> eliminated = new ArrayList<>();

    // Match en cours
    private UUID currentPlayerA;
    private UUID currentPlayerB;

    // Blocs de neige originaux (pour la reconstruction)
    private final Set<Block> snowBlocks = new HashSet<>();

    // --- ARÈNE ---

    public Location getArenaCenter() { return arenaCenter; }
    public void setArenaCenter(Location loc) {
        this.arenaCenter = loc;
        // Spawns à cy+1 (au dessus de la neige)
        this.spawnA = loc.clone().add(RADIUS - 2, 1, 0);
        this.spawnB = loc.clone().add(-(RADIUS - 2), 1, 0);
        // Spectateurs 15 blocs au dessus de la neige
        this.spectatorSpawn = loc.clone().add(0, 15, 0);
    }

    public boolean isArenaDefined() { return arenaCenter != null; }

    public Location getSpawnA() { return spawnA; }
    public void setSpawnA(Location loc) { this.spawnA = loc; }

    public Location getSpawnB() { return spawnB; }
    public void setSpawnB(Location loc) { this.spawnB = loc; }

    public Location getSpectatorSpawn() { return spectatorSpawn; }
    public void setSpectatorSpawn(Location loc) { this.spectatorSpawn = loc; }

    public boolean isSpawnsDefined() { return spawnA != null && spawnB != null && spectatorSpawn != null; }

    public Set<Block> getSnowBlocks() { return snowBlocks; }
    public void setSnowBlocks(Set<Block> blocks) {
        snowBlocks.clear();
        snowBlocks.addAll(blocks);
    }

    // --- BRACKET ---

    public List<UUID> getBracket() { return bracket; }

    public void setBracket(List<UUID> players) {
        bracket.clear();
        bracket.addAll(players);
    }

    public UUID getCurrentPlayerA() { return currentPlayerA; }
    public UUID getCurrentPlayerB() { return currentPlayerB; }

    public void setCurrentMatch(UUID a, UUID b) {
        this.currentPlayerA = a;
        this.currentPlayerB = b;
    }

    public boolean isInCurrentMatch(UUID uuid) {
        return uuid.equals(currentPlayerA) || uuid.equals(currentPlayerB);
    }

    // --- ÉLIMINATIONS ---

    public void eliminate(UUID uuid) {
        bracket.remove(uuid);
        eliminated.add(uuid);
    }

    public List<UUID> getEliminated() { return eliminated; }

    // --- RESET ---

    public void reset() {
        arenaCenter = null;
        spawnA = null;
        spawnB = null;
        spectatorSpawn = null;
        bracket.clear();
        eliminated.clear();
        snowBlocks.clear();
        currentPlayerA = null;
        currentPlayerB = null;
    }
}