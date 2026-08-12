package com.emipokemon.client.shop;

import com.emipokemon.Emipokemon;
import com.emipokemon.shop.ShopSnapshot;
import com.emipokemon.shop.network.ShopNetworking.OpenShopPayload;
import com.emipokemon.shop.network.ShopNetworking.ShopActionPayload;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public final class ShopClient {
    private static final Gson GSON = new GsonBuilder().create();

    private ShopClient() {
    }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(OpenShopPayload.ID, (payload, context) ->
                context.client().execute(() -> open(payload)));
        Emipokemon.LOGGER.info("Visual Poke Mart client initialized");
    }

    public static void requestOpen(String category) {
        if (ClientPlayNetworking.canSend(ShopActionPayload.ID)) {
            ClientPlayNetworking.send(new ShopActionPayload("open", safe(category), "", 0));
        }
    }

    static void buy(String category, String productId, int quantity) {
        if (ClientPlayNetworking.canSend(ShopActionPayload.ID)) {
            ClientPlayNetworking.send(new ShopActionPayload("buy", safe(category), safe(productId), quantity));
        }
    }

    private static void open(OpenShopPayload payload) {
        try {
            ShopSnapshot snapshot = GSON.fromJson(payload.json(), ShopSnapshot.class);
            if (snapshot == null) return;
            MinecraftClient client = MinecraftClient.getInstance();
            Screen parent = client.currentScreen instanceof ShopScreen shop ? shop.parent() : client.currentScreen;
            client.setScreen(new ShopScreen(parent, snapshot, payload.category(), payload.productId(),
                    payload.message(), payload.success()));
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not open the Poke Mart", exception);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
