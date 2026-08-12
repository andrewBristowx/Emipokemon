package com.emipokemon.visual.command;

import com.emipokemon.registry.ModRegistries;
import com.emipokemon.visual.MediaDisplayEntity;
import com.emipokemon.visual.VisualAssetService;
import com.emipokemon.npc.NpcNetworking;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class MediaCommands {
    private static VisualAssetService assets;

    private MediaCommands() {
    }

    public static void register(VisualAssetService visualAssets) {
        assets = visualAssets;
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerAll(dispatcher));
    }

    private static void registerAll(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("emipokemon").requires(source -> source.hasPermissionLevel(4))
                .then(literal("media")
                        .then(literal("crear")
                                .then(argument("id", StringArgumentType.word())
                                        .then(argument("ancho", FloatArgumentType.floatArg(0.25f, 16.0f))
                                                .then(argument("alto", FloatArgumentType.floatArg(0.25f, 16.0f))
                                                        .executes(context -> create(context.getSource(),
                                                                StringArgumentType.getString(context, "id"),
                                                                FloatArgumentType.getFloat(context, "ancho"),
                                                                FloatArgumentType.getFloat(context, "alto")))))))
                        .then(literal("cargar")
                                .then(argument("id", StringArgumentType.word())
                                        .then(literal("archivo")
                                                .executes(context -> loadFile(context.getSource(),
                                                        StringArgumentType.getString(context, "id"))))
                                        .then(literal("url")
                                                .then(argument("url", StringArgumentType.greedyString())
                                                        .executes(context -> loadUrl(context.getSource(),
                                                                StringArgumentType.getString(context, "id"),
                                                                StringArgumentType.getString(context, "url")))))))
                        .then(literal("editar")
                                .then(argument("id", StringArgumentType.word())
                                        .executes(context -> openEditor(context.getSource(),
                                                StringArgumentType.getString(context, "id")))))
                        .then(literal("mover")
                                .then(argument("id", StringArgumentType.word())
                                        .executes(context -> move(context.getSource(),
                                                StringArgumentType.getString(context, "id")))))
                        .then(literal("tamano")
                                .then(argument("id", StringArgumentType.word())
                                        .then(argument("ancho", FloatArgumentType.floatArg(0.25f, 16.0f))
                                                .then(argument("alto", FloatArgumentType.floatArg(0.25f, 16.0f))
                                                        .executes(context -> resize(context.getSource(),
                                                                StringArgumentType.getString(context, "id"),
                                                                FloatArgumentType.getFloat(context, "ancho"),
                                                                FloatArgumentType.getFloat(context, "alto")))))))
                        .then(literal("eliminar")
                                .then(argument("id", StringArgumentType.word())
                                        .executes(context -> remove(context.getSource(),
                                                StringArgumentType.getString(context, "id")))))
                        .then(literal("listar").executes(context -> list(context.getSource())))));
    }

    private static int create(ServerCommandSource source, String rawId, float width, float height) {
        String id = normalizeId(rawId);
        if (id.isBlank() || find(source.getServer(), id) != null) {
            source.sendError(Text.literal(id.isBlank() ? "ID multimedia no válido." : "Ya existe un panel con ese ID."));
            return 0;
        }
        try {
            assets.ensureMediaFolder(id);
        } catch (Exception exception) {
            source.sendError(Text.literal("No se pudo crear la carpeta multimedia: " + exception.getMessage()));
            return 0;
        }
        MediaDisplayEntity display = ModRegistries.MEDIA_DISPLAY.create(source.getWorld());
        if (display == null) return 0;
        Vec3d pos = source.getPosition();
        float yaw = source.getEntity() == null ? 0.0f : source.getEntity().getYaw();
        display.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0.0f);
        display.setDisplayId(id);
        display.setDisplaySize(width, height);
        display.setInvulnerable(true);
        display.setNoGravity(true);
        if (!source.getWorld().spawnEntity(display)) return 0;
        source.sendFeedback(() -> Text.literal("§aPanel multimedia §f" + id + " §acreado. Sube media.png o media.gif a §f"
                + assets.mediaFolder(id) + " §ay ejecuta §f/emipokemon media cargar " + id + " archivo"), true);
        return 1;
    }

    private static int loadFile(ServerCommandSource source, String rawId) {
        String id = normalizeId(rawId);
        if (require(source, id) == null) return 0;
        try {
            VisualAssetService.Asset asset = assets.loadMedia(id);
            assets.broadcast(source.getServer(), asset);
            source.sendFeedback(() -> Text.literal("§aImagen/GIF validado y aplicado al panel §f" + id), true);
            return 1;
        } catch (Exception exception) {
            source.sendError(Text.literal("Archivo rechazado: " + exception.getMessage()));
            return 0;
        }
    }

    private static int loadUrl(ServerCommandSource source, String rawId, String url) {
        String id = normalizeId(rawId);
        if (require(source, id) == null) return 0;
        source.sendFeedback(() -> Text.literal("§7Descargando y validando la imagen/GIF HTTPS..."), false);
        CompletableFuture.runAsync(() -> {
            try {
                VisualAssetService.Asset asset = assets.downloadMedia(id, url);
                source.getServer().execute(() -> {
                    assets.broadcast(source.getServer(), asset);
                    source.sendFeedback(() -> Text.literal("§aRecurso aplicado al panel §f" + id), true);
                });
            } catch (Exception exception) {
                source.getServer().execute(() -> source.sendError(Text.literal("Archivo rechazado: " + exception.getMessage())));
            }
        });
        return 1;
    }

    private static int move(ServerCommandSource source, String rawId) {
        MediaDisplayEntity display = require(source, rawId);
        if (display == null) return 0;
        if (display.getWorld() != source.getWorld()) {
            source.sendError(Text.literal("El panel debe estar cargado en esta dimensión para moverlo."));
            return 0;
        }
        Vec3d pos = source.getPosition();
        float yaw = source.getEntity() == null ? display.getYaw() : source.getEntity().getYaw();
        display.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0.0f);
        source.sendFeedback(() -> Text.literal("§aPanel §f" + display.displayId() + " §amovido."), true);
        return 1;
    }

    private static int openEditor(ServerCommandSource source, String rawId) {
        MediaDisplayEntity display = require(source, rawId);
        if (display == null) return 0;
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("Este menú solo puede abrirlo un jugador."));
            return 0;
        }
        NpcNetworking.openMediaEditor(player, display);
        return 1;
    }

    private static int resize(ServerCommandSource source, String rawId, float width, float height) {
        MediaDisplayEntity display = require(source, rawId);
        if (display == null) return 0;
        display.setDisplaySize(width, height);
        source.sendFeedback(() -> Text.literal("§aPanel §f" + display.displayId() + " §aajustado a "
                + width + "×" + height + " bloques."), true);
        return 1;
    }

    private static int remove(ServerCommandSource source, String rawId) {
        MediaDisplayEntity display = require(source, rawId);
        if (display == null) return 0;
        String id = display.displayId();
        display.discard();
        source.sendFeedback(() -> Text.literal("§ePanel §f" + id + " §eeliminado. Su carpeta se conserva."), true);
        return 1;
    }

    private static int list(ServerCommandSource source) {
        List<MediaDisplayEntity> displays = allLoaded(source.getServer());
        source.sendFeedback(() -> Text.literal("§dPaneles multimedia cargados: §f" + displays.size()), false);
        for (MediaDisplayEntity display : displays) {
            source.sendFeedback(() -> Text.literal("§7- §f" + display.displayId() + " §7"
                    + display.getWorld().getRegistryKey().getValue() + " " + display.getBlockPos().toShortString()
                    + " §8(" + display.displayWidth() + "×" + display.displayHeight() + ")"), false);
        }
        return displays.size();
    }

    private static MediaDisplayEntity require(ServerCommandSource source, String rawId) {
        String id = normalizeId(rawId);
        MediaDisplayEntity display = find(source.getServer(), id);
        if (display == null) source.sendError(Text.literal("No se encontró el panel cargado con ID '" + id + "'."));
        return display;
    }

    private static MediaDisplayEntity find(MinecraftServer server, String id) {
        for (MediaDisplayEntity display : allLoaded(server)) {
            if (display.displayId().equalsIgnoreCase(id)) return display;
        }
        return null;
    }

    private static List<MediaDisplayEntity> allLoaded(MinecraftServer server) {
        List<MediaDisplayEntity> result = new ArrayList<>();
        for (ServerWorld world : server.getWorlds()) {
            result.addAll(world.getEntitiesByType(ModRegistries.MEDIA_DISPLAY, entity -> true));
        }
        return result;
    }

    private static String normalizeId(String value) {
        if (value == null) return "";
        String normalized = value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        return normalized.length() > 32 ? normalized.substring(0, 32) : normalized;
    }
}
