package fr.archipel.archiEvent;

import fr.archipel.archiEvent.commands.ArchiEventCommand;
import fr.archipel.archiEvent.listeners.MenuListener;
import fr.archipel.archiEvent.games.quiz.QuizChatListener;
import fr.archipel.archiEvent.games.quiz.QuizData; // Import du nouveau module
import org.bukkit.plugin.java.JavaPlugin;

public class ArchiEvent extends JavaPlugin {

    @Override
    public void onEnable() {
        // 1. On crée les deux objets de stockage
        // EventData : Pour les récompenses communes
        EventData eventData = new EventData();
        // QuizData : Pour la logique spécifique au Quiz (questions/scores)
        QuizData quizData = new QuizData();

        // 2. On donne les DEUX objets à la commande
        // (Il faudra sûrement mettre à jour le constructeur de ArchiEventCommand)
        getCommand("archievent").setExecutor(new ArchiEventCommand(eventData, quizData));

        // 3. On enregistre les listeners
        // Le MenuListener n'a besoin que d'EventData (pour régler les récompenses)
        getServer().getPluginManager().registerEvents(new MenuListener(eventData), this);

        // Le QuizChatListener a besoin des deux (Rewards + Logique Quiz)
        getServer().getPluginManager().registerEvents(new QuizChatListener(eventData, quizData), this);

        getLogger().info("ArchiEvent active ! Prêt à mener la danse.");
    }
}