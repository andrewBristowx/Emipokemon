package com.emipokemon.client;

import com.emipokemon.Emipokemon;
import com.emipokemon.client.render.GachaMachineRenderer;
import com.emipokemon.registry.ModRegistries;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public final class EmipokemonClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRendererFactories.register(ModRegistries.GACHA_MACHINE_BLOCK_ENTITY, GachaMachineRenderer::new);
        Emipokemon.LOGGER.info("Emipokemon client layer initialized with GachaMachineRenderer");
    }
}
