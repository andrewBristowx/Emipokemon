package com.emipokemon.client.render;

import com.emipokemon.client.model.GachaMachineModel;
import com.emipokemon.gacha.machine.GachaMachineBlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class GachaMachineRenderer extends GeoBlockRenderer<GachaMachineBlockEntity> {
    private final TextRenderer textRenderer;

    public GachaMachineRenderer(BlockEntityRendererFactory.Context context) {
        super(new GachaMachineModel());
        this.textRenderer = context.getTextRenderer();
    }

    @Override
    public void render(GachaMachineBlockEntity animatable, float partialTick, MatrixStack matrices,
                       VertexConsumerProvider buffers, int packedLight, int packedOverlay) {
        // GeckoLib may leave model-space transforms active while rendering complex generated geometry.
        // Keep the seasonal billboard isolated from those transforms.
        matrices.push();
        super.render(animatable, partialTick, matrices, buffers, packedLight, packedOverlay);
        matrices.pop();
        matrices.push();
        SeasonalPokemonWorldRenderer.draw(animatable, partialTick, matrices, buffers, textRenderer);
        matrices.pop();
    }

    @Override
    public @Nullable RenderLayer getRenderType(
            GachaMachineBlockEntity animatable,
            Identifier texture,
            @Nullable VertexConsumerProvider bufferSource,
            float partialTick
    ) {
        return RenderLayer.getEntityTranslucent(texture);
    }

    @Override public boolean rendersOutsideBoundingBox(GachaMachineBlockEntity blockEntity) { return true; }
    @Override public int getRenderDistance() { return 72; }
}
