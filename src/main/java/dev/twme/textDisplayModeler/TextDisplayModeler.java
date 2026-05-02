package dev.twme.textdisplaymodeler;

import com.github.retrooper.packetevents.PacketEvents;
import dev.twme.textdisplaymodeler.command.ModelCommandRegistrar;
import dev.twme.textdisplaymodeler.model.ModelManager;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.tofaa.entitylib.APIConfig;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class TextDisplayModeler extends JavaPlugin {

    private static TextDisplayModeler instance;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings().checkForUpdates(false).bStats(true);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        PacketEvents.getAPI().init();
        EntityLib.init(new SpigotEntityLibPlatform(this), new APIConfig(PacketEvents.getAPI()));

        ModelManager.init();
        LanguageManager.init();

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            new ModelCommandRegistrar().register(event.registrar());
        });

        File modelFolder = new File(getDataFolder(), "models");
        if (!modelFolder.exists()) {
            modelFolder.mkdirs();
        }
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
    }

    public static TextDisplayModeler getInstance() {
        return instance;
    }
}
