package fr.archipel.archiEvent;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EventData {

    public enum RewardType {
        LEGENDAIRE("§6Légendaire", "cle_legendaire", Material.GOLD_NUGGET),
        LEGENDAIRE_ALL("§6Légendaire (all)", "cle_legendaire_all", Material.GOLD_INGOT),
        ANTIQUE("§dAntique", "cle_antique", Material.ECHO_SHARD),
        ANTIQUE_ALL("§dAntique (all)", "cle_antique_all", Material.AMETHYST_SHARD),
        QUANTIQUE("§bQuantique", "cle_quantique", Material.NETHER_STAR),
        QUANTIQUE_ALL("§bQuantique (all)", "cle_quantique_all", Material.DIAMOND),
        SPAWNER("§aSpawner", "cle_spawner", Material.SPAWNER),
        SPAWNER_ALL("§aSpawner (all)", "cle_spawner_all", Material.ZOMBIE_SPAWN_EGG),
        VOTE("§eVote", "cle_vote", Material.PAPER),
        VOTE_ALL("§eVote (all)", "cle_vote_all", Material.MAP);

        private final String displayName;
        private final String commandName;
        private final Material guiMaterial;

        RewardType(String displayName, String commandName, Material guiMaterial) {
            this.displayName = displayName;
            this.commandName = commandName;
            this.guiMaterial = guiMaterial;
        }

        public String getDisplayName() { return displayName; }
        public String getCommandName() { return commandName; }
        public Material getGuiMaterial() { return guiMaterial; }
    }

    private String eventType;
    private final Map<Integer, Map<RewardType, Integer>> rewards = new HashMap<>();

    // Inscriptions — uniquement utilisé pour les events qui le nécessitent (Nexus etc.)
    private boolean registrationOpen = false;
    private final List<UUID> registeredPlayers = new ArrayList<>();

    public void setEventType(String type) { this.eventType = type; }
    public String getEventType() { return eventType; }

    // --- MODE RÉCOMPENSES ---

    public enum RewardMode { TOP3, TEAM }

    private RewardMode rewardMode = RewardMode.TOP3;

    public void setRewardMode(RewardMode mode) { this.rewardMode = mode; }
    public RewardMode getRewardMode() { return rewardMode; }

    // --- RÉCOMPENSES ---
    // TOP3 : clé = 1, 2, 3 (place)
    // TEAM : clé = 1 (gagnant), 2 (perdant)

    public void setReward(int place, RewardType type, int amount) {
        rewards.computeIfAbsent(place, k -> new HashMap<>()).put(type, amount);
    }

    public int getReward(int place, RewardType type) {
        if (!rewards.containsKey(place)) return 0;
        return rewards.get(place).getOrDefault(type, 0);
    }

    // --- INSCRIPTIONS ---

    public void openRegistration() { this.registrationOpen = true; }
    public void closeRegistration() { this.registrationOpen = false; }
    public boolean isRegistrationOpen() { return registrationOpen; }

    public boolean register(UUID uuid) {
        if (registeredPlayers.contains(uuid)) return false;
        registeredPlayers.add(uuid);
        return true;
    }

    public boolean unregister(UUID uuid) {
        return registeredPlayers.remove(uuid);
    }

    public List<UUID> getRegisteredPlayers() { return registeredPlayers; }

    public int getRegisteredCount() { return registeredPlayers.size(); }

    // --- RESET ---

    public void reset() {
        rewards.clear();
        eventType = null;
        rewardMode = RewardMode.TOP3;
        registrationOpen = false;
        registeredPlayers.clear();
    }
}