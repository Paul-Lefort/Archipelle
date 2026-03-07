package fr.archipel.archiEvent;

import fr.archipel.archiEvent.commands.ArchiEventCommand;
import fr.archipel.archiEvent.games.nexus.NexusData;
import fr.archipel.archiEvent.games.quiz.QuizData;
import fr.archipel.archiEvent.listeners.MenuListener;
import org.bukkit.plugin.java.JavaPlugin;

public class ArchiEvent extends JavaPlugin {

    @Override
    public void onEnable() {
        EventData eventData = new EventData();
        QuizData quizData = new QuizData();
        NexusData nexusData = new NexusData();

        getCommand("archievent").setExecutor(new ArchiEventCommand(this, eventData, quizData, nexusData));
        getServer().getPluginManager().registerEvents(new MenuListener(eventData), this);

        getLogger().info("ArchiEvent active ! Prêt à mener la danse.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ArchiEvent désactivé !");
    }
}