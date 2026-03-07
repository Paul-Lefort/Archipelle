package fr.archipel.archiEvent;

import fr.archipel.archiEvent.commands.ArchiEventCommand;
import fr.archipel.archiEvent.games.dac.DACLogic;
import fr.archipel.archiEvent.games.dac.DACPlayerListener;
import fr.archipel.archiEvent.listeners.MenuListener;
import fr.archipel.archiEvent.games.quiz.QuizChatListener;
import org.bukkit.plugin.java.JavaPlugin;

public class ArchiEvent extends JavaPlugin {

    @Override
    public void onEnable() {

        EventData eventData = new EventData();
        DACLogic dacLogic = new DACLogic(eventData);

        getCommand("archievent").setExecutor(new ArchiEventCommand(eventData));

        getServer().getPluginManager().registerEvents(new MenuListener(eventData), this);
        getServer().getPluginManager().registerEvents(new QuizChatListener(eventData), this);
        getServer().getPluginManager().registerEvents(new DACPlayerListener(eventData, dacLogic), this);

        getLogger().info("ArchiEvent active ! Prepret a mener la danse.");
    }
}