package com.emipokemon.client;

import com.emipokemon.client.emote.HologramStreamotesClientService;
import com.emipokemon.Emipokemon;
import com.emipokemon.client.render.GachaMachineRenderer;
import com.emipokemon.client.render.ServiceNpcRenderer;
import com.emipokemon.client.render.CustomNpcRenderer;
import com.emipokemon.client.render.MediaDisplayRenderer;
import com.emipokemon.client.render.HologramRenderer;
import com.emipokemon.client.visual.ClientVisualAssetCache;
import com.emipokemon.client.npc.NpcAdminClient;
import com.emipokemon.client.emote.ChatEmoteController;
import com.emipokemon.client.progress.ProgressionClient;
import com.emipokemon.client.shop.ShopClient;
import com.emipokemon.client.admin.AdminClient;
import com.emipokemon.client.casino.CasinoClient;
import com.emipokemon.client.render.CasinoMachineRenderer;
import com.emipokemon.client.rewards.BattlePassClient;
import com.emipokemon.client.rewards.DailyRewardClient;
import com.emipokemon.registry.ModRegistries;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.util.Identifier;

public final class EmipokemonClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HologramStreamotesClientService.initialize();
        BlockEntityRendererFactories.register(ModRegistries.GACHA_MACHINE_BLOCK_ENTITY, GachaMachineRenderer::new);
        BlockEntityRendererFactories.register(ModRegistries.CASINO_MACHINE_BLOCK_ENTITY, CasinoMachineRenderer::new);
        EntityRendererRegistry.register(ModRegistries.NURSE_NPC, context -> new ServiceNpcRenderer(
                context, true, Identifier.of(Emipokemon.MOD_ID, "textures/entity/nurse_npc.png")));
        EntityRendererRegistry.register(ModRegistries.SHOP_NPC, context -> new ServiceNpcRenderer(
                context, false, Identifier.of(Emipokemon.MOD_ID, "textures/entity/shop_npc.png")));
        EntityRendererRegistry.register(ModRegistries.CUSTOM_NPC, context -> new CustomNpcRenderer(context, false));
        EntityRendererRegistry.register(ModRegistries.CUSTOM_SLIM_NPC, context -> new CustomNpcRenderer(context, true));
        EntityRendererRegistry.register(ModRegistries.MEDIA_DISPLAY, MediaDisplayRenderer::new);
        EntityRendererRegistry.register(ModRegistries.HOLOGRAM, HologramRenderer::new);
        ClientVisualAssetCache.initialize();
        NpcAdminClient.initialize();
        ChatEmoteController.initialize();
        ProgressionClient.initialize();
        ShopClient.initialize();
        AdminClient.initialize();
        CasinoClient.initialize();
        BattlePassClient.initialize();
        DailyRewardClient.initialize();
        Emipokemon.LOGGER.info("Emipokemon client layer initialized with GachaMachineRenderer");
    }
}
