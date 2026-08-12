package com.emipokemon.progress.network;

import com.emipokemon.Emipokemon;
import com.emipokemon.progress.ProgressionService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class ProgressionNetworking {
    private static boolean initialized;
    private static ProgressionService service;

    private ProgressionNetworking() {
    }

    public static synchronized void initializeServer(ProgressionService progressionService) {
        service = progressionService;
        if (initialized) return;
        initialized = true;
        PayloadTypeRegistry.playS2C().register(OpenJournalPayload.ID, OpenJournalPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BalanceSyncPayload.ID, BalanceSyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(JournalActionPayload.ID, JournalActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(JournalActionPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            switch (payload.action()) {
                case "open" -> open(player, payload.value());
                case "balance" -> syncBalance(player);
                case "claim" -> {
                    String track = normalizedTrack(payload.value());
                    progressionService.claimCurrentQuest(player, track);
                    open(player, "missions:" + track);
                }
                case "job_join" -> {
                    progressionService.joinJob(player, payload.value());
                    open(player, "jobs");
                }
                case "job_leave" -> {
                    progressionService.leaveJob(player, payload.value());
                    open(player, "jobs");
                }
                default -> Emipokemon.LOGGER.warn("Ignored unknown journal action {}", payload.action());
            }
        });
    }

    public static void open(ServerPlayerEntity player, String tab) {
        if (service == null || !ServerPlayNetworking.canSend(player, OpenJournalPayload.ID)) {
            player.sendMessage(net.minecraft.text.Text.literal("§cEl cliente necesita la misma versión de Emipokemon que el servidor para abrir el diario."), false);
            return;
        }
        String normalized = normalizedTab(tab);
        ServerPlayNetworking.send(player, new OpenJournalPayload(
                service.snapshotJson(player, trackFromTab(normalized)), normalized));
    }

    private static void syncBalance(ServerPlayerEntity player) {
        if (service != null && ServerPlayNetworking.canSend(player, BalanceSyncPayload.ID)) {
            ServerPlayNetworking.send(player, new BalanceSyncPayload(service.balance(player.getUuid())));
        }
    }

    private static String normalizedTab(String tab) {
        if ("jobs".equalsIgnoreCase(tab)) return "jobs";
        return "missions:" + trackFromTab(tab);
    }

    private static String trackFromTab(String tab) {
        return tab != null && tab.toLowerCase(java.util.Locale.ROOT).endsWith(":adventure")
                ? "adventure" : "progression";
    }

    private static String normalizedTrack(String track) {
        return "adventure".equalsIgnoreCase(track) ? "adventure" : "progression";
    }

    public record OpenJournalPayload(String json, String tab) implements CustomPayload {
        public static final Id<OpenJournalPayload> ID = new Id<>(Identifier.of(Emipokemon.MOD_ID, "open_journal"));
        public static final PacketCodec<RegistryByteBuf, OpenJournalPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, OpenJournalPayload::json,
                PacketCodecs.STRING, OpenJournalPayload::tab,
                OpenJournalPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record JournalActionPayload(String action, String value) implements CustomPayload {
        public static final Id<JournalActionPayload> ID = new Id<>(Identifier.of(Emipokemon.MOD_ID, "journal_action"));
        public static final PacketCodec<RegistryByteBuf, JournalActionPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, JournalActionPayload::action,
                PacketCodecs.STRING, JournalActionPayload::value,
                JournalActionPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record BalanceSyncPayload(long balance) implements CustomPayload {
        public static final Id<BalanceSyncPayload> ID = new Id<>(Identifier.of(Emipokemon.MOD_ID, "balance_sync"));
        public static final PacketCodec<RegistryByteBuf, BalanceSyncPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_LONG, BalanceSyncPayload::balance,
                BalanceSyncPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
