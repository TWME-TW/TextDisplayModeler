package dev.twme.textdisplaymodeler.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.twme.textdisplaymodeler.LanguageManager;
import dev.twme.textdisplaymodeler.model.ModelInstance;
import dev.twme.textdisplaymodeler.model.ModelManager;
import dev.twme.textdisplaymodeler.model.RenderMode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ModelCommandRegistrar {

    public void register(Commands commands) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("textdisplaymodeler")
                .requires(source -> source.getSender().hasPermission("textdisplaymodeler.use"));

        // Subcommand: load <filename>
        builder.then(Commands.literal("load")
                .then(Commands.argument("filename", StringArgumentType.string())
                        .suggests((context, suggestionsBuilder) -> {
                            File modelFolder = new File(dev.twme.textdisplaymodeler.TextDisplayModeler.getInstance().getDataFolder(), "models");
                            File[] files = modelFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".stl"));
                            if (files != null) {
                                for (File file : files) {
                                    suggestionsBuilder.suggest(file.getName());
                                }
                            }
                            return suggestionsBuilder.buildFuture();
                        })
                        .executes(this::executeLoad)));

        // Subcommand: spawn <name> <mode> [scale] [viewDistance]
        builder.then(Commands.literal("spawn")
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests((context, suggestionsBuilder) -> {
                            ModelManager.getLoadedFacets().keySet().forEach(suggestionsBuilder::suggest);
                            return suggestionsBuilder.buildFuture();
                        })
                        .then(Commands.argument("mode", StringArgumentType.string())
                                .suggests((context, suggestionsBuilder) -> {
                                    Arrays.stream(RenderMode.values()).forEach(mode -> suggestionsBuilder.suggest(mode.name().toLowerCase()));
                                    return suggestionsBuilder.buildFuture();
                                })
                                .executes(ctx -> executeSpawn(ctx, 1.0f, 64.0))
                                .then(Commands.argument("scale", FloatArgumentType.floatArg(0.01f))
                                        .executes(ctx -> executeSpawn(ctx, FloatArgumentType.getFloat(ctx, "scale"), 64.0))
                                        .then(Commands.argument("viewDistance", DoubleArgumentType.doubleArg(1.0))
                                                .executes(ctx -> executeSpawn(ctx, FloatArgumentType.getFloat(ctx, "scale"), DoubleArgumentType.getDouble(ctx, "viewDistance"))))))));

        // Subcommand: list [page]
        builder.then(Commands.literal("list")
                .executes(ctx -> executeList(ctx, 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> executeList(ctx, IntegerArgumentType.getInteger(ctx, "page")))));

        // Subcommand: removeall
        builder.then(Commands.literal("removeall")
                .executes(this::executeRemoveAll));

        commands.register(builder.build(), "Main STL Rendering Command", List.of("tsmodel"));
    }

    private int executeLoad(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getSender() instanceof Player player)) return 0;
        String fileName = StringArgumentType.getString(context, "filename");
        String modelName = fileName.replace(".stl", "");
        if (ModelManager.loadModel(modelName, fileName)) {
            player.sendMessage(LanguageManager.getMessage("load.success", Placeholder.parsed("name", modelName)));
        } else {
            player.sendMessage(LanguageManager.getMessage("load.fail"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int executeSpawn(CommandContext<CommandSourceStack> context, float scale, double viewDistance) {
        if (!(context.getSource().getSender() instanceof Player player)) return 0;
        String modelName = StringArgumentType.getString(context, "name");
        String modeStr = StringArgumentType.getString(context, "mode");
        RenderMode mode;
        try {
            mode = RenderMode.valueOf(modeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(LanguageManager.getMessage("spawn.invalid_mode"));
            return 0;
        }

        ModelInstance instance = ModelManager.spawnModel(modelName, player.getLocation(), scale, new Vector3f(0, 0, 0), mode, viewDistance);
        if (instance != null) {
            if (mode == RenderMode.PACKET) {
                instance.addViewer(player);
            }
            player.sendMessage(LanguageManager.getMessage("spawn.success",
                    Placeholder.parsed("name", modelName),
                    Placeholder.parsed("mode", mode.name()),
                    Placeholder.parsed("scale", String.valueOf(scale)),
                    Placeholder.parsed("viewdist", String.valueOf(viewDistance))
            ));
        } else {
            player.sendMessage(LanguageManager.getMessage("spawn.fail"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int executeList(CommandContext<CommandSourceStack> context, int page) {
        if (!(context.getSource().getSender() instanceof Player player)) return 0;
        File modelFolder = new File(dev.twme.textdisplaymodeler.TextDisplayModeler.getInstance().getDataFolder(), "models");
        File[] files = modelFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".stl"));

        if (files == null || files.length == 0) {
            player.sendMessage(LanguageManager.getMessage("list.no_files"));
            return Command.SINGLE_SUCCESS;
        }

        int itemsPerPage = 10;
        int totalPages = (int) Math.ceil((double) files.length / itemsPerPage);

        if (page < 1 || page > totalPages) {
            player.sendMessage(LanguageManager.getMessage("list.invalid_page", Placeholder.parsed("total", String.valueOf(totalPages))));
            return Command.SINGLE_SUCCESS;
        }

        player.sendMessage(LanguageManager.getMessage("list.header",
                Placeholder.parsed("page", String.valueOf(page)),
                Placeholder.parsed("total", String.valueOf(totalPages))
        ));

        int start = (page - 1) * itemsPerPage;
        int end = Math.min(start + itemsPerPage, files.length);

        for (int i = start; i < end; i++) {
            String fileName = files[i].getName();
            String modelName = fileName.replace(".stl", "");
            player.sendMessage(LanguageManager.getMessage("list.item",
                    Placeholder.parsed("file", fileName),
                    Placeholder.parsed("name", modelName)
            ));
        }
        player.sendMessage(LanguageManager.getMessage("list.footer"));
        return Command.SINGLE_SUCCESS;
    }

    private int executeRemoveAll(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getSender() instanceof Player player)) return 0;
        ModelManager.removeAll();
        player.sendMessage(LanguageManager.getMessage("system.remove_all"));
        return Command.SINGLE_SUCCESS;
    }
}
