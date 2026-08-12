package com.emipokemon;

import com.emipokemon.command.EmipokemonCommands;
import com.emipokemon.command.GachaCommands;
import com.emipokemon.command.HubCommands;
import com.emipokemon.command.Phase3Commands;
import com.emipokemon.config.ConfigManager;
import com.emipokemon.data.PlayerDataManager;
import com.emipokemon.gacha.GachaService;
import com.emipokemon.gacha.banner.BannerManager;
import com.emipokemon.gacha.catalog.PokemonCatalogService;
import com.emipokemon.integration.StreamotesServerIntegration;
import com.emipokemon.npc.command.NpcCommands;
import com.emipokemon.npc.NpcNetworking;
import com.emipokemon.npc.NpcBattleService;
import com.emipokemon.progress.ProgressionService;
import com.emipokemon.progress.command.ProgressionCommands;
import com.emipokemon.progress.network.ProgressionNetworking;
import com.emipokemon.registry.ModRegistries;
import com.emipokemon.shop.ShopCatalog;
import com.emipokemon.shop.ShopService;
import com.emipokemon.shop.command.ShopCommands;
import com.emipokemon.shop.network.ShopNetworking;
import com.emipokemon.visual.VisualAssetNetworking;
import com.emipokemon.visual.VisualAssetService;
import com.emipokemon.visual.command.MediaCommands;
import com.emipokemon.hologram.HologramCommands;
import com.emipokemon.hologram.HologramService;
import com.emipokemon.hologram.HologramViewerTextService;
import com.emipokemon.admin.AdminNetworking;
import com.emipokemon.casino.CasinoNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Emipokemon implements ModInitializer {
    public static final String MOD_ID = "emipokemon";
    public static final String VERSION = "0.4.0-alpha.62";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final ConfigManager CONFIG_MANAGER = new ConfigManager();
    private static final PlayerDataManager PLAYER_DATA_MANAGER = new PlayerDataManager();
    private static final PokemonCatalogService POKEMON_CATALOG = new PokemonCatalogService();
    private static final BannerManager BANNER_MANAGER = new BannerManager();
    private static final GachaService GACHA_SERVICE = new GachaService(POKEMON_CATALOG, BANNER_MANAGER, PLAYER_DATA_MANAGER);
    private static final ProgressionService PROGRESSION_SERVICE = new ProgressionService(
            PLAYER_DATA_MANAGER, POKEMON_CATALOG, CONFIG_MANAGER);
    private static final ShopCatalog SHOP_CATALOG = new ShopCatalog();
    private static final ShopService SHOP_SERVICE = new ShopService(SHOP_CATALOG, PROGRESSION_SERVICE, CONFIG_MANAGER);
    private static final VisualAssetService VISUAL_ASSETS = new VisualAssetService();

    @Override
    public void onInitialize() {
        LOGGER.info("Starting Emipokemon {}", VERSION);

        CONFIG_MANAGER.initialize();
        BANNER_MANAGER.initialize();
        SHOP_CATALOG.initialize();
        VISUAL_ASSETS.initialize();
        ModRegistries.initialize();
        EmipokemonCommands.register(CONFIG_MANAGER, PLAYER_DATA_MANAGER);
        HubCommands.register(CONFIG_MANAGER);
        GachaCommands.register(POKEMON_CATALOG, BANNER_MANAGER, GACHA_SERVICE, PLAYER_DATA_MANAGER);
        Phase3Commands.register();
        ProgressionNetworking.initializeServer(PROGRESSION_SERVICE);
        ProgressionCommands.register(PROGRESSION_SERVICE);
        PROGRESSION_SERVICE.initialize();
        ShopNetworking.initializeServer(SHOP_SERVICE);
        ShopCommands.register(SHOP_CATALOG);
        VisualAssetNetworking.initializeServer();
        NpcNetworking.initializeServer(VISUAL_ASSETS);
        NpcCommands.register(VISUAL_ASSETS);
        MediaCommands.register(VISUAL_ASSETS);
        HologramCommands.register();
        HologramViewerTextService.initialize();
        AdminNetworking.initializeServer();
        CasinoNetworking.initializeServer();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PROGRESSION_SERVICE.playerJoined(handler.player);
            VISUAL_ASSETS.syncAll(handler.player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            PROGRESSION_SERVICE.playerLeft(handler.player.getUuid());
            PLAYER_DATA_MANAGER.saveAndUnload(handler.player.getUuid());
        });
        ServerLifecycleEvents.SERVER_STARTING.register(server -> StreamotesServerIntegration.ensureOfficialChannel());
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            POKEMON_CATALOG.rebuild();
            NpcBattleService.auditCompatibility(server);
            HologramService.restoreAll(server);
            LOGGER.info("Emipokemon backends ready: {} Pokemon, {} banners, {} shop products",
                    POKEMON_CATALOG.size(), BANNER_MANAGER.size(), SHOP_CATALOG.availableProductCount());
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> PLAYER_DATA_MANAGER.saveAll());

        LOGGER.info("Emipokemon core initialized successfully");
    }

    public static ConfigManager configManager() {
        return CONFIG_MANAGER;
    }

    public static PlayerDataManager playerDataManager() {
        return PLAYER_DATA_MANAGER;
    }

    public static PokemonCatalogService pokemonCatalog() {
        return POKEMON_CATALOG;
    }

    public static BannerManager bannerManager() {
        return BANNER_MANAGER;
    }

    public static GachaService gachaService() {
        return GACHA_SERVICE;
    }

    public static ProgressionService progressionService() {
        return PROGRESSION_SERVICE;
    }

    public static ShopCatalog shopCatalog() {
        return SHOP_CATALOG;
    }

    public static ShopService shopService() {
        return SHOP_SERVICE;
    }
}
