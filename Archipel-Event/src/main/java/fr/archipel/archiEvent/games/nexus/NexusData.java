package fr.archipel.archiEvent.games.nexus;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NexusData {

    public static final double NEXUS_MAX_HP = 5000.0;

    // Les deux équipes
    private final NexusTeam teamRed = new NexusTeam("§cRouge");
    private final NexusTeam teamBlue = new NexusTeam("§9Bleu");

    // Dégâts infligés au cristal adverse par joueur : UUID -> total dégâts
    private final Map<UUID, Double> damageDealt = new HashMap<>();

    // HP actuels de chaque cristal
    private double redCrystalHp = NEXUS_MAX_HP;
    private double blueCrystalHp = NEXUS_MAX_HP;

    // Locations pré-enregistrées par le staff avant /start
    private Location pendingRedLocation;
    private Location pendingBlueLocation;

    public NexusTeam getTeamRed() { return teamRed; }
    public NexusTeam getTeamBlue() { return teamBlue; }

    public NexusTeam getTeamOf(UUID uuid) {
        if (teamRed.getMembers().contains(uuid)) return teamRed;
        if (teamBlue.getMembers().contains(uuid)) return teamBlue;
        return null;
    }

    public NexusTeam getOpponentTeam(NexusTeam team) {
        return team == teamRed ? teamBlue : teamRed;
    }

    // --- HP ---

    public double getRedCrystalHp() { return redCrystalHp; }
    public double getBlueCrystalHp() { return blueCrystalHp; }

    public void damageRedCrystal(double amount) { redCrystalHp = Math.max(0, redCrystalHp - amount); }
    public void damageBlueCrystal(double amount) { blueCrystalHp = Math.max(0, blueCrystalHp - amount); }

    // --- DÉGÂTS PAR JOUEUR ---

    public void addDamage(UUID uuid, double amount) {
        damageDealt.put(uuid, damageDealt.getOrDefault(uuid, 0.0) + amount);
    }

    public Map<UUID, Double> getDamageDealt() { return damageDealt; }

    // --- LOCATIONS EN ATTENTE ---

    public Location getPendingRedLocation() { return pendingRedLocation; }
    public void setPendingRedLocation(Location loc) { this.pendingRedLocation = loc; }

    public Location getPendingBlueLocation() { return pendingBlueLocation; }
    public void setPendingBlueLocation(Location loc) { this.pendingBlueLocation = loc; }

    public boolean areBothLocationsSet() {
        return pendingRedLocation != null && pendingBlueLocation != null;
    }

    public void reset() {
        teamRed.getMembers().clear();
        teamBlue.getMembers().clear();
        teamRed.setCrystal(null);
        teamBlue.setCrystal(null);
        damageDealt.clear();
        redCrystalHp = NEXUS_MAX_HP;
        blueCrystalHp = NEXUS_MAX_HP;
        pendingRedLocation = null;
        pendingBlueLocation = null;
    }
}