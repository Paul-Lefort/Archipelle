package fr.archipel.archiEvent.games.quiz;

import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizData {

    private String currentQuestion;
    private String currentAnswer;

    // --- SCORES ET GAGNANTS SPÉCIFIQUES AU QUIZ ---
    private final List<Player> currentQuestionWinners = new ArrayList<>();
    private final Map<String, Integer> globalScores = new HashMap<>();

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

    // --- LOGIQUE Quiz / Question ---

    public List<Player> getCurrentQuestionWinners() {
        return currentQuestionWinners;
    }

    public void clearQuestionWinners() {
        currentQuestionWinners.clear();
    }

    public void addPoints(String playerName, int pts) {
        globalScores.put(playerName, globalScores.getOrDefault(playerName, 0) + pts);
    }

    public Map<String, Integer> getGlobalScores() {
        return globalScores;
    }

    public void reset() {
        this.currentQuestion = null;
        this.currentAnswer = null;
        this.currentQuestionWinners.clear();
        this.globalScores.clear();
    }
}