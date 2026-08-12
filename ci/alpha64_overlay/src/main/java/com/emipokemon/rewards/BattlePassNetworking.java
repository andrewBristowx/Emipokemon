package com.emipokemon.rewards;

import com.emipokemon.Emipokemon;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class BattlePassNetworking {
    private static final Gson GSON = new Gson();
    private static BattlePassService service;

    private BattlePassNetworking() { }

    public static void initializeServer(BattlePassService value) {
        service = value;
        PayloadTypeRegistry.playS2C().register(OpenBattlePassPayload.ID, OpenBattlePassPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BattlePassActionPayload.ID, BattlePassActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(BattlePassActionPayload.ID,
                (payload, context) -> handle(context.player(), payload));
    }

    public static void open(ServerPlayerEntity player, int page) {
        send(player, service.snapshot(player, page, ""));
    }

    private static void handle(ServerPlayerEntity player, BattlePassActionPayload payload) {
        String action = payload.action() == null ? "" : payload.action();
        int page = Math.max(0, payload.value());
        String message = "";
        if ("claim_free".equals(action) || "claim_premium".equals(action)) {
            int level = payload.value();
            boolean premium = "claim_premium".equals(action);
            boolean claimed = service.claim(player, premium, level);
            message = claimed ? "Recompensa reclamada correctamente."
                    : "No puedes reclamar esa recompensa todavía.";
            page = Math.max(0, (Math.max(1, level) - 1) / BattlePassService.PAGE_SIZE);
        } else if (!"open".equals(action) && !"page".equals(action)) {
            return;
        }
        send(player, service.snapshot(player, page, message));
    }

    private static void send(ServerPlayerEntity player, BattlePassSnapshot snapshot) {
        if (player != null && ServerPlayNetworking.canSend(player, OpenBattlePassPayload.ID))
            ServerPlayNetworking.send(player, new OpenBattlePassPayload(GSON.toJson(snapshot)));
    }

    public record OpenBattlePassPayload(String json) implements CustomPayload {
        public static final Id<OpenBattlePassPayload> ID = new Id<>(Identifier.of(Emipokemon.MOD_ID, "open_battle_pass"));
        public static final PacketCodec<RegistryByteBuf, OpenBattlePassPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, OpenBattlePassPayload::json, OpenBattlePassPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record BattlePassActionPayload(String action, int value) implements CustomPayload {
        public static final Id<BattlePassActionPayload> ID = new Id<>(Identifier.of(Emipokemon.MOD_ID, "battle_pass_action"));
        public static final PacketCodec<RegistryByteBuf, BattlePassActionPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, BattlePassActionPayload::action,
                PacketCodecs.VAR_INT, BattlePassActionPayload::value,
                BattlePassActionPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
