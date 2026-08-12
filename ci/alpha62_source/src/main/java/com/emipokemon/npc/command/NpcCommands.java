package com.emipokemon.npc.command;

import com.emipokemon.npc.ServiceNpcEntity;
import com.emipokemon.npc.NpcNetworking;
import com.emipokemon.npc.ServiceNpcEntity.NpcKind;
import com.emipokemon.registry.ModRegistries;
import com.emipokemon.visual.VisualAssetService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.EntityType;
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

public final class NpcCommands {
    private static VisualAssetService assets;

    private NpcCommands() {
    }

    public static void register(VisualAssetService visualAssets) {
        assets = visualAssets;
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerAll(dispatcher));
    }

    private static void registerAll(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("emipokemon").requires(source -> source.hasPermissionLevel(4))
                .then(literal("npc")
                        .then(literal("crear")
                                .then(literal("enfermera")
                                        .then(argument("id", StringArgumentType.word())
                                                .executes(context -> create(context.getSource(), NpcKind.NURSE,
                                                        StringArgumentType.getString(context, "id"), "balls"))))
                                .then(literal("tienda")
                                        .then(argument("id", StringArgumentType.word())
                                                .executes(context -> create(context.getSource(), NpcKind.SHOP,
                                                        StringArgumentType.getString(context, "id"), "balls"))
                                                .then(argument("categoria", StringArgumentType.word())
                                                        .suggests((context, builder) -> net.minecraft.command.CommandSource.suggestMatching(categories(), builder))
                                                        .executes(context -> create(context.getSource(), NpcKind.SHOP,
                                                                StringArgumentType.getString(context, "id"),
                                                                StringArgumentType.getString(context, "categoria"))))))
                                .then(literal("custom")
                                        .then(argument("id", StringArgumentType.word())
                                                .executes(context -> createCustom(context.getSource(),
                                                        StringArgumentType.getString(context, "id"), false))
                                                .then(argument("modelo", StringArgumentType.word())
                                                        .suggests((context, builder) -> net.minecraft.command.CommandSource.suggestMatching(
                                                                new String[]{"clasico", "slim"}, builder))
                                                        .executes(context -> createCustom(context.getSource(),
                                                                StringArgumentType.getString(context, "id"),
                                                                "slim".equalsIgnoreCase(StringArgumentType.getString(context, "modelo"))))))))
                        .then(literal("skin")
                                .then(argument("id", StringArgumentType.word())
                                        .then(literal("archivo")
                                                .executes(context -> loadSkinFile(context.getSource(),
                                                        StringArgumentType.getString(context, "id"))))
                                        .then(literal("url")
                                                .then(argument("url", StringArgumentType.greedyString())
                                                        .executes(context -> loadSkinUrl(context.getSource(),
                                                                StringArgumentType.getString(context, "id"),
                                                                StringArgumentType.getString(context, "url")))))))
                        .then(literal("editar")
                                .then(argument("id", StringArgumentType.word())
                                        .executes(context -> openEditor(context.getSource(),
                                                StringArgumentType.getString(context, "id")))))
                        .then(literal("mover")
                                .then(argument("id", StringArgumentType.word())
                                        .executes(context -> move(context.getSource(), StringArgumentType.getString(context, "id")))))
                        .then(literal("eliminar")
                                .then(argument("id", StringArgumentType.word())
                                        .executes(context -> remove(context.getSource(), StringArgumentType.getString(context, "id")))))
                        .then(literal("nombre")
                                .then(argument("id", StringArgumentType.word())
                                        .then(argument("nombre", StringArgumentType.greedyString())
                                                .executes(context -> rename(context.getSource(),
                                                        StringArgumentType.getString(context, "id"),
                                                        StringArgumentType.getString(context, "nombre"))))))
                        .then(literal("categoria")
                                .then(argument("id", StringArgumentType.word())
                                        .then(argument("categoria", StringArgumentType.word())
                                                .suggests((context, builder) -> net.minecraft.command.CommandSource.suggestMatching(categories(), builder))
                                                .executes(context -> category(context.getSource(),
                                                        StringArgumentType.getString(context, "id"),
                                                        StringArgumentType.getString(context, "categoria"))))))
                        .then(literal("listar").executes(context -> list(context.getSource())))));
    }

    private static int create(ServerCommandSource source, NpcKind kind, String rawId, String category) {
        String id = normalizeId(rawId);
        if (id.isBlank()) {
            source.sendError(Text.literal("El ID debe contener letras, números, guion o guion bajo."));
            return 0;
        }
        if (find(source.getServer(), id) != null) {
            source.sendError(Text.literal("Ya existe un NPC cargado con el ID '" + id + "'."));
            return 0;
        }

        EntityType<ServiceNpcEntity> type = kind == NpcKind.NURSE ? ModRegistries.NURSE_NPC : ModRegistries.SHOP_NPC;
        ServiceNpcEntity npc = type.create(source.getWorld());
        if (npc == null) {
            source.sendError(Text.literal("No se pudo crear el NPC."));
            return 0;
        }

        Vec3d pos = source.getPosition();
        float yaw = source.getEntity() == null ? 0.0f : source.getEntity().getYaw();
        npc.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0.0f);
        npc.setNpcId(id);
        npc.setShopCategory(category);
        npc.setCustomName(kind.defaultDisplayName());
        npc.setCustomNameVisible(true);
        npc.setAiDisabled(true);
        npc.setInvulnerable(true);
        npc.setPersistent();
        if (!source.getWorld().spawnEntity(npc)) {
            source.sendError(Text.literal("El mundo rechazó la creación del NPC."));
            return 0;
        }

        source.sendFeedback(() -> Text.literal("§aNPC " + kind.commandName() + " creado con ID §f" + id
                + "§a. Mira en la misma dirección en la que debe quedar."), true);
        return 1;
    }

    private static int createCustom(ServerCommandSource source, String rawId, boolean slim) {
        String id = normalizeId(rawId);
        if (id.isBlank()) {
            source.sendError(Text.literal("El ID debe contener letras, números, guion o guion bajo."));
            return 0;
        }
        if (find(source.getServer(), id) != null) {
            source.sendError(Text.literal("Ya existe un NPC cargado con el ID '" + id + "'."));
            return 0;
        }
        try {
            assets.ensureNpcFolder(id);
        } catch (Exception exception) {
            source.sendError(Text.literal("No se pudo crear la carpeta de la skin: " + exception.getMessage()));
            return 0;
        }
        EntityType<ServiceNpcEntity> type = slim ? ModRegistries.CUSTOM_SLIM_NPC : ModRegistries.CUSTOM_NPC;
        ServiceNpcEntity npc = type.create(source.getWorld());
        if (npc == null) return 0;
        Vec3d pos = source.getPosition();
        float yaw = source.getEntity() == null ? 0.0f : source.getEntity().getYaw();
        npc.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0.0f);
        npc.setNpcId(id);
        npc.setCustomName(NpcKind.CUSTOM.defaultDisplayName());
        npc.setCustomNameVisible(true);
        npc.setAiDisabled(true);
        npc.setInvulnerable(true);
        npc.setPersistent();
        if (!source.getWorld().spawnEntity(npc)) return 0;
        source.sendFeedback(() -> Text.literal("§aNPC custom §f" + id + " §acreado. Sube skin.png a §f"
                + assets.npcFolder(id) + " §ay ejecuta §f/emipokemon npc skin " + id + " archivo"), true);
        return 1;
    }

    private static int loadSkinFile(ServerCommandSource source, String rawId) {
        String id = normalizeId(rawId);
        ServiceNpcEntity npc = requireCustom(source, id);
        if (npc == null) return 0;
        try {
            VisualAssetService.Asset asset = assets.loadNpc(id);
            assets.broadcast(source.getServer(), asset);
            source.sendFeedback(() -> Text.literal("§aSkin validada y aplicada al NPC §f" + id), true);
            return 1;
        } catch (Exception exception) {
            source.sendError(Text.literal("Skin rechazada: " + exception.getMessage()));
            return 0;
        }
    }

    private static int loadSkinUrl(ServerCommandSource source, String rawId, String url) {
        String id = normalizeId(rawId);
        if (requireCustom(source, id) == null) return 0;
        source.sendFeedback(() -> Text.literal("§7Descargando y validando la skin HTTPS..."), false);
        CompletableFuture.runAsync(() -> {
            try {
                VisualAssetService.Asset asset = assets.downloadNpc(id, url);
                source.getServer().execute(() -> {
                    assets.broadcast(source.getServer(), asset);
                    source.sendFeedback(() -> Text.literal("§aSkin descargada y aplicada al NPC §f" + id), true);
                });
            } catch (Exception exception) {
                source.getServer().execute(() -> source.sendError(Text.literal("Skin rechazada: " + exception.getMessage())));
            }
        });
        return 1;
    }

    private static int move(ServerCommandSource source, String rawId) {
        ServiceNpcEntity npc = require(source, rawId);
        if (npc == null) return 0;
        if (npc.getWorld() != source.getWorld()) {
            NpcKind kind = npc.kind();
            boolean slim = npc.slimModel();
            String id = npc.npcId();
            String category = npc.shopCategory();
            Text name = npc.getCustomName();
            int result = recreateInWorld(source, kind, slim, id, category, name,
                    npc.dialogue(), npc.pokemonTeam());
            if (result > 0) npc.discard();
            return result;
        }
        Vec3d pos = source.getPosition();
        float yaw = source.getEntity() == null ? npc.getYaw() : source.getEntity().getYaw();
        npc.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0.0f);
        source.sendFeedback(() -> Text.literal("§aNPC §f" + npc.npcId() + " §amovido a tu posición."), true);
        return 1;
    }

    private static int recreateInWorld(ServerCommandSource source, NpcKind kind, boolean slim,
                                       String id, String category, Text name, String dialogue, List<String> team) {
        EntityType<ServiceNpcEntity> type = switch (kind) {
            case NURSE -> ModRegistries.NURSE_NPC;
            case SHOP -> ModRegistries.SHOP_NPC;
            case CUSTOM -> slim ? ModRegistries.CUSTOM_SLIM_NPC : ModRegistries.CUSTOM_NPC;
        };
        ServiceNpcEntity npc = type.create(source.getWorld());
        if (npc == null) return 0;
        Vec3d pos = source.getPosition();
        float yaw = source.getEntity() == null ? 0.0f : source.getEntity().getYaw();
        npc.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0.0f);
        npc.setNpcId(id);
        npc.setShopCategory(category);
        npc.setDialogue(dialogue);
        npc.setPokemonTeam(team);
        npc.setCustomName(name == null ? kind.defaultDisplayName() : name);
        npc.setCustomNameVisible(true);
        npc.setAiDisabled(true);
        npc.setInvulnerable(true);
        npc.setPersistent();
        if (!source.getWorld().spawnEntity(npc)) return 0;
        source.sendFeedback(() -> Text.literal("§aNPC §f" + npc.npcId() + " §amovido a esta dimensión."), true);
        return 1;
    }

    private static int openEditor(ServerCommandSource source, String rawId) {
        ServiceNpcEntity npc = requireCustom(source, normalizeId(rawId));
        if (npc == null) return 0;
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("Este menú solo puede abrirlo un jugador."));
            return 0;
        }
        NpcNetworking.openEditor(player, npc);
        return 1;
    }

    private static int remove(ServerCommandSource source, String rawId) {
        ServiceNpcEntity npc = require(source, rawId);
        if (npc == null) return 0;
        String id = npc.npcId();
        npc.discard();
        source.sendFeedback(() -> Text.literal("§eNPC §f" + id + " §eeliminado."), true);
        return 1;
    }

    private static int rename(ServerCommandSource source, String rawId, String name) {
        ServiceNpcEntity npc = require(source, rawId);
        if (npc == null) return 0;
        String clean = name == null ? "" : name.trim();
        if (clean.isBlank() || clean.length() > 48) {
            source.sendError(Text.literal("El nombre debe tener entre 1 y 48 caracteres."));
            return 0;
        }
        npc.setCustomName(Text.literal(clean));
        npc.setCustomNameVisible(true);
        source.sendFeedback(() -> Text.literal("§aNombre del NPC §f" + npc.npcId() + " §acambiado a §f" + clean), true);
        return 1;
    }

    private static int category(ServerCommandSource source, String rawId, String category) {
        ServiceNpcEntity npc = require(source, rawId);
        if (npc == null) return 0;
        if (npc.kind() != NpcKind.SHOP) {
            source.sendError(Text.literal("Solo los vendedores pueden tener una categoría inicial."));
            return 0;
        }
        String safe = NpcKind.safeCategory(category);
        npc.setShopCategory(safe);
        source.sendFeedback(() -> Text.literal("§aEl vendedor §f" + npc.npcId() + " §aabrirá en la categoría §f" + safe), true);
        return 1;
    }

    private static int list(ServerCommandSource source) {
        List<ServiceNpcEntity> npcs = allLoaded(source.getServer());
        if (npcs.isEmpty()) {
            source.sendFeedback(() -> Text.literal("§7No hay NPC de Emipokemon cargados."), false);
            return 0;
        }
        source.sendFeedback(() -> Text.literal("§dNPC de Emipokemon cargados: §f" + npcs.size()), false);
        for (ServiceNpcEntity npc : npcs) {
            source.sendFeedback(() -> Text.literal("§7- §f" + npc.npcId() + " §8[" + npc.kind().commandName() + "] §7"
                    + npc.getWorld().getRegistryKey().getValue() + " " + npc.getBlockPos().toShortString()), false);
        }
        return npcs.size();
    }

    private static ServiceNpcEntity require(ServerCommandSource source, String rawId) {
        String id = normalizeId(rawId);
        ServiceNpcEntity npc = find(source.getServer(), id);
        if (npc == null) {
            source.sendError(Text.literal("No se encontró el NPC cargado con ID '" + id + "'. Acércate a su zona para cargarla."));
        }
        return npc;
    }

    private static ServiceNpcEntity requireCustom(ServerCommandSource source, String rawId) {
        ServiceNpcEntity npc = require(source, rawId);
        if (npc != null && npc.kind() != NpcKind.CUSTOM) {
            source.sendError(Text.literal("Ese ID pertenece a un NPC de servicio, no a uno custom."));
            return null;
        }
        return npc;
    }

    private static ServiceNpcEntity find(MinecraftServer server, String id) {
        for (ServiceNpcEntity npc : allLoaded(server)) {
            if (npc.npcId().equalsIgnoreCase(id)) return npc;
        }
        return null;
    }

    private static List<ServiceNpcEntity> allLoaded(MinecraftServer server) {
        List<ServiceNpcEntity> result = new ArrayList<>();
        for (ServerWorld world : server.getWorlds()) {
            result.addAll(world.getEntitiesByType(ModRegistries.NURSE_NPC, entity -> true));
            result.addAll(world.getEntitiesByType(ModRegistries.SHOP_NPC, entity -> true));
            result.addAll(world.getEntitiesByType(ModRegistries.CUSTOM_NPC, entity -> true));
            result.addAll(world.getEntitiesByType(ModRegistries.CUSTOM_SLIM_NPC, entity -> true));
        }
        return result;
    }

    private static String normalizeId(String value) {
        if (value == null) return "";
        String normalized = value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        return normalized.length() > 32 ? normalized.substring(0, 32) : normalized;
    }

    private static String[] categories() {
        return new String[]{"balls", "medicine", "battle", "evolution", "supplies", "special_balls",
                "special_evolution", "protections", "gacha"};
    }
}
