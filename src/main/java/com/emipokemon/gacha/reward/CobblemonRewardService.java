package com.emipokemon.gacha.reward;

import com.emipokemon.Emipokemon;
import com.emipokemon.gacha.GachaRollResult;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class CobblemonRewardService {
    public boolean deliver(ServerPlayerEntity player, GachaRollResult result) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        String species = result.pokemon().speciesId();
        if (species.startsWith("cobblemon:")) species = species.substring("cobblemon:".length());

        StringBuilder properties = new StringBuilder(species)
                .append(" level=").append(result.level());
        if (result.shiny()) properties.append(" shiny=true");

        String command = "givepokemonother " + player.getGameProfile().getName() + " " + properties;
        try {
            int resultCode = server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), command);
            if (resultCode <= 0) {
                Emipokemon.LOGGER.error("Cobblemon reward command returned {} for {}", resultCode, command);
                return false;
            }
            return true;
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not deliver gacha Pokemon with command {}", command, exception);
            return false;
        }
    }
}
