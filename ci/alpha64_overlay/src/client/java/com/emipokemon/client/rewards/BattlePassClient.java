package com.emipokemon.client.rewards;

import com.emipokemon.Emipokemon;
import com.emipokemon.rewards.BattlePassNetworking.BattlePassActionPayload;
import com.emipokemon.rewards.BattlePassNetworking.OpenBattlePassPayload;
import com.emipokemon.rewards.BattlePassSnapshot;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class BattlePassClient {
    private static final Gson GSON = new Gson();
    private static KeyBinding key;

    private BattlePassClient() { }

    public static void initialize() {
        key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.emipokemon.battle_pass", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, "category.emipokemon"));
        ClientPlayNetworking.registerGlobalReceiver(OpenBattlePassPayload.ID, (payload, context) ->
                context.client().execute(() -> open(payload.json())));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (key.wasPressed()) request("open", -1);
        });
    }

    public static void request(String action, int value) {
        if (ClientPlayNetworking.canSend(BattlePassActionPayload.ID))
            ClientPlayNetworking.send(new BattlePassActionPayload(action, value));
    }

    private static void open(String json) {
        try {
            BattlePassSnapshot snapshot = GSON.fromJson(json, BattlePassSnapshot.class);
            if (snapshot == null) return;
            MinecraftClient client = MinecraftClient.getInstance();
            Screen parent = client.currentScreen instanceof BattlePassScreen pass ? pass.parent() : client.currentScreen;
            client.setScreen(new BattlePassScreen(parent, snapshot));
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not open battle pass", exception);
        }
    }
}
