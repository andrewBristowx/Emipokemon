package com.emipokemon.integration;

import com.emipokemon.Emipokemon;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class CobbleverseMissionHooks {
    private CobbleverseMissionHooks() {
    }

    public static void altarInvocationAccepted(ServerPlayerEntity player, Identifier altarId) {
        if (altarId == null || !"lumymon".equals(altarId.getNamespace())) return;
        switch (altarId.getPath()) {
            case "articuno_altar", "zapdos_altar", "moltres_altar" ->
                    Emipokemon.progressionService().recordExactAltarInvocation(player, altarId);
            default -> {
                // This release intentionally implements only audited quest altar IDs.
            }
        }
    }
}
