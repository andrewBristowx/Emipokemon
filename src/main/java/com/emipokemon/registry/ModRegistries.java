package com.emipokemon.registry;

import com.emipokemon.Emipokemon;

public final class ModRegistries {
    private ModRegistries() {
    }

    public static void initialize() {
        // Phase 1 keeps registries intentionally empty. Future items, blocks,
        // sounds and networking entry points will be registered from here.
        Emipokemon.LOGGER.debug("Emipokemon registries initialized");
    }
}
