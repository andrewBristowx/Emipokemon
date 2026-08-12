package com.emipokemon.npc;

import com.emipokemon.Emipokemon;
import com.emipokemon.registry.ModRegistries;
import com.emipokemon.visual.MediaDisplayEntity;
import com.emipokemon.visual.VisualAssetService;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class NpcNetworking {
    private static final Gson GSON = new Gson();
    private static final int CHUNK_BYTES = 18_000;
    private static final int MAX_CHUNKS = (VisualAssetService.MAX_BYTES + CHUNK_BYTES - 1) / CHUNK_BYTES;
    private static final Map<UploadKey, UploadAssembly> UPLOADS = new HashMap<>();
    private static boolean initialized;
    private static VisualAssetService assets;

    private NpcNetworking() {
    }

    public static synchronized void initializeServer(VisualAssetService visualAssets) {
        assets = visualAssets;
        if (initialized) return;
        initialized = true;
        PayloadTypeRegistry.playS2C().register(OpenEditorPayload.ID, OpenEditorPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenDialoguePayload.ID, OpenDialoguePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SaveNpcPayload.ID, SaveNpcPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SaveMediaPayload.ID, SaveMediaPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StartBattlePayload.ID, StartBattlePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UrlAssetPayload.ID, UrlAssetPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UploadChunkPayload.ID, UploadChunkPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SaveNpcPayload.ID,
                (payload, context) -> saveNpc(context.player(), payload.json()));
        ServerPlayNetworking.registerGlobalReceiver(SaveMediaPayload.ID,
                (payload, context) -> saveMedia(context.player(), payload.json()));
        ServerPlayNetworking.registerGlobalReceiver(StartBattlePayload.ID,
                (payload, context) -> startBattle(context.player(), payload.npcId()));
        ServerPlayNetworking.registerGlobalReceiver(UrlAssetPayload.ID,
                (payload, context) -> loadUrl(context.player(), payload.kind(), payload.id(), payload.url()));
        ServerPlayNetworking.registerGlobalReceiver(UploadChunkPayload.ID,
                (payload, context) -> acceptUpload(context.player(), payload));
    }

    public static void openEditor(ServerPlayerEntity player, ServiceNpcEntity npc) {
        if (!player.hasPermissionLevel(4) || !ServerPlayNetworking.canSend(player, OpenEditorPayload.ID)) return;
        NpcEditorState state = new NpcEditorState("npc", npc.npcId(),
                npc.getCustomName() == null ? "NPC personalizado" : npc.getCustomName().getString(),
                npc.dialogue(), npc.pokemonTeam(), npc.battleRewards(), npc.battleRewardRepeatable(), 0.0f, 0.0f);
        ServerPlayNetworking.send(player, new OpenEditorPayload(GSON.toJson(state)));
    }

    public static void openMediaEditor(ServerPlayerEntity player, MediaDisplayEntity display) {
        if (!player.hasPermissionLevel(4) || !ServerPlayNetworking.canSend(player, OpenEditorPayload.ID)) return;
        NpcEditorState state = new NpcEditorState("media", display.displayId(), "", "", List.of(), List.of(),
                false, display.displayWidth(), display.displayHeight());
        ServerPlayNetworking.send(player, new OpenEditorPayload(GSON.toJson(state)));
    }

    public static void openDialogue(ServerPlayerEntity player, ServiceNpcEntity npc) {
        if (!ServerPlayNetworking.canSend(player, OpenDialoguePayload.ID)) {
            player.sendMessage(Text.literal(npc.dialogue().isBlank() ? "§7Este NPC todavía no tiene diálogo."
                    : npc.dialogue()), false);
            return;
        }
        DialogueState state = new DialogueState(npc.npcId(),
                npc.getCustomName() == null ? "NPC personalizado" : npc.getCustomName().getString(),
                npc.dialogue().isBlank() ? "Este NPC todavía no tiene diálogo configurado." : npc.dialogue(),
                !npc.pokemonTeam().isEmpty(), NpcRewardService.describe(npc.battleRewards()),
                npc.battleRewardRepeatable(), NpcRewardService.alreadyClaimed(player, npc));
        ServerPlayNetworking.send(player, new OpenDialoguePayload(GSON.toJson(state)));
    }

    private static void saveNpc(ServerPlayerEntity player, String json) {
        if (!player.hasPermissionLevel(4)) return;
        try {
            NpcEditorState state = GSON.fromJson(json, NpcEditorState.class);
            String id = normalizeId(state.id());
            ServiceNpcEntity npc = findNpc(player.getServer(), id);
            if (npc == null || npc.kind() != ServiceNpcEntity.NpcKind.CUSTOM) {
                throw new IllegalArgumentException("No se encontró el NPC custom cargado.");
            }
            String name = safe(state.name(), 48).strip();
            if (name.isBlank()) throw new IllegalArgumentException("El nombre no puede estar vacío.");
            String dialogue = safe(state.dialogue(), 2048);
            List<String> team = NpcBattleService.validateTeam(state.team());
            List<String> rewards = NpcRewardService.validate(state.rewards());
            npc.setCustomName(Text.literal(name));
            npc.setCustomNameVisible(true);
            npc.setDialogue(dialogue);
            npc.setPokemonTeam(team);
            npc.setBattleRewards(rewards);
            npc.setBattleRewardRepeatable(state.rewardRepeatable());
            player.sendMessage(Text.literal("§aNPC §f" + id + " §aguardado: diálogo y " + team.size()
                    + " Pokémon."), false);
        } catch (Exception exception) {
            player.sendMessage(Text.literal("§cNo se guardó el NPC: " + exception.getMessage()), false);
        }
    }

    private static void saveMedia(ServerPlayerEntity player, String json) {
        if (!player.hasPermissionLevel(4)) return;
        try {
            NpcEditorState state = GSON.fromJson(json, NpcEditorState.class);
            MediaDisplayEntity display = findMedia(player.getServer(), normalizeId(state.id()));
            if (display == null) throw new IllegalArgumentException("No se encontró el panel cargado.");
            if (state.width() < 0.25f || state.width() > 16.0f || state.height() < 0.25f || state.height() > 16.0f) {
                throw new IllegalArgumentException("El tamaño debe estar entre 0.25 y 16 bloques.");
            }
            display.setDisplaySize(state.width(), state.height());
            player.sendMessage(Text.literal("§aTamaño del panel §f" + display.displayId() + " §aguardado."), false);
        } catch (Exception exception) {
            player.sendMessage(Text.literal("§cNo se guardó el panel: " + exception.getMessage()), false);
        }
    }

    private static void startBattle(ServerPlayerEntity player, String rawId) {
        ServiceNpcEntity npc = findNpc(player.getServer(), normalizeId(rawId));
        if (npc == null) {
            player.sendMessage(Text.literal("§cEl NPC ya no está cargado."), false);
            return;
        }
        NpcBattleService.start(player, npc);
    }

    private static void loadUrl(ServerPlayerEntity player, String rawKind, String rawId, String rawUrl) {
        if (!player.hasPermissionLevel(4)) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;
        String kind = "media".equalsIgnoreCase(rawKind) ? "media" : "npc";
        String id = normalizeId(rawId);
        if (!validTarget(server, kind, id)) {
            player.sendMessage(Text.literal("§cNo se encontró el destino cargado."), false);
            return;
        }
        String url = safe(rawUrl, 2048).strip();
        player.sendMessage(Text.literal("§7Descargando y validando el archivo HTTPS..."), false);
        CompletableFuture.runAsync(() -> {
            try {
                VisualAssetService.Asset asset = "media".equals(kind)
                        ? assets.downloadMedia(id, url) : assets.downloadNpc(id, url);
                server.execute(() -> {
                    if (server.getPlayerManager().getPlayer(player.getUuid()) == null) return;
                    assets.broadcast(server, asset);
                    player.sendMessage(Text.literal("§aArchivo actualizado en el servidor y enviado a los clientes."), false);
                });
            } catch (Exception exception) {
                server.execute(() -> {
                    if (server.getPlayerManager().getPlayer(player.getUuid()) != null) {
                        player.sendMessage(Text.literal("§cArchivo rechazado: " + exception.getMessage()), false);
                    }
                });
            }
        });
    }

    private static void acceptUpload(ServerPlayerEntity player, UploadChunkPayload payload) {
        if (!player.hasPermissionLevel(4)) return;
        String kind = "media".equalsIgnoreCase(payload.kind()) ? "media" : "npc";
        String id = normalizeId(payload.id());
        if (!validTarget(player.getServer(), kind, id) || payload.total() < 1 || payload.total() > MAX_CHUNKS
                || payload.index() < 0 || payload.index() >= payload.total()) return;
        byte[] part;
        try {
            part = Base64.getDecoder().decode(payload.base64Data());
        } catch (IllegalArgumentException exception) {
            return;
        }
        if (part.length < 1 || part.length > CHUNK_BYTES) return;
        UploadKey key = new UploadKey(player.getUuid(), kind, id);
        long now = System.currentTimeMillis();
        UPLOADS.entrySet().removeIf(entry -> now - entry.getValue().createdAt > 60_000L);
        long activeForPlayer = UPLOADS.keySet().stream().filter(existing -> existing.player().equals(player.getUuid())).count();
        if (!UPLOADS.containsKey(key) && activeForPlayer >= 2) return;
        UploadAssembly assembly = UPLOADS.computeIfAbsent(key, ignored -> new UploadAssembly(payload.total()));
        if (assembly.total != payload.total()) {
            UPLOADS.remove(key);
            return;
        }
        assembly.parts[payload.index()] = part;
        if (!assembly.complete()) return;
        UPLOADS.remove(key);
        byte[] bytes = assembly.join();
        if (bytes.length < 1 || bytes.length > VisualAssetService.MAX_BYTES) return;
        try {
            VisualAssetService.Asset asset = "media".equals(kind)
                    ? assets.storeMedia(id, bytes) : assets.storeNpc(id, bytes);
            assets.broadcast(player.getServer(), asset);
            player.sendMessage(Text.literal("§aArchivo subido al servidor y actualizado para todos los clientes."), false);
        } catch (Exception exception) {
            player.sendMessage(Text.literal("§cArchivo rechazado: " + exception.getMessage()), false);
        }
    }

    private static boolean validTarget(MinecraftServer server, String kind, String id) {
        if ("media".equals(kind)) return findMedia(server, id) != null;
        ServiceNpcEntity npc = findNpc(server, id);
        return npc != null && npc.kind() == ServiceNpcEntity.NpcKind.CUSTOM;
    }

    public static ServiceNpcEntity findNpc(MinecraftServer server, String id) {
        if (server == null) return null;
        for (ServerWorld world : server.getWorlds()) {
            for (ServiceNpcEntity npc : world.getEntitiesByType(ModRegistries.CUSTOM_NPC, entity -> true)) {
                if (npc.npcId().equalsIgnoreCase(id)) return npc;
            }
            for (ServiceNpcEntity npc : world.getEntitiesByType(ModRegistries.CUSTOM_SLIM_NPC, entity -> true)) {
                if (npc.npcId().equalsIgnoreCase(id)) return npc;
            }
        }
        return null;
    }

    public static MediaDisplayEntity findMedia(MinecraftServer server, String id) {
        if (server == null) return null;
        for (ServerWorld world : server.getWorlds()) {
            for (MediaDisplayEntity display : world.getEntitiesByType(ModRegistries.MEDIA_DISPLAY, entity -> true)) {
                if (display.displayId().equalsIgnoreCase(id)) return display;
            }
        }
        return null;
    }

    private static String normalizeId(String value) {
        if (value == null) return "";
        String normalized = value.strip().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        return normalized.length() > 32 ? normalized.substring(0, 32) : normalized;
    }

    private static String safe(String value, int max) {
        if (value == null) return "";
        return value.length() > max ? value.substring(0, max) : value;
    }

    public record NpcEditorState(String kind, String id, String name, String dialogue, List<String> team,
                                 List<String> rewards, boolean rewardRepeatable, float width, float height) {
        public NpcEditorState {
            team = team == null ? List.of() : List.copyOf(team);
            rewards = rewards == null ? List.of() : List.copyOf(rewards);
        }
    }

    public record DialogueState(String id, String name, String dialogue, boolean hasTeam, String rewards,
                                boolean rewardRepeatable, boolean rewardClaimed) {
    }

    public record OpenEditorPayload(String json) implements CustomPayload {
        public static final Id<OpenEditorPayload> ID = new Id<>(Identifier.of(Emipokemon.MOD_ID, "open_npc_editor"));
        public static final PacketCodec<RegistryByteBuf, OpenEditorPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, OpenEditorPayload::json, OpenEditorPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record OpenDialoguePayload(String json) implements CustomPayload {
        public static final Id<OpenDialoguePayload> ID = new Id<>(Identifier.of(Emipokemon.MOD_ID, "open_npc_dialogue"));
        public static final PacketCodec<RegistryByteBuf, OpenDialoguePayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, OpenDialoguePayload::json, OpenDialoguePayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record SaveNpcPayload(String json) implements CustomPayload {
        public static final Id<SaveNpcPayload> ID = new Id<>(Identifier.of(Emipokemon.MOD_ID, "save_custom_npc"));
        public static final PacketCodec<RegistryByteBuf, SaveNpcPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, SaveNpcPayload::json, SaveNpcPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record SaveMediaPayload(String json) implements CustomPayload {
        public static final Id<SaveMediaPayload> ID = new Id<>(Identifier.of(Emipokemon.MOD_ID, "save_media_panel"));
        public static final PacketCodec<RegistryByteBuf, SaveMediaPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, SaveMediaPayload::json, SaveMediaPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record StartBattlePayload(String npcId) implements CustomPayload {
        public static final Id<StartBattlePayload> ID = new Id<>(Identifier.of(Emipokemon.MOD_ID, "start_npc_battle"));
        public static final PacketCodec<RegistryByteBuf, StartBattlePayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, StartBattlePayload::npcId, StartBattlePayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record UrlAssetPayload(String kind, String id, String url) implements CustomPayload {
        public static final Id<UrlAssetPayload> ID = new Id<>(Identifier.of(Emipokemon.MOD_ID, "npc_asset_url"));
        public static final PacketCodec<RegistryByteBuf, UrlAssetPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, UrlAssetPayload::kind,
                PacketCodecs.STRING, UrlAssetPayload::id,
                PacketCodecs.STRING, UrlAssetPayload::url,
                UrlAssetPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record UploadChunkPayload(String kind, String id, int index, int total, String base64Data)
            implements CustomPayload {
        public static final Id<UploadChunkPayload> ID = new Id<>(Identifier.of(Emipokemon.MOD_ID, "npc_asset_upload"));
        public static final PacketCodec<RegistryByteBuf, UploadChunkPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, UploadChunkPayload::kind,
                PacketCodecs.STRING, UploadChunkPayload::id,
                PacketCodecs.VAR_INT, UploadChunkPayload::index,
                PacketCodecs.VAR_INT, UploadChunkPayload::total,
                PacketCodecs.STRING, UploadChunkPayload::base64Data,
                UploadChunkPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    private record UploadKey(UUID player, String kind, String id) {
    }

    private static final class UploadAssembly {
        private final int total;
        private final byte[][] parts;
        private final long createdAt = System.currentTimeMillis();
        private UploadAssembly(int total) { this.total = total; this.parts = new byte[total][]; }
        private boolean complete() {
            for (byte[] part : parts) if (part == null) return false;
            return true;
        }
        private byte[] join() {
            try {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                for (byte[] part : parts) output.write(part);
                return output.toByteArray();
            } catch (Exception exception) {
                return new byte[0];
            }
        }
    }
}
