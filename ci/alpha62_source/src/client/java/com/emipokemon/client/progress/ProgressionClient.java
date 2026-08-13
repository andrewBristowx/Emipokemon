package com.emipokemon.client.progress;

import com.emipokemon.Emipokemon;
import com.emipokemon.progress.JournalSnapshot;
import com.emipokemon.progress.network.ProgressionNetworking.JournalActionPayload;
import com.emipokemon.progress.network.ProgressionNetworking.BalanceSyncPayload;
import com.emipokemon.progress.network.ProgressionNetworking.OpenJournalPayload;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class ProgressionClient {
    private static final Gson GSON = new GsonBuilder().create();
    private static KeyBinding journalKey;
    private static JournalSnapshot lastSnapshot;
    private static long cachedBalance = -1L;
    private static int balanceRefreshTicks;

    private ProgressionClient() {
    }

    public static void initialize() {
        journalKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.emipokemon.quests", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "category.emipokemon"));
        ClientPlayNetworking.registerGlobalReceiver(OpenJournalPayload.ID, (payload, context) ->
                context.client().execute(() -> open(payload.json(), payload.tab())));
        ClientPlayNetworking.registerGlobalReceiver(BalanceSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> cachedBalance = payload.balance()));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (journalKey.wasPressed()) requestOpen("missions:progression");
            if (client.player != null && client.getNetworkHandler() != null && ++balanceRefreshTicks >= 200) {
                balanceRefreshTicks = 0;
                requestBalance();
            }
        });
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof InventoryScreen)) return;
            int inventoryLeft = (scaledWidth - 176) / 2;
            int inventoryTop = (scaledHeight - 166) / 2;
            int shortcutX = Math.max(3, inventoryLeft - 25);
            Screens.getButtons(screen).add(new QuestBookButton(
                    shortcutX, Math.max(3, inventoryTop + 4),
                    () -> requestOpen("missions:progression"), ProgressionClient::hasClaimableReward));
            Screens.getButtons(screen).add(new MichicoinsButton(
                    shortcutX, Math.max(30, inventoryTop + 31),
                    () -> requestOpen("jobs"), ProgressionClient::cachedBalance));
            requestBalance();
        });
        Emipokemon.LOGGER.info("Visual quest journal initialized (J)");
    }

    public static void requestOpen(String tab) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null || !ClientPlayNetworking.canSend(JournalActionPayload.ID)) return;
        ClientPlayNetworking.send(new JournalActionPayload("open", tab));
    }

    public static void sendAction(String action, String value) {
        if (ClientPlayNetworking.canSend(JournalActionPayload.ID)) {
            ClientPlayNetworking.send(new JournalActionPayload(action, value == null ? "" : value));
        }
    }

    private static void requestBalance() {
        if (ClientPlayNetworking.canSend(JournalActionPayload.ID)) {
            ClientPlayNetworking.send(new JournalActionPayload("balance", ""));
        }
    }

    private static long cachedBalance() {
        return cachedBalance;
    }

    public static long cachedBalanceValue() {
        return cachedBalance;
    }

    public static boolean hasClaimableReward() {
        return lastSnapshot != null && lastSnapshot.claimableQuests > 0;
    }

    private static void open(String json, String tab) {
        try {
            JournalSnapshot snapshot = GSON.fromJson(json, JournalSnapshot.class);
            if (snapshot == null) return;
            lastSnapshot = snapshot;
            cachedBalance = snapshot.balance;
            MinecraftClient client = MinecraftClient.getInstance();
            Screen parent = client.currentScreen instanceof QuestJournalScreen journal ? journal.parent() : client.currentScreen;
            client.setScreen(new QuestJournalScreen(parent, snapshot, tab));
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not open progression journal", exception);
        }
    }
}
