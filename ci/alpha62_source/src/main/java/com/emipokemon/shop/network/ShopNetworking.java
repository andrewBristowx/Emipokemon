package com.emipokemon.shop.network;

import com.emipokemon.Emipokemon;
import com.emipokemon.shop.ShopService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ShopNetworking {
    private static boolean initialized;
    private static ShopService service;

    private ShopNetworking() {
    }

    public static synchronized void initializeServer(ShopService shopService) {
        service = shopService;
        if (initialized) return;
        initialized = true;
        PayloadTypeRegistry.playS2C().register(OpenShopPayload.ID, OpenShopPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ShopActionPayload.ID, ShopActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ShopActionPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            switch (payload.action()) {
                case "open" -> open(player, payload.category(), payload.productId(), "", true);
                case "buy" -> {
                    ShopService.PurchaseResult result = shopService.purchase(player, payload.productId(), payload.quantity());
                    open(player, payload.category(), payload.productId(), result.message(), result.success());
                }
                default -> Emipokemon.LOGGER.warn("Ignored unknown shop action {}", payload.action());
            }
        });
    }

    public static void open(ServerPlayerEntity player, String category) {
        open(player, category, "", "", true);
    }

    private static void open(ServerPlayerEntity player, String category, String productId, String message, boolean success) {
        if (service == null || !ServerPlayNetworking.canSend(player, OpenShopPayload.ID)) {
            player.sendMessage(Text.literal("§cEl cliente necesita la misma versión de Emipokemon para abrir la Poké Mart."), false);
            return;
        }
        ServerPlayNetworking.send(player, new OpenShopPayload(
                service.snapshotJson(player), safe(category), safe(productId), safe(message), success));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public record OpenShopPayload(String json, String category, String productId, String message, boolean success)
            implements CustomPayload {
        public static final Id<OpenShopPayload> ID = new Id<>(Identifier.of(Emipokemon.MOD_ID, "open_shop"));
        public static final PacketCodec<RegistryByteBuf, OpenShopPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, OpenShopPayload::json,
                PacketCodecs.STRING, OpenShopPayload::category,
                PacketCodecs.STRING, OpenShopPayload::productId,
                PacketCodecs.STRING, OpenShopPayload::message,
                PacketCodecs.BOOL, OpenShopPayload::success,
                OpenShopPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ShopActionPayload(String action, String category, String productId, int quantity)
            implements CustomPayload {
        public static final Id<ShopActionPayload> ID = new Id<>(Identifier.of(Emipokemon.MOD_ID, "shop_action"));
        public static final PacketCodec<RegistryByteBuf, ShopActionPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, ShopActionPayload::action,
                PacketCodecs.STRING, ShopActionPayload::category,
                PacketCodecs.STRING, ShopActionPayload::productId,
                PacketCodecs.VAR_INT, ShopActionPayload::quantity,
                ShopActionPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
