package com.emipokemon.client.admin;

import com.emipokemon.admin.AdminNetworking.AdminActionPayload;
import com.emipokemon.admin.AdminNetworking.OpenAdminPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class AdminClient {
    private AdminClient() {
    }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(OpenAdminPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    int tab = context.client().currentScreen instanceof AdminScreen current ? current.tab() : 0;
                    context.client().setScreen(new AdminScreen(context.client().currentScreen, payload.json(), payload.message(), tab));
                }));
    }

    static void send(String action, String json) {
        if (ClientPlayNetworking.canSend(AdminActionPayload.ID)) {
            ClientPlayNetworking.send(new AdminActionPayload(action, json == null ? "" : json));
        }
    }
}
