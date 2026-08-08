package com.emipokemon.gacha.reward;

import com.emipokemon.Emipokemon;
import com.emipokemon.gacha.GachaRollResult;
import com.mojang.brigadier.ParseResults;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.concurrent.atomic.AtomicBoolean;

public final class CobblemonRewardService {
    public boolean deliver(ServerPlayerEntity player, GachaRollResult result) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        String species = result.pokemon().speciesId();
        StringBuilder properties = new StringBuilder(species)
                .append(" level=").append(result.level());
        if (result.shiny()) properties.append(" shiny=true");

        String command = "givepokemonother " + player.getGameProfile().getName() + " " + properties;
        try {
            ServerCommandSource baseSource = server.getCommandSource().withSilent();
            ParseResults<ServerCommandSource> parsed = server.getCommandManager().getDispatcher().parse(command, baseSource);
            if (parsed.getReader().canRead() || CommandManager.getException(parsed) != null) {
                Emipokemon.LOGGER.error("Cobblemon reward command could not be parsed: {}", command);
                return false;
            }

            AtomicBoolean completed = new AtomicBoolean(false);
            AtomicBoolean successful = new AtomicBoolean(false);
            ServerCommandSource source = baseSource.withReturnValueConsumer((success, value) -> {
                completed.set(true);
                successful.set(success && value > 0);
            });

            server.getCommandManager().executeWithPrefix(source, command);
            if (!completed.get() || !successful.get()) {
                Emipokemon.LOGGER.error("Cobblemon reward command did not complete successfully: {}", command);
                return false;
            }
            return true;
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not deliver gacha Pokemon with command {}", command, exception);
            return false;
        }
    }
}
