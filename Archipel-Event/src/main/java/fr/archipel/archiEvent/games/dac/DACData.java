package fr.archipel.archiEvent.games.dac;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

public class DACData {

    // Zone de plongeon définie par 2 coins
    private Location poolPos1;
    private Location poolPos2;

    // Zone de saut — là où les joueurs sont TP au début
    private Location jumpLocation;

    // File d'attente tournante des joueurs inscrits
    private final Queue<UUID> jumpQueue = new LinkedList<>();

    // Scores : UUID -> points
    private final Map<UUID, Integer> scores = new HashMap<>();

    // Joueur actuellement en train de sauter
    private UUID currentJumper;

    // --- ZONE DE PLONGEON ---

    public Location getPoolPos1() { return poolPos1; }
    public void setPoolPos1(Location loc) { this.poolPos1 = loc; }

    public Location getPoolPos2() { return poolPos2; }
    public void setPoolPos2(Location loc) { this.poolPos2 = loc; }

    public boolean isPoolDefined() { return poolPos1 != null && poolPos2 != null; }

    /** Vérifie si une location est dans la zone de plongeon (XZ uniquement, pas de limite Y) */
    public boolean isInPool(Location loc) {
        if (!isPoolDefined()) return false;
        if (!loc.getWorld().equals(poolPos1.getWorld())) return false;

        int minX = Math.min(poolPos1.getBlockX(), poolPos2.getBlockX());
        int maxX = Math.max(poolPos1.getBlockX(), poolPos2.getBlockX());
        int minZ = Math.min(poolPos1.getBlockZ(), poolPos2.getBlockZ());
        int maxZ = Math.max(poolPos1.getBlockZ(), poolPos2.getBlockZ());
        int minY = Math.min(poolPos1.getBlockY(), poolPos2.getBlockY());
        int maxY = Math.max(poolPos1.getBlockY(), poolPos2.getBlockY());

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    // --- ZONE DE SAUT ---

    public Location getJumpLocation() { return jumpLocation; }
    public void setJumpLocation(Location loc) { this.jumpLocation = loc; }

    public boolean isJumpDefined() { return jumpLocation != null; }

    // --- FILE D'ATTENTE ---

    public Queue<UUID> getJumpQueue() { return jumpQueue; }

    public UUID pollNextJumper() {
        return jumpQueue.poll();
    }

    /** Remet le joueur en fin de file pour la rotation */
    public void requeueJumper(UUID uuid) {
        jumpQueue.offer(uuid);
    }

    public UUID getCurrentJumper() { return currentJumper; }
    public void setCurrentJumper(UUID uuid) { this.currentJumper = uuid; }

    // --- SCORES ---

    public void addScore(UUID uuid, int points) {
        scores.put(uuid, scores.getOrDefault(uuid, 0) + points);
    }

    public int getScore(UUID uuid) {
        return scores.getOrDefault(uuid, 0);
    }

    public Map<UUID, Integer> getScores() { return scores; }

    // --- RESET ---

    public void reset() {
        poolPos1 = null;
        poolPos2 = null;
        jumpLocation = null;
        jumpQueue.clear();
        scores.clear();
        currentJumper = null;
    }
}