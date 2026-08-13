package com.emipokemon.client.casino;

import com.emipokemon.casino.CasinoNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public final class CasinoClient {
    private static boolean initialized;

    private CasinoClient() { }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ClientPlayNetworking.registerGlobalReceiver(CasinoNetworking.OpenCasinoPayload.ID, (payload, context) ->
                context.client().execute(() -> open(payload.json())));
    }

    private static void open(String json) {
        MinecraftClient client = MinecraftClient.getInstance();
        Screen current = client.currentScreen;
        Screen parent = current;
        String previousAmount = null;
        CasinoScreen.PresentationState presentation = null;
        if (current instanceof CasinoScreen casino) {
            parent = casino.parentScreen();
            previousAmount = casino.amountText();
            presentation = casino.presentationState();
        }
        client.setScreen(new CasinoScreen(parent, json, previousAmount, presentation));
    }
}
