package com.emipokemon.client.rewards;

import com.emipokemon.Emipokemon;
import com.emipokemon.rewards.DailyRewardNetworking.DailyRewardActionPayload;
import com.emipokemon.rewards.DailyRewardNetworking.OpenDailyRewardPayload;
import com.emipokemon.rewards.DailyRewardSnapshot;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public final class DailyRewardClient {
    private static final Gson GSON = new Gson();

    private DailyRewardClient() { }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(OpenDailyRewardPayload.ID, (payload, context) ->
                context.client().execute(() -> open(payload.json())));
    }

    static void send(String action) {
        if (ClientPlayNetworking.canSend(DailyRewardActionPayload.ID))
            ClientPlayNetworking.send(new DailyRewardActionPayload(action));
    }

    private static void open(String json) {
        try {
            DailyRewardSnapshot snapshot = GSON.fromJson(json, DailyRewardSnapshot.class);
            if (snapshot == null) return;
            MinecraftClient client = MinecraftClient.getInstance();
            Screen parent = client.currentScreen instanceof DailyRewardScreen daily ? daily.parent() : client.currentScreen;
            client.setScreen(new DailyRewardScreen(parent, snapshot));
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not open daily reward", exception);
        }
    }
}
