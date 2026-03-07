package fr.archipel.archiEvent.games;

import java.util.Map;

public interface Game {

    /**
     * Retourne le classement final : Map<NomJoueur, Place>
     * ex: {"Steve": 1, "Alex": 2}
     */
    Map<String, Integer> getRanking();

    /**
     * Retourne le score/stat affiché à côté du pseudo dans le classement final.
     * ex: Quiz → "10 pts", Nexus → "3240 dégâts"
     */
    Map<String, String> getDisplayScores();
}