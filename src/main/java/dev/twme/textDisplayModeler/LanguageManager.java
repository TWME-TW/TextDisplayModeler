package dev.twme.textdisplaymodeler;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class LanguageManager {
    private static YamlConfiguration langConfig;
    private static final MiniMessage mm = MiniMessage.miniMessage();

    public static void init() {
        TextDisplayModeler plugin = TextDisplayModeler.getInstance();
        File langFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!langFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);

        // Load defaults from jar
        InputStream defStream = plugin.getResource("messages.yml");
        if (defStream != null) {
            langConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8)));
        }
    }

    public static Component getMessage(String path, TagResolver... resolvers) {
        String message = langConfig.getString(path, "Missing message: " + path);
        return mm.deserialize(message, resolvers);
    }

    public static String getString(String path) {
        return langConfig.getString(path, "");
    }
}
