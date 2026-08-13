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

public final class DailyRewardNetworking {
    private static final Gson GSON = new Gson();
    private static DailyRewardService service;

    private DailyRewardNetworking() { }

    public static void initializeServer(DailyRewardService value) {
        service = value;
        PayloadTypeRegistry.playS2C().register(OpenDailyRewardPayload.ID, OpenDailyRewardPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DailyRewardActionPayload.ID, DailyRewardActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(DailyRewardActionPayload.ID,
                (payload, context) -> handle(context.player(), payload));
    }

    public static void open(ServerPlayerEntity player, String message) {
        send(player, service.snapshot(player, message));
    }

    private static void handle(ServerPlayerEntity player, DailyRewardActionPayload payload) {
        if ("claim".equals(payload.action())) {
            DailyRewardService.ClaimResult result = service.claim(player);
            send(player, service.snapshot(player, result.message()));
        } else if ("open".equals(payload.action())) open(player, "");
    }

    private static void send(ServerPlayerEntity player, DailyRewardSnapshot snapshot) {
        if (player != null && ServerPlayNetworking.canSend(player, OpenDailyRewardPayload.ID))
            ServerPlayNetworking.send(player, new OpenDailyRewardPayload(GSON.toJson(snapshot)));
    }

    public record OpenDailyRewardPayload(String json) implements CustomPayload {
        public static final Id<OpenDailyRewardPayload> ID = new Id<>(Identifier.of(Emipokemon.MOD_ID, "open_daily_reward"));
        public static final PacketCodec<RegistryByteBuf, OpenDailyRewardPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, OpenDailyRewardPayload::json, OpenDailyRewardPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record DailyRewardActionPayload(String action) implements CustomPayload {
        public static final Id<DailyRewardActionPayload> ID = new Id<>(Identifier.of(Emipokemon.MOD_ID, "daily_reward_action"));
        public static final PacketCodec<RegistryByteBuf, DailyRewardActionPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, DailyRewardActionPayload::action, DailyRewardActionPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
