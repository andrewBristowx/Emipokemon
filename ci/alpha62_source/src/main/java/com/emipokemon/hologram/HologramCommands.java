package com.emipokemon.hologram;

import com.emipokemon.Emipokemon;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class HologramCommands {
    private HologramCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("emipokemon").requires(source -> source.hasPermissionLevel(4))
                .then(literal("holograma")
                        .then(literal("crear").then(argument("id", StringArgumentType.word())
                                .then(argument("texto", StringArgumentType.greedyString())
                                        .executes(context -> create(context.getSource(),
                                                StringArgumentType.getString(context, "id"),
                                                StringArgumentType.getString(context, "texto"))))))
                        .then(literal("texto").then(argument("id", StringArgumentType.word())
                                .then(argument("texto", StringArgumentType.greedyString())
                                        .executes(context -> text(context.getSource(),
                                                StringArgumentType.getString(context, "id"),
                                                StringArgumentType.getString(context, "texto"))))))
                        .then(literal("escala").then(argument("id", StringArgumentType.word())
                                .then(argument("escala", FloatArgumentType.floatArg(0.25F, 8.0F))
                                        .executes(context -> scale(context.getSource(),
                                                StringArgumentType.getString(context, "id"),
                                                FloatArgumentType.getFloat(context, "escala"))))))
                        .then(literal("color").then(argument("id", StringArgumentType.word())
                                .then(argument("hex", StringArgumentType.word())
                                        .executes(context -> color(context.getSource(),
                                                StringArgumentType.getString(context, "id"),
                                                StringArgumentType.getString(context, "hex"))))))
                        .then(literal("mover").then(argument("id", StringArgumentType.word())
                                .executes(context -> move(context.getSource(), StringArgumentType.getString(context, "id")))))
                        .then(literal("eliminar").then(argument("id", StringArgumentType.word())
                                .executes(context -> remove(context.getSource(), StringArgumentType.getString(context, "id")))))
                        .then(literal("placeholders").executes(context -> placeholders(context.getSource())))
                        .then(literal("listar").executes(context -> list(context.getSource())))));
    }

    public static int create(ServerCommandSource source, String rawId, String value) {
        try {
            String id = normalize(rawId);
            if (id.isBlank() || HologramService.exists(source.getServer(), id)) {
                source.sendError(Text.literal(id.isBlank() ? "ID de holograma no válido." : "Ya existe ese holograma persistido."));
                return 0;
            }
            ServerWorld world = source.getWorld();
            if (world == null) world = source.getServer().getOverworld();
            if (world == null) throw new IllegalStateException("ningún mundo está disponible");
            Vec3d pos = source.getPosition();
            DisplayEntity.TextDisplayEntity entity = HologramService.create(world, id, value, pos);
            if (entity == null) throw new IllegalStateException("el mundo rechazó la entidad");
            source.sendFeedback(() -> Text.literal("§aHolograma §f" + id + " §acreado y persistido."), true);
            return 1;
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not create hologram {}", rawId, exception);
            source.sendError(Text.literal("No se pudo crear el holograma: " + exception.getClass().getSimpleName()));
            return 0;
        }
    }

    private static int text(ServerCommandSource source, String rawId, String value) {
        String id = normalize(rawId);
        if (HologramService.updateText(source.getServer(), id, value) == null) return missing(source, id);
        return success(source, id, "texto actualizado");
    }

    private static int scale(ServerCommandSource source, String rawId, float value) {
        String id = normalize(rawId);
        if (HologramService.updateScale(source.getServer(), id, value) == null) return missing(source, id);
        source.sendFeedback(() -> Text.literal("§eHolograma §f" + id + "§e: escala " + value + " guardada; alpha.26 prueba texto vanilla a escala 1.0."), true);
        return 1;
    }

    private static int color(ServerCommandSource source, String rawId, String rawHex) {
        String id = normalize(rawId);
        try {
            String hex = rawHex.startsWith("#") ? rawHex.substring(1) : rawHex;
            if (!hex.matches("[0-9a-fA-F]{6}")) throw new IllegalArgumentException();
            int color = Integer.parseUnsignedInt(hex, 16);
            if (HologramService.updateColor(source.getServer(), id, color) == null) return missing(source, id);
            return success(source, id, "color #" + hex.toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            source.sendError(Text.literal("Usa un color hexadecimal RRGGBB, por ejemplo FF55FF."));
            return 0;
        }
    }

    private static int move(ServerCommandSource source, String rawId) {
        String id = normalize(rawId);
        ServerWorld world = source.getWorld();
        if (world == null) return missing(source, id);
        DisplayEntity.TextDisplayEntity entity = HologramService.move(source.getServer(), world, id, source.getPosition());
        if (entity == null) return missing(source, id);
        return success(source, id, "movido y recreado en esta posición");
    }

    private static int remove(ServerCommandSource source, String rawId) {
        String id = normalize(rawId);
        if (!HologramService.remove(source.getServer(), id)) return missing(source, id);
        source.sendFeedback(() -> Text.literal("§eHolograma §f" + id + " §eeliminado del registro y del mundo cargado."), true);
        return 1;
    }

    private static int list(ServerCommandSource source) {
        var records = HologramService.records(source.getServer());
        source.sendFeedback(() -> Text.literal("§dHologramas persistidos: §f" + records.size()), false);
        for (HologramRegistryStore.Entry entry : records) {
            boolean loaded = HologramService.find(source.getServer(), entry.id()) != null;
            source.sendFeedback(() -> Text.literal("§7- §f" + entry.id() + " §7" + entry.world() + " "
                    + (int)Math.floor(entry.x()) + " " + (int)Math.floor(entry.y()) + " " + (int)Math.floor(entry.z())
                    + " §8[text_display=" + (loaded ? "sí" : "no") + "]"), false);
        }
        return records.size();
    }

    private static int placeholders(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("§aalpha.27: placeholders locales activos sobre minecraft:text_display."), false);
        source.sendFeedback(() -> Text.literal("§7Disponibles: {player} {displayname} {uuid} {michicoins} {ping} {fps} {online} {max_players} {server} {dimension} {biome} {x} {y} {z} {time} {date} {hologram_id} {emipokemon_version}"), false);
        source.sendFeedback(() -> Text.literal("§dEmotes opcionales: usa :NombreEmote:. Si Streamotes no está disponible, el token queda como texto normal."), false);
        return 2;
    }

    private static int missing(ServerCommandSource source, String id) {
        source.sendError(Text.literal("No se encontró el holograma persistido '" + id + "'. Usa /emipokemon holograma listar."));
        return 0;
    }

    private static int success(ServerCommandSource source, String id, String action) {
        source.sendFeedback(() -> Text.literal("§aHolograma §f" + id + "§a: " + action + "."), true);
        return 1;
    }

    private static String normalize(String value) {
        return HologramRegistryStore.normalize(value);
    }
}
