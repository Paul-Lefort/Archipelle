package fr.archipel.archiEvent.manager;

import fr.archipel.archiEvent.EventData;
import fr.archipel.archiEvent.EventData.RewardType;
import fr.archipel.archiEvent.EventData.RewardMode;
import org.bukkit.Bukkit;

import java.util.Map;

public class RewardManager {

    private final EventData eventData;

    public RewardManager(EventData eventData) {
        this.eventData = eventData;
    }

    public void distributeRewards(Map<String, Integer> ranking) {
        // En mode TEAM : place 1 = gagnant, place 2 = perdant
        // En mode TOP3 : place 1, 2, 3
        int maxPlace = eventData.getRewardMode() == RewardMode.TEAM ? 2 : 3;

        for (Map.Entry<String, Integer> entry : ranking.entrySet()) {
            String playerName = entry.getKey();
            int place = entry.getValue();

            if (place < 1 || place > maxPlace) continue;

            for (RewardType type : RewardType.values()) {
                int amount = eventData.getReward(place, type);
                if (amount > 0) {
                    String cmd;
                    if (type == RewardType.MONEY) {
                        cmd = "eco give " + playerName + " " + amount;
                    } else {
                        cmd = type.getCommandName() + " " + playerName + " " + amount;
                    }
                    Bukkit.getLogger().info("[ArchiEvent] Récompense : " + cmd);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
            }
        }
    }
}