package fr.archipel.archiEvent.manager;

import fr.archipel.archiEvent.EventData;
import fr.archipel.archiEvent.EventData.RewardType;
import org.bukkit.Bukkit;

import java.util.Map;

public class RewardManager {

    private final EventData eventData;

    public RewardManager(EventData eventData) {
        this.eventData = eventData;
    }

    /**
     * Distribue les récompenses à partir d'un classement.
     *
     * @param ranking Map<NomJoueur, Place> — ex: {"Steve": 1, "Alex": 2, "Notch": 3}
     */
    public void distributeRewards(Map<String, Integer> ranking) {
        for (Map.Entry<String, Integer> entry : ranking.entrySet()) {
            String playerName = entry.getKey();
            int place = entry.getValue();

            // On ignore les places hors top 3
            if (place < 1 || place > 3) continue;

            for (RewardType type : RewardType.values()) {
                int amount = eventData.getReward(place, type);
                if (amount > 0) {
                    String cmd = "excellentcrates give " + playerName + " " + type.getCommandName() + " " + amount;
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
            }
        }
    }
}