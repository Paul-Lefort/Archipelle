package fr.archipel.archiEvent.games.nexus;

import org.bukkit.Location;
import org.bukkit.entity.EnderCrystal;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NexusTeam {

    private final String name;
    private final List<UUID> members = new ArrayList<>();
    private EnderCrystal crystal;
    private Location crystalLocation;

    public NexusTeam(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public List<UUID> getMembers() { return members; }

    public void addMember(UUID uuid) { members.add(uuid); }

    public EnderCrystal getCrystal() { return crystal; }

    public void setCrystal(EnderCrystal crystal) { this.crystal = crystal; }

    public Location getCrystalLocation() { return crystalLocation; }

    public void setCrystalLocation(Location loc) { this.crystalLocation = loc; }

    public boolean hasCrystal() {
        return crystal != null && !crystal.isDead();
    }
}