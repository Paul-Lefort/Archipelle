package fr.archipel.archiEvent;

import fr.archipel.archiEvent.commands.ArchiEventCommand;
import fr.archipel.archiEvent.listeners.MenuListener;
import fr.archipel.archiEvent.games.quiz.QuizChatListener;
import org.bukkit.plugin.java.JavaPlugin;

public class ArchiEvent extends JavaPlugin {

    @Override
    public void onEnable() {
        // 1. On crée l'objet qui va stocker les données de l'event
        EventData eventData = new EventData();

        // 2. On donne cet objet à la commande
        getCommand("archievent").setExecutor(new ArchiEventCommand(eventData));

        // 3. On donne le MÊME objet au listener (très important pour que les menus enregistrent dedans)
        getServer().getPluginManager().registerEvents(new MenuListener(eventData), this);
        getServer().getPluginManager().registerEvents(new QuizChatListener(eventData), this);

        getLogger().info("ArchiEvent active ! Prepret a mener la danse.");
    }
}