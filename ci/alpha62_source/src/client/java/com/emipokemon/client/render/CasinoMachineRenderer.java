package com.emipokemon.client.render;

import com.emipokemon.casino.CasinoMachineBlockEntity;
import com.emipokemon.client.model.CasinoMachineModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class CasinoMachineRenderer extends GeoBlockRenderer<CasinoMachineBlockEntity> {
    public CasinoMachineRenderer(BlockEntityRendererFactory.Context context) {
        super(new CasinoMachineModel());
    }

    @Override
    public @Nullable RenderLayer getRenderType(CasinoMachineBlockEntity animatable, Identifier texture,
                                               @Nullable VertexConsumerProvider buffers, float partialTick) {
        return RenderLayer.getEntityCutoutNoCull(texture, false);
    }

    @Override
    public boolean rendersOutsideBoundingBox(CasinoMachineBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getRenderDistance() {
        return 96;
    }

}
