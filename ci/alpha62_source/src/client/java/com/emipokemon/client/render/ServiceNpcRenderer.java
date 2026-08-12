package com.emipokemon.client.render;

import com.emipokemon.npc.ServiceNpcEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

public final class ServiceNpcRenderer extends MobEntityRenderer<ServiceNpcEntity, PlayerEntityModel<ServiceNpcEntity>> {
    private final Identifier texture;

    public ServiceNpcRenderer(EntityRendererFactory.Context context, boolean slim, Identifier texture) {
        super(context, new PlayerEntityModel<>(context.getPart(slim ? EntityModelLayers.PLAYER_SLIM : EntityModelLayers.PLAYER), slim), 0.5f);
        this.texture = texture;
    }

    @Override
    public Identifier getTexture(ServiceNpcEntity entity) {
        return texture;
    }
}
