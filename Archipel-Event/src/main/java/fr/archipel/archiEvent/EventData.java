package fr.archipel.archiEvent;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

public class EventData {

    // Permet de centraliser le nom des clés.
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

    // --- DONNÉES DE L'EVENT ---
    private String eventType;

    // --- DONNÉES DU QUIZ ---
    private String currentQuestion;
    private String currentAnswer;

    // --- DONNÉES DE DAS ---

    // Dans EventData.java
    private Location dacJumpPoint;
    private Location poolPos1;
    private Location poolPos2;
    private Player currentDacPlayer; // Pour savoir qui est en train de sauter

    private List<Player> dacPlayerOrder = new ArrayList<>();

    public void setDacPlayerOrder(List<Player> dacPlayerOrder) {
        this.dacPlayerOrder = dacPlayerOrder;
    }

    public void setDacJumpPoint(Location loc) { this.dacJumpPoint = loc; }
    public Location getDacJumpPoint() { return dacJumpPoint; }

    public void setPoolPos1(Location loc) { this.poolPos1 = loc; }
    public void setPoolPos2(Location loc) { this.poolPos2 = loc; }
    public Location getPoolPos1() { return poolPos1; }
    public Location getPoolPos2() { return poolPos2; }

    public Player getCurrentDacPlayer() { return currentDacPlayer; }
    public void setCurrentDacPlayer(Player p) { this.currentDacPlayer = p; }













    // --- RÉCOMPENSES (Place -> Enum RewardType -> Quantité) ---
    private final Map<Integer, Map<RewardType, Integer>> rewards = new HashMap<>();

    // --- SCORES ET GAGNANTS ---
    private final List<Player> currentQuestionWinners = new ArrayList<>();
    private final Map<String, Integer> globalScores = new HashMap<>();

    // --- GETTERS & SETTERS CLASSIQUES ---
    public void setEventType(String type) {
        this.eventType = type;
    }

    public String getEventType() {
        return eventType;
    }

    public void setQuestion(String question) {
        this.currentQuestion = question;
    }

    public String getQuestion() {
        return currentQuestion;
    }

    public void setAnswer(String answer) {
        this.currentAnswer = answer;
    }

    public String getAnswer() {
        return currentAnswer;
    }

    // --- GESTION DES RÉCOMPENSES (Utilise l'Enum) ---

    // Map les récompenses avec les positions
    public void setReward(int place, RewardType type, int amount) {
        rewards.computeIfAbsent(place, k -> new HashMap<>()).put(type, amount);
    }

    // Récupère les récompenses d'une place
    public int getReward(int place, RewardType type) {
        if (!rewards.containsKey(place)) return 0;
        return rewards.get(place).getOrDefault(type, 0);
    }


    // --- LOGIQUE Quiz / Question  ---

    public List<Player> getCurrentQuestionWinners() {
        return currentQuestionWinners;
    }

    // Utiliser à chaque question pour reset le classement temporaire
    public void clearQuestionWinners() {
        currentQuestionWinners.clear();
    }

    // Permet d'ajouter des points selon la place
    public void addPoints(String playerName, int pts) {
        globalScores.put(playerName, globalScores.getOrDefault(playerName, 0) + pts);
    }

    // Permet de récupérer le score final
    public Map<String, Integer> getGlobalScores() {
        return globalScores;
    }

    // Reset tout les données
    public void reset(){
        rewards.clear();
        currentQuestionWinners.clear();
        globalScores.clear();
    }
}