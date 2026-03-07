package fr.archipel.archiEvent.commands;

import fr.archipel.archiEvent.EventData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class ArchiEventTabCompleter implements TabCompleter {

    private final EventData eventData;

    public ArchiEventTabCompleter(EventData eventData) {
        this.eventData = eventData;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) return List.of();

        String type = eventData.getEventType();

        if (args.length == 1) {
            List<String> suggestions;

            if (type == null) {
                // Pas d'event configuré
                suggestions = List.of("create");

            } else if (type.contains("Quiz")) {
                suggestions = List.of("create", "question", "start", "stop", "cancel");

            } else if (type.contains("Dès") || type.contains("DAC")) {
                suggestions = List.of("create", "setpool", "setjump", "open", "participe", "quitter", "start", "test", "stop", "cancel");

            } else if (type.contains("Nexus")) {
                suggestions = List.of("create", "setnexus", "open", "participe", "quitter", "start", "test", "stop", "cancel");

            } else {
                suggestions = List.of("create", "start", "stop", "cancel");
            }

            return filter(suggestions, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("setnexus")) {
            return filter(List.of("rouge", "bleu"), args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("setpool")) {
            return filter(List.of("1", "2"), args[1]);
        }

        return List.of();
    }

    private List<String> filter(List<String> suggestions, String input) {
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}