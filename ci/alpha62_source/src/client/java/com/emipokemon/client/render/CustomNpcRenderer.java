package com.emipokemon.client.render;

import com.emipokemon.Emipokemon;
import com.emipokemon.client.visual.ClientVisualAssetCache;
import com.emipokemon.npc.ServiceNpcEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

public final class CustomNpcRenderer extends MobEntityRenderer<ServiceNpcEntity, PlayerEntityModel<ServiceNpcEntity>> {
    private final Identifier fallback;

    public CustomNpcRenderer(EntityRendererFactory.Context context, boolean slim) {
        super(context, new PlayerEntityModel<>(
                context.getPart(slim ? EntityModelLayers.PLAYER_SLIM : EntityModelLayers.PLAYER), slim), 0.5f);
        this.fallback = Identifier.of(Emipokemon.MOD_ID,
                slim ? "textures/entity/nurse_npc.png" : "textures/entity/shop_npc.png");
    }

    @Override
    public Identifier getTexture(ServiceNpcEntity entity) {
        return ClientVisualAssetCache.texture("npc:" + entity.npcId(), fallback);
    }
}
