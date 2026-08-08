package com.emipokemon.gacha.reward;

import com.emipokemon.Emipokemon;
import com.emipokemon.gacha.GachaRollResult;
import com.mojang.brigadier.ParseResults;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
            AtomicInteger returnValue = new AtomicInteger(0);
            ServerCommandSource source = baseSource.withReturnValueConsumer((success, value) -> {
                completed.set(true);
                successful.set(success);
                returnValue.set(value);
            });

            server.getCommandManager().executeWithPrefix(source, command);

            // Cobblemon can legitimately return 0 after a successful give (notably when the
            // Pokemon is routed to storage). The boolean success flag is authoritative here;
            // treating returnValue > 0 as mandatory caused successful rewards to be refunded
            // and prevented pity from being committed.
            if (completed.get()) {
                if (!successful.get()) {
                    Emipokemon.LOGGER.error(
                            "Cobblemon reward command reported failure (returnValue={}): {}",
                            returnValue.get(), command
                    );
                    return false;
                }
                return true;
            }

            // Some command implementations do not publish a return-value callback. At this
            // point parsing succeeded and executeWithPrefix returned without throwing; the
            // command is built only from a catalog-validated species and the current player.
            Emipokemon.LOGGER.warn(
                    "Cobblemon reward command completed without a return callback; accepting delivery: {}",
                    command
            );
            return true;
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not deliver gacha Pokemon with command {}", command, exception);
            return false;
        }
    }
}
