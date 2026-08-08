package com.emipokemon.client.render;

import com.emipokemon.client.model.GachaMachineModel;
import com.emipokemon.gacha.machine.GachaMachineBlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class GachaMachineRenderer extends GeoBlockRenderer<GachaMachineBlockEntity> {
    public GachaMachineRenderer(BlockEntityRendererFactory.Context context) {
        super(new GachaMachineModel());
    }
}
