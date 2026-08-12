package com.emipokemon.client.render;

import com.emipokemon.client.model.GachaMachineModel;
import com.emipokemon.gacha.machine.GachaMachineBlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class GachaMachineRenderer extends GeoBlockRenderer<GachaMachineBlockEntity> {
    public GachaMachineRenderer(BlockEntityRendererFactory.Context context) {
        super(new GachaMachineModel());
    }

    @Override
    public @Nullable RenderLayer getRenderType(
            GachaMachineBlockEntity animatable,
            Identifier texture,
            @Nullable VertexConsumerProvider bufferSource,
            float partialTick
    ) {
        return RenderLayer.getEntityTranslucentCull(texture);
    }
}
