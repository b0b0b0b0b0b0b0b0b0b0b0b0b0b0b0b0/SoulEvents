package bm.b0b0b0.soulevents.api.message;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

import java.util.Map;

public interface MessageService {

    Component resolve(String key, Map<String, String> placeholders);

    void send(CommandSender sender, String key, Map<String, String> placeholders);

    void broadcast(String key, Map<String, String> placeholders);

    void reload();
}
