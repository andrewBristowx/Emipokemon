package com.emipokemon.client.npc;

import com.emipokemon.npc.NpcNetworking.OpenDialoguePayload;
import com.emipokemon.npc.NpcNetworking.OpenEditorPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class NpcAdminClient {
    private NpcAdminClient() {
    }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(OpenEditorPayload.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(
                        new NpcEditorScreen(context.client().currentScreen, payload.json()))));
        ClientPlayNetworking.registerGlobalReceiver(OpenDialoguePayload.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(
                        new NpcDialogueScreen(context.client().currentScreen, payload.json()))));
    }
}
