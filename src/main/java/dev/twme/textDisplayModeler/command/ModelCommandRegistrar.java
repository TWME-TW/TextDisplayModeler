package dev.twme.textdisplaymodeler.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
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

public class ModelCommandRegistrar {

    public void register(Commands commands) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("textdisplaymodeler")
                .requires(source -> source.getSender().hasPermission("textdisplaymodeler.use"));

        // Subcommand: load <filename>
        builder.then(Commands.literal("load")
                .then(Commands.argument("filename", StringArgumentType.string())
                        .suggests((context, suggestionsBuilder) -> {
                            File modelFolder = new File(dev.twme.textdisplaymodeler.TextDisplayModeler.getInstance().getDataFolder(), "models");
                            File[] files = modelFolder.listFiles((dir, name) -> 
                                    name.toLowerCase().endsWith(".stl") || name.toLowerCase().endsWith(".obj"));
                            if (files != null) {
                                for (File file : files) {
                                    suggestionsBuilder.suggest(file.getName());
                                }
                            }
                            return suggestionsBuilder.buildFuture();
                        })
                        .executes(this::executeLoad)))
                .then(Commands.literal("unload")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests((ctx, sb) -> {
                                    ModelManager.getLoadedFacets().keySet().forEach(sb::suggest);
                                    return sb.buildFuture();
                                })
                                .executes(this::executeUnload)));

        // Subcommand: spawn <name> <mode> [scale] [viewDistance] [color]
        var nameArg = Commands.argument("name", StringArgumentType.string())
                .suggests((ctx, sb) -> {
                    ModelManager.getLoadedFacets().keySet().forEach(sb::suggest);
                    return sb.buildFuture();
                });

        var modeArg = Commands.argument("mode", StringArgumentType.string())
                .suggests((ctx, sb) -> {
                    Arrays.stream(RenderMode.values()).forEach(mode -> sb.suggest(mode.name().toLowerCase()));
                    return sb.buildFuture();
                })
                .executes(ctx -> executeSpawn(ctx, 1.0f, 64.0, 0xFFFFFFFF));

        var scaleArg = Commands.argument("scale", FloatArgumentType.floatArg(0.01f))
                .executes(ctx -> executeSpawn(ctx, FloatArgumentType.getFloat(ctx, "scale"), 64.0, 0xFFFFFFFF));

        var viewDistArg = Commands.argument("viewDistance", DoubleArgumentType.doubleArg(1.0, 512.0))
                .executes(ctx -> executeSpawn(ctx, 
                        FloatArgumentType.getFloat(ctx, "scale"), 
                        DoubleArgumentType.getDouble(ctx, "viewDistance"),
                        0xFFFFFFFF));

        var colorArg = Commands.argument("color", StringArgumentType.string())
                .suggests((ctx, sb) -> {
                    sb.suggest("FFFFFF");
                    sb.suggest("FF0000");
                    sb.suggest("00FF00");
                    sb.suggest("0000FF");
                    return sb.buildFuture();
                })
                .executes(ctx -> executeSpawn(ctx,
                        FloatArgumentType.getFloat(ctx, "scale"),
                        DoubleArgumentType.getDouble(ctx, "viewDistance"),
                        parseHexColor(StringArgumentType.getString(ctx, "color"))));

        builder.then(Commands.literal("spawn")
                .then(nameArg.then(modeArg.then(scaleArg.then(viewDistArg.then(colorArg))))));

        // Subcommand: list [page]
        builder.then(Commands.literal("list")
                .executes(ctx -> executeList(ctx, 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> executeList(ctx, IntegerArgumentType.getInteger(ctx, "page")))));

        // Subcommand: removeall
        builder.then(Commands.literal("removeall")
                .executes(this::executeRemoveAll));

        // Subcommand: debug
        builder.then(Commands.literal("debug")
                .executes(this::executeDebug));

        commands.register(builder.build(), "Main STL Rendering Command", List.of("tsmodel"));
    }

    private int executeDebug(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getSender() instanceof Player player)) return 0;
        Location loc = player.getLocation();
        loc.setYaw(0);
        loc.setPitch(0);
        
        dev.twme.textdisplayshape.shape.Shape shape = new dev.twme.textdisplayshape.packet.PacketShapeFactory()
            .triangle(loc, 
                new org.joml.Vector3f((float)loc.getX(), (float)loc.getY() + 1, (float)loc.getZ()), 
                new org.joml.Vector3f((float)loc.getX() - 1, (float)loc.getY(), (float)loc.getZ()), 
                new org.joml.Vector3f((float)loc.getX() + 1, (float)loc.getY(), (float)loc.getZ()))
            .rootAnchor(true)
            .color(0xFFFF0000) // Red
            .build();
            
        shape.spawn();
        shape.addViewer(player.getUniqueId());
        player.sendMessage("§aSpawned debug red triangle! If this is broken, TextDisplayShapes is broken.");
        return Command.SINGLE_SUCCESS;
    }

    private int executeLoad(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getSender() instanceof Player player)) return 0;
        String fileName = StringArgumentType.getString(context, "filename");
        String modelName = fileName.replaceAll("(?i)\\.(stl|obj)$", "");
        int facetCount = ModelManager.loadModel(modelName, fileName);
        if (facetCount != -1) {
            player.sendMessage(LanguageManager.getMessage("load.success", 
                    Placeholder.parsed("name", modelName),
                    Placeholder.parsed("facets", String.valueOf(facetCount))
            ));
        } else {
            player.sendMessage(LanguageManager.getMessage("load.fail"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int executeSpawn(CommandContext<CommandSourceStack> context, float scale, double viewDistance, int argbColor) {
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

        Location loc = player.getLocation();
        loc.setYaw(0);
        loc.setPitch(0);
        ModelInstance instance = ModelManager.spawnModel(modelName, loc, scale, new Vector3f(0, 0, 0), mode, viewDistance, argbColor);
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

    private int parseHexColor(String hex) {
        try {
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }
            if (hex.length() == 6) {
                return 0xFF000000 | Integer.parseInt(hex, 16);
            } else if (hex.length() == 8) {
                return (int) Long.parseLong(hex, 16);
            }
        } catch (NumberFormatException ignored) {}
        return 0xFFFFFFFF; // Default to white if invalid
    }

    private int executeList(CommandContext<CommandSourceStack> context, int page) {
        if (!(context.getSource().getSender() instanceof Player player)) return 0;
        File modelFolder = new File(dev.twme.textdisplaymodeler.TextDisplayModeler.getInstance().getDataFolder(), "models");
        File[] files = modelFolder.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".stl") || name.toLowerCase().endsWith(".obj"));

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
            String modelName = fileName.replaceAll("(?i)\\.(stl|obj)$", "");
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

    private int executeUnload(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getSender() instanceof Player player)) return 0;
        String name = StringArgumentType.getString(context, "name");
        if (ModelManager.unloadModel(name)) {
            player.sendMessage(LanguageManager.getMessage("unload.success", Placeholder.parsed("name", name)));
        } else {
            player.sendMessage(LanguageManager.getMessage("unload.fail", Placeholder.parsed("name", name)));
        }
        return Command.SINGLE_SUCCESS;
    }
}
