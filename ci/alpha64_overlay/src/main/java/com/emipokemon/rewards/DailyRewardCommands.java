package com.emipokemon.rewards;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.minecraft.server.command.CommandManager.literal;

public final class DailyRewardCommands {
    private DailyRewardCommands() { }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(literal("diario").executes(context -> open(context.getSource().getPlayer()))
                        .then(literal("recompensa").executes(context -> open(context.getSource().getPlayer())))));
    }

    private static int open(ServerPlayerEntity player) {
        if (player == null) return 0;
        DailyRewardNetworking.open(player, "");
        return 1;
    }
}
