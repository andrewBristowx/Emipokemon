package com.emipokemon;

import com.emipokemon.command.EmipokemonCommands;
import com.emipokemon.config.ConfigManager;
import com.emipokemon.data.PlayerDataManager;
import com.emipokemon.registry.ModRegistries;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Emipokemon implements ModInitializer {
    public static final String MOD_ID = "emipokemon";
    public static final String VERSION = "0.1.0-alpha.1";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final ConfigManager CONFIG_MANAGER = new ConfigManager();
    private static final PlayerDataManager PLAYER_DATA_MANAGER = new PlayerDataManager();

    @Override
    public void onInitialize() {
        LOGGER.info("Starting Emipokemon {}", VERSION);

        CONFIG_MANAGER.initialize();
        ModRegistries.initialize();
        EmipokemonCommands.register(CONFIG_MANAGER, PLAYER_DATA_MANAGER);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                PLAYER_DATA_MANAGER.load(handler.player.getUuid()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                PLAYER_DATA_MANAGER.saveAndUnload(handler.player.getUuid()));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> PLAYER_DATA_MANAGER.saveAll());

        LOGGER.info("Emipokemon core initialized successfully");
    }

    public static ConfigManager configManager() {
        return CONFIG_MANAGER;
    }

    public static PlayerDataManager playerDataManager() {
        return PLAYER_DATA_MANAGER;
    }
}
