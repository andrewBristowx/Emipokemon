package com.emipokemon.rewards;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.minecraft.server.command.CommandManager.literal;

public final class BattlePassCommands {
    private BattlePassCommands() { }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("pase").executes(context -> open(context.getSource().getPlayer())));
            dispatcher.register(literal("battlepass").executes(context -> open(context.getSource().getPlayer())));
        });
    }

    private static int open(ServerPlayerEntity player) {
        if (player == null) return 0;
        BattlePassNetworking.open(player, -1);
        return 1;
    }
}
