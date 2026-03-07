package fr.archipel.archiEvent;

import fr.archipel.archiEvent.commands.ArchiEventCommand;
import fr.archipel.archiEvent.commands.ArchiEventTabCompleter;
import fr.archipel.archiEvent.games.nexus.NexusData;
import fr.archipel.archiEvent.games.quiz.QuizData;
import fr.archipel.archiEvent.listeners.MenuListener;
import org.bukkit.plugin.java.JavaPlugin;

public class ArchiEvent extends JavaPlugin {

    private ArchiEventCommand executor;

    @Override
    public void onEnable() {
        EventData eventData = new EventData();
        QuizData quizData = new QuizData();
        NexusData nexusData = new NexusData();

        ArchiEventCommand executor = new ArchiEventCommand(this, eventData, quizData, nexusData);
        getCommand("archievent").setExecutor(executor);
        getCommand("archievent").setTabCompleter(new ArchiEventTabCompleter(eventData));
        this.executor = executor;
        getServer().getPluginManager().registerEvents(new MenuListener(eventData), this);

        getLogger().info("ArchiEvent active ! Prêt à mener la danse.");
    }

    @Override
    public void onDisable() {
        // Nettoyer proprement en cas de crash ou reload
        if (executor != null) {
            executor.cleanup();
        }
        getLogger().info("ArchiEvent désactivé !");
    }
}