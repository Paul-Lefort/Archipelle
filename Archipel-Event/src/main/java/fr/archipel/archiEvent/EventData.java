package fr.archipel.archiEvent;

import org.bukkit.Material;
import java.util.HashMap;
import java.util.Map;

public class EventData {

    public enum RewardType {
        LEGENDAIRE("§6Légendaire", "legendaire", Material.GOLD_NUGGET),
        SPAWNERS("§aSpawners", "spawners", Material.SPAWNER),
        QUANTIQUE("§bQuantique", "quantique", Material.NETHER_STAR),
        ANTIQUE("§dAntique", "antique", Material.ECHO_SHARD);

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

    public void setEventType(String type) {
        this.eventType = type;
    }

    public String getEventType() {
        return eventType;
    }

    // --- GESTION DES RÉCOMPENSES (Commun à tous) ---

    public void setReward(int place, RewardType type, int amount) {
        rewards.computeIfAbsent(place, k -> new HashMap<>()).put(type, amount);
    }

    public int getReward(int place, RewardType type) {
        if (!rewards.containsKey(place)) return 0;
        return rewards.get(place).getOrDefault(type, 0);
    }

    public void reset() {
        rewards.clear();
        eventType = null;
    }
}