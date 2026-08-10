package com.emipokemon.client;

import com.emipokemon.Emipokemon;
import net.fabricmc.api.ClientModInitializer;

public final class EmipokemonClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Emipokemon.LOGGER.info("Emipokemon client layer initialized");
    }
}
