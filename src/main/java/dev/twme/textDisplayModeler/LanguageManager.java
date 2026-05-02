package dev.twme.textdisplaymodeler;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class LanguageManager {
    private static JavaPlugin plugin;
    private static File langFile;
    private static YamlConfiguration langConfig;
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    public static void init() {
        plugin = TextDisplayModeler.getInstance();
        langFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!langFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public static Component getMessage(String key, TagResolver... placeholders) {
        String message = langConfig.getString(key);
        if (message == null) {
            // Try with 'commands.' prefix as a fallback if not found
            message = langConfig.getString("commands." + key);
        }
        
        if (message == null) {
            return Component.text("Missing message: " + key);
        }

        Component prefix = miniMessage.deserialize(langConfig.getString("prefix", ""));
        return prefix.append(miniMessage.deserialize(message, placeholders));
    }
}
